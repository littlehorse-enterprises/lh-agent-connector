package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.anthropic.internal.mapper.AnthropicMapper;
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
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class JsonSchemaTransformerTest {

    @Test
    void transformsStructMetadataAndPrimitiveFields() {
        InlineStructDef inlineStructDef = InlineStructDef.newBuilder()
                .putFields("name", requiredField(primitive(VariableType.STR)))
                .putFields("age", requiredField(primitive(VariableType.INT)))
                .putFields("score", requiredField(primitive(VariableType.DOUBLE)))
                .putFields("active", optionalField(primitive(VariableType.BOOL)))
                .putFields(
                        "payload",
                        requiredField(primitive(VariableType.BYTES)).toBuilder()
                                .setDescription("  ")
                                .build())
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
    void mapsFieldDescriptionsToPropertySchemas() {
        TypeDefinition addressType = TypeDefinition.newBuilder()
                .setInlineStructDef(InlineStructDef.newBuilder()
                        .putFields(
                                "city", describedField(primitive(VariableType.STR), "City name")))
                .build();
        TypeDefinition addressesType = TypeDefinition.newBuilder()
                .setInlineArrayDef(InlineArrayDef.newBuilder().setArrayType(addressType))
                .build();
        TypeDefinition mapType = TypeDefinition.newBuilder()
                .setInlineMapDef(InlineMapDef.newBuilder()
                        .setKeyType(primitive(VariableType.STR))
                        .setValueType(primitive(VariableType.STR)))
                .build();
        StructDef root = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("contact"))
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields("name", describedField(primitive(VariableType.STR), "Full name"))
                        .putFields(
                                "active",
                                optionalField(primitive(VariableType.BOOL)).toBuilder()
                                        .setDescription("Whether active")
                                        .build())
                        .putFields(
                                "nickname",
                                nullableField(primitive(VariableType.STR)).toBuilder()
                                        .setDescription("Preferred name")
                                        .build())
                        .putFields("address", describedField(addressType, "Home address"))
                        .putFields("addresses", describedField(addressesType, "Past addresses"))
                        .putFields("attributes", describedField(mapType, "Other attributes")))
                .build();

        var schema = JsonSchemaTransformer.fromStruct(root, Map.of(root.getId(), root));

        assertThat(schema.rootElement()).isInstanceOfSatisfying(JsonObjectSchema.class, object -> {
            assertThat(object.required())
                    .containsExactlyInAnyOrder(
                            "name", "nickname", "address", "addresses", "attributes");
            assertThat(object.properties().get("name"))
                    .isEqualTo(
                            JsonStringSchema.builder().description("Full name").build());
            assertThat(object.properties().get("active"))
                    .isEqualTo(JsonBooleanSchema.builder()
                            .description("Whether active")
                            .build());
            assertThat(object.properties().get("nickname"))
                    .isEqualTo(JsonAnyOfSchema.builder()
                            .description("Preferred name")
                            .anyOf(new JsonStringSchema(), new JsonNullSchema())
                            .build());
            assertThat(object.properties().get("address"))
                    .isInstanceOfSatisfying(JsonObjectSchema.class, address -> {
                        assertThat(address.description()).isEqualTo("Home address");
                        assertThat(address.properties().get("city").description())
                                .isEqualTo("City name");
                    });
            assertThat(object.properties().get("addresses"))
                    .isInstanceOfSatisfying(JsonArraySchema.class, addresses -> {
                        assertThat(addresses.description()).isEqualTo("Past addresses");
                        assertThat(addresses.items().description()).isNull();
                    });
            assertThat(object.properties().get("attributes"))
                    .isInstanceOfSatisfying(JsonObjectSchema.class, attributes -> {
                        assertThat(attributes.description()).isEqualTo("Other attributes");
                        assertThat(attributes.additionalProperties()).isTrue();
                    });
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

    @Test
    void keepsReferencedFieldDescriptionsSeparateFromTheSharedDefinition() {
        StructDefId addressId =
                StructDefId.newBuilder().setName("address").setVersion(2).build();
        StructDef address = StructDef.newBuilder()
                .setId(addressId)
                .setDescription("A postal address")
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields("city", requiredField(primitive(VariableType.STR))))
                .build();
        TypeDefinition reference =
                TypeDefinition.newBuilder().setStructDefId(addressId).build();
        StructDef root = StructDef.newBuilder()
                .setId(StructDefId.newBuilder().setName("contact"))
                .setStructDef(InlineStructDef.newBuilder()
                        .putFields("home", describedField(reference, "Home address"))
                        .putFields("office", describedField(reference, "Office address"))
                        .putFields("backup", requiredField(reference))
                        .putFields(
                                "mailing",
                                nullableField(reference).toBuilder()
                                        .setDescription("Mailing address")
                                        .build()))
                .build();

        var schema = JsonSchemaTransformer.fromStruct(
                root, Map.of(root.getId(), root, addressId, address));

        assertThat(schema.rootElement()).isInstanceOfSatisfying(JsonObjectSchema.class, object -> {
            JsonReferenceSchema addressReference =
                    JsonReferenceSchema.builder().reference("address_v2").build();
            assertThat(object.properties().get("home"))
                    .isEqualTo(JsonAnyOfSchema.builder()
                            .description("Home address")
                            .anyOf(addressReference)
                            .build());
            assertThat(object.properties().get("office").description()).isEqualTo("Office address");
            assertThat(object.properties().get("backup")).isEqualTo(addressReference);
            assertThat(object.properties().get("mailing"))
                    .isEqualTo(JsonAnyOfSchema.builder()
                            .description("Mailing address")
                            .anyOf(addressReference, new JsonNullSchema())
                            .build());
            assertThat(object.definitions().get("address_v2").description())
                    .isEqualTo("A postal address");

            for (Map<String, Object> providerSchema : List.of(
                    JsonSchemaElementUtils.toMap(object, false),
                    JsonSchemaElementUtils.toMap(object, true),
                    AnthropicMapper.toAnthropicSchema(object))) {
                JsonObject serialized = JsonParser.parseString(new Gson().toJson(providerSchema))
                        .getAsJsonObject();
                JsonObject home = serialized.getAsJsonObject("properties").getAsJsonObject("home");
                assertThat(home.get("description").getAsString()).isEqualTo("Home address");
                assertThat(home.getAsJsonArray("anyOf")
                                .get(0)
                                .getAsJsonObject()
                                .get("$ref")
                                .getAsString())
                        .isEqualTo("#/$defs/address_v2");
                JsonObject mailing =
                        serialized.getAsJsonObject("properties").getAsJsonObject("mailing");
                assertThat(mailing.get("description").getAsString()).isEqualTo("Mailing address");
                assertThat(mailing.getAsJsonArray("anyOf").size()).isEqualTo(2);
                assertThat(serialized
                                .getAsJsonObject("$defs")
                                .getAsJsonObject("address_v2")
                                .get("description")
                                .getAsString())
                        .isEqualTo("A postal address");
            }
        });
    }

    private static TypeDefinition primitive(VariableType type) {
        return TypeDefinition.newBuilder().setPrimitiveType(type).build();
    }

    private static StructFieldDef requiredField(TypeDefinition type) {
        return StructFieldDef.newBuilder().setFieldType(type).build();
    }

    private static StructFieldDef describedField(TypeDefinition type, String description) {
        return StructFieldDef.newBuilder()
                .setFieldType(type)
                .setDescription(description)
                .build();
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
