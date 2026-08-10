package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.InlineArrayDef;
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

class StructTransformerTest {

    @Test
    void transformsJsonUsingTheResolvedStructDefinitionGraph() {
        StructDefId addressId = id("address", 2);
        StructDef address = struct(
                addressId,
                InlineStructDef.newBuilder()
                        .putFields("city", required(primitive(VariableType.STR)))
                        .build());
        InlineStructDef profile = InlineStructDef.newBuilder()
                .putFields("displayName", required(primitive(VariableType.STR)))
                .build();
        StructDefId rootId = id("agent-output", 4);
        StructDef root = struct(
                rootId,
                InlineStructDef.newBuilder()
                        .putFields("name", required(primitive(VariableType.STR)))
                        .putFields("count", required(primitive(VariableType.INT)))
                        .putFields("score", required(primitive(VariableType.DOUBLE)))
                        .putFields("active", required(primitive(VariableType.BOOL)))
                        .putFields("payload", required(primitive(VariableType.BYTES)))
                        .putFields("wfRunId", required(primitive(VariableType.WF_RUN_ID)))
                        .putFields("createdAt", required(primitive(VariableType.TIMESTAMP)))
                        .putFields("metadata", required(primitive(VariableType.JSON_OBJ)))
                        .putFields("legacyItems", required(primitive(VariableType.JSON_ARR)))
                        .putFields("tags", required(array(primitive(VariableType.STR))))
                        .putFields(
                                "scores",
                                required(map(
                                        primitive(VariableType.INT), primitive(VariableType.STR))))
                        .putFields("address", required(reference(addressId)))
                        .putFields(
                                "profile",
                                required(TypeDefinition.newBuilder()
                                        .setInlineStructDef(profile)
                                        .build()))
                        .putFields("nickname", nullable(primitive(VariableType.STR)))
                        .putFields(
                                "withDefault",
                                StructFieldDef.newBuilder()
                                        .setFieldType(primitive(VariableType.INT))
                                        .setDefaultValue(
                                                VariableValue.newBuilder().setInt(7))
                                        .build())
                        .build());

        InlineStruct result =
                StructTransformer.fromJson("""
                {
                  "name": "Ada",
                  "count": 42,
                  "score": "9.5",
                  "active": true,
                  "payload": "aGVsbG8=",
                  "wfRunId": "parent_child",
                  "createdAt": "2026-08-05T12:00:00Z",
                  "metadata": {"tone": "concise"},
                  "legacyItems": [1, "two"],
                  "tags": ["agent", "workflow"],
                  "scores": {"7": "seven"},
                  "address": {"city": "Quito"},
                  "profile": {"displayName": "Ada L."},
                  "nickname": null
                }
                """, root, Map.of(rootId, root, addressId, address));

        assertThat(value(result, "name").getStr()).isEqualTo("Ada");
        assertThat(value(result, "count").getInt()).isEqualTo(42);
        assertThat(value(result, "score").getDouble()).isEqualTo(9.5);
        assertThat(value(result, "active").getBool()).isTrue();
        assertThat(value(result, "payload").getBytes()).isEqualTo(ByteString.copyFromUtf8("hello"));
        assertThat(LHLibUtil.wfRunIdToString(value(result, "wfRunId").getWfRunId()))
                .isEqualTo("parent_child");
        assertThat(value(result, "createdAt").getUtcTimestamp())
                .isEqualTo(LHLibUtil.fromInstant(Instant.parse("2026-08-05T12:00:00Z")));
        assertThat(value(result, "metadata").getJsonObj()).isEqualTo("{\"tone\":\"concise\"}");
        assertThat(value(result, "legacyItems").getJsonArr()).isEqualTo("[1,\"two\"]");
        assertThat(value(result, "tags").getArray().getItemsList())
                .extracting(VariableValue::getStr)
                .containsExactly("agent", "workflow");
        assertThat(value(result, "tags").getArray().getElementType())
                .isEqualTo(primitive(VariableType.STR));
        assertThat(value(result, "scores").getMap().getMapType())
                .isEqualTo(mapDefinition(primitive(VariableType.INT), primitive(VariableType.STR)));
        assertThat(value(result, "scores").getMap().getEntries(0).getKey().getInt())
                .isEqualTo(7);
        assertThat(value(result, "scores").getMap().getEntries(0).getValue().getStr())
                .isEqualTo("seven");
        assertThat(value(result, "address").getStruct().getStructDefId()).isEqualTo(addressId);
        assertThat(value(result, "address")
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("city")
                        .getValue()
                        .getStr())
                .isEqualTo("Quito");
        assertThat(value(result, "profile").getStruct().getStructDefId())
                .isEqualTo(StructDefId.getDefaultInstance());
        assertThat(value(result, "profile")
                        .getStruct()
                        .getStruct()
                        .getFieldsOrThrow("displayName")
                        .getValue()
                        .getStr())
                .isEqualTo("Ada L.");
        assertThat(value(result, "nickname").getValueCase())
                .isEqualTo(VariableValue.ValueCase.VALUE_NOT_SET);
        assertThat(result.getFieldsMap()).doesNotContainKey("withDefault");
    }

    @Test
    void omitsAbsentNullableFields() {
        StructDef root = struct(
                id("result", 1),
                InlineStructDef.newBuilder()
                        .putFields("maybe", nullable(primitive(VariableType.STR)))
                        .build());

        InlineStruct result = StructTransformer.fromJson("{}", root, Map.of(root.getId(), root));

        assertThat(result.getFieldsMap()).isEmpty();
    }

    @Test
    void rejectsJsonThatDoesNotMatchTheStructDefinition() {
        StructDefId nestedId = id("nested", 1);
        StructDef root = struct(
                id("result", 1),
                InlineStructDef.newBuilder()
                        .putFields("required", required(primitive(VariableType.INT)))
                        .putFields("nested", required(reference(nestedId)))
                        .build());
        Map<StructDefId, StructDef> definitions = Map.of(root.getId(), root);

        assertThatThrownBy(() -> StructTransformer.fromJson("{", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON output");
        assertThatThrownBy(() ->
                        StructTransformer.fromJson("{required: 1, nested: {}}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON output");
        assertThatThrownBy(() -> StructTransformer.fromJson(
                        "{\"required\":1,\"nested\":{}} trailing", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON output");
        assertThatThrownBy(() -> StructTransformer.fromJson("[]", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("$: expected a JSON object");
        assertThatThrownBy(() -> StructTransformer.fromJson(
                        "{\"required\":1,\"nested\":{},\"extra\":true}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("$.extra: unknown Struct field");
        assertThatThrownBy(() -> StructTransformer.fromJson("{\"nested\":{}}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("$.required: missing required Struct field");
        assertThatThrownBy(() -> StructTransformer.fromJson(
                        "{\"required\":null,\"nested\":{}}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("$.required: field is not nullable");
        assertThatThrownBy(() -> StructTransformer.fromJson(
                        "{\"required\":\"1.5\",\"nested\":{}}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("$.required: invalid INT value");
        assertThatThrownBy(() -> StructTransformer.fromJson(
                        "{\"required\":1,\"nested\":{}}", root, definitions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "$.nested: StructDef 'nested@1' is not present in the resolved definitions");
    }

    private static StructDef struct(StructDefId id, InlineStructDef definition) {
        return StructDef.newBuilder().setId(id).setStructDef(definition).build();
    }

    private static StructDefId id(String name, int version) {
        return StructDefId.newBuilder().setName(name).setVersion(version).build();
    }

    private static StructFieldDef required(TypeDefinition type) {
        return StructFieldDef.newBuilder().setFieldType(type).build();
    }

    private static StructFieldDef nullable(TypeDefinition type) {
        return StructFieldDef.newBuilder()
                .setFieldType(type)
                .setIsNullable(true)
                .build();
    }

    private static TypeDefinition primitive(VariableType type) {
        return TypeDefinition.newBuilder().setPrimitiveType(type).build();
    }

    private static TypeDefinition reference(StructDefId id) {
        return TypeDefinition.newBuilder().setStructDefId(id).build();
    }

    private static TypeDefinition array(TypeDefinition elementType) {
        return TypeDefinition.newBuilder()
                .setInlineArrayDef(InlineArrayDef.newBuilder().setArrayType(elementType))
                .build();
    }

    private static TypeDefinition map(TypeDefinition keyType, TypeDefinition valueType) {
        return TypeDefinition.newBuilder()
                .setInlineMapDef(mapDefinition(keyType, valueType))
                .build();
    }

    private static InlineMapDef mapDefinition(TypeDefinition keyType, TypeDefinition valueType) {
        return InlineMapDef.newBuilder()
                .setKeyType(keyType)
                .setValueType(valueType)
                .build();
    }

    private static VariableValue value(InlineStruct struct, String fieldName) {
        return struct.getFieldsOrThrow(fieldName).getValue();
    }
}
