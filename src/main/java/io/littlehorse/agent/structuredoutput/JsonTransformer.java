package io.littlehorse.agent.structuredoutput;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.VariableValue;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public final class JsonTransformer {

    private JsonTransformer() {}

    /**
     * Converts a validated LittleHorse struct payload into a Gson JSON object.
     *
     * @param inlineStruct payload containing the struct fields
     * @return JSON tree whose properties are the struct's field names and values
     */
    public static JsonElement fromStruct(InlineStruct inlineStruct) {
        Objects.requireNonNull(inlineStruct, "input struct must not be null");
        JsonObject result = new JsonObject();
        inlineStruct.getFieldsMap().forEach((name, field) -> result.add(name, toJsonTree(field)));
        return result;
    }

    /**
     * Converts one LittleHorse struct field to JSON while preserving an absent value as null.
     *
     * @param field field containing a LittleHorse variable value
     * @return JSON tree representation of the field value
     */
    private static JsonElement toJsonTree(StructField field) {
        return field.hasValue() ? toJsonTree(field.getValue()) : JsonNull.INSTANCE;
    }

    /**
     * Maps every native LittleHorse variable type to its corresponding JSON representation.
     *
     * @param value LittleHorse value to convert
     * @return JSON tree representation of the native value
     */
    private static JsonElement toJsonTree(VariableValue value) {
        return switch (value.getValueCase()) {
            case JSON_OBJ -> parseJsonObject(value.getJsonObj());
            case JSON_ARR -> parseJsonArray(value.getJsonArr());
            case DOUBLE -> new JsonPrimitive(finiteDouble(value.getDouble()));
            case BOOL -> new JsonPrimitive(value.getBool());
            case STR -> new JsonPrimitive(value.getStr());
            case INT -> new JsonPrimitive(value.getInt());
            case BYTES ->
                new JsonPrimitive(
                        Base64.getEncoder().encodeToString(value.getBytes().toByteArray()));
            case WF_RUN_ID -> new JsonPrimitive(LHLibUtil.wfRunIdToString(value.getWfRunId()));
            case UTC_TIMESTAMP ->
                new JsonPrimitive(Instant.ofEpochSecond(
                                value.getUtcTimestamp().getSeconds(),
                                value.getUtcTimestamp().getNanos())
                        .toString());
            case STRUCT -> fromStruct(value.getStruct().getStruct());
            case ARRAY -> toJsonTree(value.getArray());
            case MAP -> toJsonTree(value.getMap());
            case VALUE_NOT_SET -> JsonNull.INSTANCE;
        };
    }

    private static JsonArray toJsonTree(io.littlehorse.sdk.common.proto.Array array) {
        JsonArray result = new JsonArray();
        array.getItemsList().forEach(item -> result.add(toJsonTree(item)));
        return result;
    }

    private static JsonObject toJsonTree(io.littlehorse.sdk.common.proto.Map map) {
        JsonObject result = new JsonObject();
        map.getEntriesList()
                .forEach(entry -> result.add(
                        toJsonPropertyName(entry.getKey()), toJsonTree(entry.getValue())));
        return result;
    }

    private static JsonObject parseJsonObject(String value) {
        JsonElement parsed = JsonParser.parseString(value);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("Expected a JSON object but found: " + value);
        }
        return parsed.getAsJsonObject();
    }

    private static JsonArray parseJsonArray(String value) {
        JsonElement parsed = JsonParser.parseString(value);
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("Expected a JSON array but found: " + value);
        }
        return parsed.getAsJsonArray();
    }

    /**
     * Converts a native LittleHorse map's primitive key into a JSON object property name.
     *
     * @param key primitive LittleHorse map key
     * @return unquoted text representation of the key
     */
    private static String toJsonPropertyName(VariableValue key) {
        return switch (key.getValueCase()) {
            case DOUBLE -> Double.toString(finiteDouble(key.getDouble()));
            case BOOL -> Boolean.toString(key.getBool());
            case STR -> key.getStr();
            case INT -> Long.toString(key.getInt());
            case BYTES -> Base64.getEncoder().encodeToString(key.getBytes().toByteArray());
            case WF_RUN_ID -> LHLibUtil.wfRunIdToString(key.getWfRunId());
            case UTC_TIMESTAMP ->
                Instant.ofEpochSecond(
                                key.getUtcTimestamp().getSeconds(),
                                key.getUtcTimestamp().getNanos())
                        .toString();
            case VALUE_NOT_SET -> "null";
            case JSON_OBJ, JSON_ARR, STRUCT, ARRAY, MAP ->
                throw new IllegalArgumentException(
                        "Cannot serialize a non-primitive LittleHorse map key as JSON: "
                                + key.getValueCase());
        };
    }

    /**
     * Validates a JSON number and rejects non-finite doubles, which JSON cannot represent.
     *
     * @param value double to serialize
     * @return the validated finite double
     */
    private static double finiteDouble(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Cannot serialize a non-finite number as JSON: " + value);
        }
        return value;
    }
}
