package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.Struct;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.VariableValue;
import io.littlehorse.sdk.common.proto.WfRunId;

import org.junit.jupiter.api.Test;

class JsonTransformerTest {

    @Test
    void serializesEveryScalarEmbeddedJsonAndNullValue() {
        WfRunId wfRunId = WfRunId.newBuilder()
                .setId("child")
                .setParentWfRunId(WfRunId.newBuilder().setId("parent"))
                .build();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(1_700_000_000L)
                .setNanos(123_456_789)
                .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("string", field(VariableValue.newBuilder().setStr("Ada")))
                .putFields("integer", field(VariableValue.newBuilder().setInt(Long.MAX_VALUE)))
                .putFields("double", field(VariableValue.newBuilder().setDouble(3.25)))
                .putFields("boolean", field(VariableValue.newBuilder().setBool(true)))
                .putFields(
                        "bytes",
                        field(VariableValue.newBuilder()
                                .setBytes(ByteString.copyFrom(new byte[] {0, 1, 2, (byte) 0xff}))))
                .putFields("wfRunId", field(VariableValue.newBuilder().setWfRunId(wfRunId)))
                .putFields(
                        "timestamp", field(VariableValue.newBuilder().setUtcTimestamp(timestamp)))
                .putFields(
                        "jsonObject",
                        field(VariableValue.newBuilder()
                                .setJsonObj("{\"nested\":{\"count\":2},\"active\":true}")))
                .putFields(
                        "jsonArray",
                        field(VariableValue.newBuilder().setJsonArr("[\"first\",2,false,null]")))
                .putFields("absentField", StructField.getDefaultInstance())
                .putFields("unsetValue", field(VariableValue.newBuilder()))
                .build();

        JsonObject expected = JsonParser.parseString("""
                        {
                          "string": "Ada",
                          "integer": 9223372036854775807,
                          "double": 3.25,
                          "boolean": true,
                          "bytes": "AAEC/w==",
                          "timestamp": "2023-11-14T22:13:20.123456789Z",
                          "jsonObject": {"nested": {"count": 2}, "active": true},
                          "jsonArray": ["first", 2, false, null],
                          "absentField": null,
                          "unsetValue": null
                        }
                        """).getAsJsonObject();
        expected.addProperty("wfRunId", LHLibUtil.wfRunIdToString(wfRunId));

        assertThat(JsonTransformer.fromStruct(input)).isEqualTo(expected);
    }

    @Test
    void serializesNestedStructsArraysMapsAndEmbeddedJsonRecursively() {
        InlineStruct leaf = InlineStruct.newBuilder()
                .putFields("enabled", field(VariableValue.newBuilder().setBool(true)))
                .putFields(
                        "details",
                        field(VariableValue.newBuilder().setJsonObj("{\"scores\":[1,2.5,null]}")))
                .build();
        io.littlehorse.sdk.common.proto.Array mixedArray =
                io.littlehorse.sdk.common.proto.Array.newBuilder()
                        .addItems(VariableValue.newBuilder().setStr("first"))
                        .addItems(VariableValue.newBuilder().setInt(42))
                        .addItems(VariableValue.getDefaultInstance())
                        .addItems(VariableValue.newBuilder()
                                .setStruct(Struct.newBuilder().setStruct(leaf)))
                        .build();
        io.littlehorse.sdk.common.proto.Map lookup =
                io.littlehorse.sdk.common.proto.Map.newBuilder()
                        .addEntries(io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                                .setKey(VariableValue.newBuilder().setStr("items"))
                                .setValue(VariableValue.newBuilder().setArray(mixedArray)))
                        .addEntries(
                                io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                                        .setKey(VariableValue.newBuilder().setStr("metadata"))
                                        .setValue(
                                                VariableValue.newBuilder()
                                                        .setJsonObj(
                                                                "{\"source\":\"workflow\",\"labels\":[\"one\",\"two\"]}")))
                        .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields(
                        "nestedStruct",
                        field(VariableValue.newBuilder()
                                .setStruct(Struct.newBuilder().setStruct(leaf))))
                .putFields("nestedArray", field(VariableValue.newBuilder().setArray(mixedArray)))
                .putFields("nestedMap", field(VariableValue.newBuilder().setMap(lookup)))
                .build();

        assertThat(JsonTransformer.fromStruct(input)).isEqualTo(JsonParser.parseString("""
                        {
                          "nestedStruct": {
                            "enabled": true,
                            "details": {"scores": [1, 2.5, null]}
                          },
                          "nestedArray": [
                            "first",
                            42,
                            null,
                            {
                              "enabled": true,
                              "details": {"scores": [1, 2.5, null]}
                            }
                          ],
                          "nestedMap": {
                            "items": [
                              "first",
                              42,
                              null,
                              {
                                "enabled": true,
                                "details": {"scores": [1, 2.5, null]}
                              }
                            ],
                            "metadata": {
                              "source": "workflow",
                              "labels": ["one", "two"]
                            }
                          }
                        }
                        """));
    }

    @Test
    void escapesJsonFieldNamesAndStringValues() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields(
                        "quoted\"field",
                        field(VariableValue.newBuilder().setStr("line\nslash\\tab\t")))
                .build();

        assertThat(JsonTransformer.fromStruct(input))
                .isEqualTo(
                        JsonParser.parseString("{\"quoted\\\"field\":\"line\\nslash\\\\tab\\t\"}"));
    }

    @Test
    void rejectsNonFiniteNumbersThatJsonCannotRepresent() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("number", field(VariableValue.newBuilder().setDouble(Double.NaN)))
                .build();

        assertThatThrownBy(() -> JsonTransformer.fromStruct(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-finite number");
    }

    @Test
    void serializesNativeLittleHorseMapsAsJsonObjects() {
        io.littlehorse.sdk.common.proto.Map map = io.littlehorse.sdk.common.proto.Map.newBuilder()
                .addEntries(io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                        .setKey(VariableValue.newBuilder().setInt(7))
                        .setValue(VariableValue.newBuilder().setStr("seven")))
                .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("lookup", field(VariableValue.newBuilder().setMap(map)))
                .build();

        assertThat(JsonTransformer.fromStruct(input))
                .isEqualTo(JsonParser.parseString("{\"lookup\":{\"7\":\"seven\"}}"));
    }

    @Test
    void convertsEveryPrimitiveNativeMapKeyToAJsonPropertyName() {
        WfRunId wfRunId = WfRunId.newBuilder().setId("run-123").build();
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(1_700_000_000L).build();
        io.littlehorse.sdk.common.proto.Map map = io.littlehorse.sdk.common.proto.Map.newBuilder()
                .addEntries(entry(VariableValue.newBuilder().setStr("text"), "string key"))
                .addEntries(entry(VariableValue.newBuilder().setInt(7), "integer key"))
                .addEntries(entry(VariableValue.newBuilder().setDouble(2.5), "double key"))
                .addEntries(entry(VariableValue.newBuilder().setBool(true), "boolean key"))
                .addEntries(entry(
                        VariableValue.newBuilder().setBytes(ByteString.copyFromUtf8("binary-key")),
                        "bytes key"))
                .addEntries(entry(VariableValue.newBuilder().setWfRunId(wfRunId), "workflow key"))
                .addEntries(entry(
                        VariableValue.newBuilder().setUtcTimestamp(timestamp), "timestamp key"))
                .addEntries(entry(VariableValue.newBuilder(), "unset key"))
                .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("lookup", field(VariableValue.newBuilder().setMap(map)))
                .build();

        JsonObject expectedLookup = new JsonObject();
        expectedLookup.addProperty("text", "string key");
        expectedLookup.addProperty("7", "integer key");
        expectedLookup.addProperty("2.5", "double key");
        expectedLookup.addProperty("true", "boolean key");
        expectedLookup.addProperty("YmluYXJ5LWtleQ==", "bytes key");
        expectedLookup.addProperty(LHLibUtil.wfRunIdToString(wfRunId), "workflow key");
        expectedLookup.addProperty("2023-11-14T22:13:20Z", "timestamp key");
        expectedLookup.addProperty("null", "unset key");

        assertThat(JsonTransformer.fromStruct(input).getAsJsonObject().getAsJsonObject("lookup"))
                .isEqualTo(expectedLookup);
    }

    @Test
    void rejectsNonPrimitiveNativeMapKeys() {
        io.littlehorse.sdk.common.proto.Map map = io.littlehorse.sdk.common.proto.Map.newBuilder()
                .addEntries(io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                        .setKey(VariableValue.newBuilder().setJsonObj("{}"))
                        .setValue(VariableValue.newBuilder().setStr("value")))
                .build();
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("lookup", field(VariableValue.newBuilder().setMap(map)))
                .build();

        assertThatThrownBy(() -> JsonTransformer.fromStruct(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-primitive LittleHorse map key")
                .hasMessageContaining("JSON_OBJ");
    }

    @Test
    void rejectsEmbeddedJsonWhoseShapeDoesNotMatchItsLittleHorseType() {
        InlineStruct objectContainingArray = InlineStruct.newBuilder()
                .putFields("value", field(VariableValue.newBuilder().setJsonObj("[]")))
                .build();
        InlineStruct arrayContainingObject = InlineStruct.newBuilder()
                .putFields("value", field(VariableValue.newBuilder().setJsonArr("{}")))
                .build();

        assertThatThrownBy(() -> JsonTransformer.fromStruct(objectContainingArray))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a JSON object");
        assertThatThrownBy(() -> JsonTransformer.fromStruct(arrayContainingObject))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a JSON array");
    }

    private static io.littlehorse.sdk.common.proto.Map.Entry entry(
            VariableValue.Builder key, String value) {
        return io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                .setKey(key)
                .setValue(VariableValue.newBuilder().setStr(value))
                .build();
    }

    private static StructField field(VariableValue.Builder value) {
        return StructField.newBuilder().setValue(value).build();
    }
}
