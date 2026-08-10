package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import io.littlehorse.sdk.common.proto.InlineArrayDef;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.junit.jupiter.api.Test;

import java.util.Map;

class JsonSchemaTransformerTest {

    @Test
    void transformsStructMetadataAndPrimitiveFields() {
        InlineStructDef inlineStructDef = InlineStructDef.newBuilder()
                .putFields("name", requiredField(primitive(VariableType.STR)))
                .putFields("age", requiredField(primitive(VariableType.INT)))
                .putFields("score", requiredField(primitive(VariableType.DOUBLE)))
                .putFields("active", optionalField(primitive(VariableType.BOOL)))
                .putFields("payload", requiredField(primitive(VariableType.BYTES)))
                .putFields("runId", requiredField(primitive(VariableType.WF_RUN_ID)))
                .putFields("createdAt", requiredField(primitive(VariableType.TIMESTAMP)))
                .putFields("nickname", nullableField(primitive(VariableType.STR)))
                .build();
        StructDef structDef = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("person"))
                .setDescription("A person response")
                .setStructDef(inlineStructDef)
                .build();

        var schema =
                JsonSchemaTransformer.fromStruct(structDef, Map.of(structDef.getId(), structDef));

        assertThat(schema.name()).isEqualTo("person");
        assertThat(schema.rootElement()).isInstanceOfSatisfying(JsonObjectSchema.class, root -> {
            assertThat(root.description()).isEqualTo("A person response");
            assertThat(root.additionalProperties()).isFalse();
            assertThat(root.required())
                    .containsExactlyInAnyOrder(
                            "name", "age", "score", "payload", "runId", "createdAt", "nickname");
            assertThat(root.properties())
                    .containsEntry("name", new JsonStringSchema())
                    .containsEntry("age", new JsonIntegerSchema())
                    .containsEntry("score", new JsonNumberSchema())
                    .containsEntry("active", new JsonBooleanSchema())
                    .containsEntry("payload", new JsonStringSchema())
                    .containsEntry("runId", new JsonStringSchema())
                    .containsEntry("createdAt", new JsonStringSchema())
                    .containsEntry(
                            "nickname",
                            JsonAnyOfSchema.builder()
                                    .anyOf(new JsonStringSchema(), new JsonNullSchema())
                                    .build());
        });
    }

    @Test
    void transformsNestedInlineStructsAndArrays() {
        InlineStructDef address = InlineStructDef.newBuilder()
                .putFields("city", requiredField(primitive(VariableType.STR)))
                .build();
        TypeDefinition addressType =
                TypeDefinition.newBuilder().setInlineStructDef(address).build();
        TypeDefinition addressesType = TypeDefinition.newBuilder()
                .setInlineArrayDef(InlineArrayDef.newBuilder().setArrayType(addressType))
                .build();
        StructDef root = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("contact"))
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields("addresses", requiredField(addressesType)))
                .build();

        var schema = JsonSchemaTransformer.fromStruct(root, Map.of(root.getId(), root));

        assertThat(schema.name()).isEqualTo("contact");
        assertThat(schema.rootElement()).isInstanceOfSatisfying(JsonObjectSchema.class, object -> {
            assertThat(object.required()).containsExactly("addresses");
            assertThat(object.properties().get("addresses"))
                    .isInstanceOfSatisfying(
                            JsonArraySchema.class, array -> assertThat(array.items())
                                    .isInstanceOfSatisfying(JsonObjectSchema.class, item -> {
                                        assertThat(item.required()).containsExactly("city");
                                        assertThat(item.properties())
                                                .containsEntry("city", new JsonStringSchema());
                                        assertThat(item.additionalProperties()).isFalse();
                                    }));
        });
    }

    @Test
    void rejectsReferencedStructsWhoseDefinitionIsUnavailable() {
        TypeDefinition reference = TypeDefinition.newBuilder()
                .setStructDefId(StructDefId.newBuilder().setName("address"))
                .build();
        StructDef root = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("contact"))
                .setStructDef(
                        InlineStructDef.newBuilder().putFields("address", requiredField(reference)))
                .build();

        assertThatThrownBy(() -> JsonSchemaTransformer.fromStruct(root, Map.of(root.getId(), root)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("StructDef 'address@0' is not present in the resolved definitions");
    }

    @Test
    void transformsReferencedStructDefsUsingCacheDefinitions() {
        StructDefId addressId =
                StructDefId.newBuilder().setName("address").setVersion(2).build();
        StructDef address = StructDef.newBuilder()
                .setId(addressId)
                .setDescription("A postal address")
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields("city", requiredField(primitive(VariableType.STR))))
                .build();
        StructDefId contactId =
                StructDefId.newBuilder().setName("contact").setVersion(4).build();
        StructDef contact = StructDef.newBuilder()
                .setId(contactId)
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields(
                                "address",
                                nullableField(TypeDefinition.newBuilder()
                                        .setStructDefId(addressId)
                                        .build())))
                .build();
        var schema = JsonSchemaTransformer.fromStruct(
                contact, Map.of(contactId, contact, addressId, address));

        assertThat(schema.rootElement()).isInstanceOfSatisfying(JsonObjectSchema.class, root -> {
            assertThat(root.properties().get("address"))
                    .isEqualTo(JsonAnyOfSchema.builder()
                            .anyOf(
                                    JsonReferenceSchema.builder()
                                            .reference("address_v2")
                                            .build(),
                                    new JsonNullSchema())
                            .build());
            assertThat(root.definitions())
                    .containsOnlyKeys("address_v2")
                    .extractingByKey("address_v2")
                    .isInstanceOfSatisfying(JsonObjectSchema.class, definition -> {
                        assertThat(definition.description()).isEqualTo("A postal address");
                        assertThat(definition.required()).containsExactly("city");
                        assertThat(definition.properties())
                                .containsEntry("city", new JsonStringSchema());
                    });
        });
    }

    private static TypeDefinition primitive(VariableType type) {
        return TypeDefinition.newBuilder().setPrimitiveType(type).build();
    }

    private static StructFieldDef requiredField(TypeDefinition type) {
        return StructFieldDef.newBuilder().setFieldType(type).build();
    }

    private static StructFieldDef optionalField(TypeDefinition type) {
        return StructFieldDef.newBuilder()
                .setFieldType(type)
                .setDefaultValue(VariableValue.newBuilder().setBool(true))
                .build();
    }

    private static StructFieldDef nullableField(TypeDefinition type) {
        return StructFieldDef.newBuilder()
                .setFieldType(type)
                .setIsNullable(true)
                .build();
    }
}
