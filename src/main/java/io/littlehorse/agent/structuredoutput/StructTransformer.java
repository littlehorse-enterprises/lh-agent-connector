package io.littlehorse.agent.structuredoutput;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.protobuf.ByteString;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.Array;
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.Struct;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import java.io.IOException;
import java.io.StringReader;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** Transforms a JSON object into a LittleHorse InlineStruct using its StructDef graph. */
public final class StructTransformer {

    private StructTransformer() {}

    public static InlineStruct fromJson(
            String json, StructDef root, Map<StructDefId, StructDef> definitionsById) {
        Objects.requireNonNull(json, "JSON must not be null");
        Objects.requireNonNull(root, "root StructDef must not be null");
        Objects.requireNonNull(definitionsById, "StructDef definitions must not be null");

        JsonElement rootElement;
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            rootElement = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonParseException("unexpected content after the root JSON value");
            }
        } catch (JsonParseException | IOException exception) {
            throw new IllegalArgumentException(
                    "Invalid JSON output: " + exception.getMessage(), exception);
        }

        return toInlineStruct(
                requireObject(rootElement, "$"), root.getStructDef(), definitionsById, "$");
    }

    private static InlineStruct toInlineStruct(
            JsonObject object,
            InlineStructDef definition,
            Map<StructDefId, StructDef> definitionsById,
            String path) {
        for (String fieldName : object.keySet()) {
            if (!definition.containsFields(fieldName)) {
                throw failure(fieldPath(path, fieldName), "unknown Struct field");
            }
        }

        InlineStruct.Builder result = InlineStruct.newBuilder();
        definition.getFieldsMap().forEach((fieldName, fieldDefinition) -> {
            String currentPath = fieldPath(path, fieldName);
            if (!object.has(fieldName)) {
                if (fieldDefinition.hasDefaultValue() || fieldDefinition.getIsNullable()) {
                    return;
                }
                throw failure(currentPath, "missing required Struct field");
            }

            JsonElement fieldValue = object.get(fieldName);
            if (fieldValue.isJsonNull()) {
                if (!fieldDefinition.getIsNullable()) {
                    throw failure(currentPath, "field is not nullable");
                }
                result.putFields(
                        fieldName,
                        StructField.newBuilder()
                                .setValue(VariableValue.getDefaultInstance())
                                .build());
                return;
            }

            result.putFields(
                    fieldName,
                    StructField.newBuilder()
                            .setValue(toVariableValue(
                                    fieldValue,
                                    fieldDefinition.getFieldType(),
                                    definitionsById,
                                    currentPath))
                            .build());
        });
        return result.build();
    }

    private static VariableValue toVariableValue(
            JsonElement element,
            TypeDefinition definition,
            Map<StructDefId, StructDef> definitionsById,
            String path) {
        if (element.isJsonNull()) {
            return VariableValue.getDefaultInstance();
        }

        return switch (definition.getDefinedTypeCase()) {
            case PRIMITIVE_TYPE -> toPrimitive(element, definition.getPrimitiveType(), path);
            case INLINE_ARRAY_DEF ->
                toArray(
                        element,
                        definition.getInlineArrayDef().getArrayType(),
                        definitionsById,
                        path);
            case INLINE_MAP_DEF ->
                toMap(element, definition.getInlineMapDef(), definitionsById, path);
            case STRUCT_DEF_ID ->
                toReferencedStruct(element, definition.getStructDefId(), definitionsById, path);
            case INLINE_STRUCT_DEF ->
                VariableValue.newBuilder()
                        .setStruct(Struct.newBuilder()
                                .setStruct(toInlineStruct(
                                        requireObject(element, path),
                                        definition.getInlineStructDef(),
                                        definitionsById,
                                        path)))
                        .build();
            case DEFINEDTYPE_NOT_SET -> toPrimitive(element, VariableType.JSON_OBJ, path);
        };
    }

    private static VariableValue toArray(
            JsonElement element,
            TypeDefinition elementDefinition,
            Map<StructDefId, StructDef> definitionsById,
            String path) {
        if (!element.isJsonArray()) {
            throw failure(path, "expected a JSON array");
        }

        Array.Builder array = Array.newBuilder().setElementType(elementDefinition);
        for (int index = 0; index < element.getAsJsonArray().size(); index++) {
            array.addItems(toVariableValue(
                    element.getAsJsonArray().get(index),
                    elementDefinition,
                    definitionsById,
                    path + "[" + index + "]"));
        }
        return VariableValue.newBuilder().setArray(array).build();
    }

    private static VariableValue toMap(
            JsonElement element,
            InlineMapDef definition,
            Map<StructDefId, StructDef> definitionsById,
            String path) {
        JsonObject object = requireObject(element, path);
        TypeDefinition keyDefinition = definition.getKeyType();
        if (keyDefinition.getDefinedTypeCase() != TypeDefinition.DefinedTypeCase.PRIMITIVE_TYPE) {
            throw failure(path, "LittleHorse Map keys must use a primitive type");
        }

        io.littlehorse.sdk.common.proto.Map.Builder map =
                io.littlehorse.sdk.common.proto.Map.newBuilder().setMapType(definition);
        object.entrySet().forEach(entry -> {
            String currentPath = fieldPath(path, entry.getKey());
            VariableValue key = toPrimitive(
                    new com.google.gson.JsonPrimitive(entry.getKey()),
                    keyDefinition.getPrimitiveType(),
                    currentPath + " (key)");
            VariableValue value = toVariableValue(
                    entry.getValue(), definition.getValueType(), definitionsById, currentPath);
            map.addEntries(io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                    .setKey(key)
                    .setValue(value));
        });
        return VariableValue.newBuilder().setMap(map).build();
    }

    private static VariableValue toReferencedStruct(
            JsonElement element,
            StructDefId id,
            Map<StructDefId, StructDef> definitionsById,
            String path) {
        StructDef definition = definitionsById.get(id);
        if (definition == null) {
            throw failure(
                    path,
                    "StructDef '%s@%d' is not present in the resolved definitions"
                            .formatted(id.getName(), id.getVersion()));
        }

        InlineStruct value = toInlineStruct(
                requireObject(element, path), definition.getStructDef(), definitionsById, path);
        return VariableValue.newBuilder()
                .setStruct(Struct.newBuilder().setStructDefId(id).setStruct(value))
                .build();
    }

    private static VariableValue toPrimitive(JsonElement element, VariableType type, String path) {
        VariableValue.Builder result = VariableValue.newBuilder();
        try {
            return switch (type) {
                case JSON_OBJ -> {
                    requireObject(element, path);
                    yield result.setJsonObj(element.toString()).build();
                }
                case JSON_ARR -> {
                    if (!element.isJsonArray()) {
                        throw failure(path, "expected a JSON array");
                    }
                    yield result.setJsonArr(element.toString()).build();
                }
                case DOUBLE -> {
                    double value = Double.parseDouble(requireScalar(element, path));
                    if (!Double.isFinite(value)) {
                        throw failure(path, "expected a finite DOUBLE");
                    }
                    yield result.setDouble(value).build();
                }
                case BOOL -> {
                    String value = requireScalar(element, path);
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw failure(path, "expected a BOOL");
                    }
                    yield result.setBool(Boolean.parseBoolean(value)).build();
                }
                case STR -> result.setStr(requireScalar(element, path)).build();
                case INT ->
                    result.setInt(Long.parseLong(requireScalar(element, path))).build();
                case BYTES ->
                    result.setBytes(ByteString.copyFrom(
                                    Base64.getDecoder().decode(requireScalar(element, path))))
                            .build();
                case WF_RUN_ID ->
                    result.setWfRunId(LHLibUtil.wfRunIdFromString(requireScalar(element, path)))
                            .build();
                case TIMESTAMP ->
                    result.setUtcTimestamp(parseTimestamp(requireScalar(element, path), path))
                            .build();
                case UNRECOGNIZED ->
                    throw failure(path, "cannot transform an unrecognized LittleHorse type");
            };
        } catch (NumberFormatException exception) {
            throw failure(path, "invalid " + type + " value", exception);
        } catch (IllegalArgumentException exception) {
            if (exception instanceof TransformationException) {
                throw exception;
            }
            throw failure(path, "invalid " + type + " value", exception);
        }
    }

    private static com.google.protobuf.Timestamp parseTimestamp(String value, String path) {
        try {
            Instant instant;
            try {
                instant = Instant.ofEpochMilli(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                instant = Instant.parse(value);
            }
            return LHLibUtil.fromInstant(instant);
        } catch (DateTimeException exception) {
            throw failure(path, "invalid TIMESTAMP value", exception);
        }
    }

    private static JsonObject requireObject(JsonElement element, String path) {
        if (!element.isJsonObject()) {
            throw failure(path, "expected a JSON object");
        }
        return element.getAsJsonObject();
    }

    private static String requireScalar(JsonElement element, String path) {
        if (!element.isJsonPrimitive()) {
            throw failure(path, "expected a scalar JSON value");
        }
        return element.getAsJsonPrimitive().getAsString();
    }

    private static String fieldPath(String path, String fieldName) {
        if (fieldName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return path + "." + fieldName;
        }
        return path + "[\"" + fieldName.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
    }

    private static TransformationException failure(String path, String message) {
        return new TransformationException(path + ": " + message);
    }

    private static TransformationException failure(String path, String message, Throwable cause) {
        return new TransformationException(path + ": " + message, cause);
    }

    private static final class TransformationException extends IllegalArgumentException {

        private TransformationException(String message) {
            super(message);
        }

        private TransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
