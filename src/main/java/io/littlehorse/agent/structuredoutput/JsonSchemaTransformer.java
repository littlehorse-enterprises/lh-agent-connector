package io.littlehorse.agent.structuredoutput;

import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;

import java.util.*;
import java.util.stream.Collectors;

/** Transforms a LittleHorse Struct definition into a LangChain4j JSON schema. */
final class JsonSchemaTransformer {

    private JsonSchemaTransformer() {}

    public static JsonSchema fromStruct(
            StructDef structDef, Map<StructDefId, StructDef> definitionsById) {
        Objects.requireNonNull(structDef, "struct definition must not be null");
        Objects.requireNonNull(definitionsById, "StructDef definitions must not be null");

        JsonObjectSchema rootElement = toJsonObjectSchema(structDef, definitionsById);
        Set<StructDefId> referencedIds = definitionsById.values().stream()
                .flatMap(definition -> referencedStructDefIds(definition.getStructDef()).stream())
                .collect(Collectors.toSet());
        if (!referencedIds.isEmpty()) {
            Map<String, JsonSchemaElement> definitions = new LinkedHashMap<>();
            referencedIds.forEach(id -> definitions.put(
                    referenceName(id),
                    toJsonObjectSchema(requireDefinition(id, definitionsById), definitionsById)));
            rootElement = rootElement.toBuilder().definitions(definitions).build();
        }

        return JsonSchema.builder()
                .name(structDef.getId().getName())
                .rootElement(rootElement)
                .build();
    }

    private static JsonObjectSchema toJsonObjectSchema(
            StructDef structDef, Map<StructDefId, StructDef> definitionsById) {
        JsonObjectSchema schema = toJsonObjectSchema(structDef.getStructDef(), definitionsById);
        if (!structDef.hasDescription()) {
            return schema;
        }
        return schema.toBuilder().description(structDef.getDescription()).build();
    }

    private static JsonObjectSchema toJsonObjectSchema(
            InlineStructDef structDef, Map<StructDefId, StructDef> definitionsById) {
        JsonObjectSchema.Builder schema = JsonObjectSchema.builder().additionalProperties(false);
        List<String> required = new ArrayList<>();

        structDef.getFieldsMap().forEach((fieldName, fieldDef) -> {
            schema.addProperty(fieldName, toJsonSchemaElement(fieldDef, definitionsById));
            if (!fieldDef.hasDefaultValue()) {
                required.add(fieldName);
            }
        });

        return schema.required(required).build();
    }

    private static JsonSchemaElement toJsonSchemaElement(
            StructFieldDef fieldDef, Map<StructDefId, StructDef> definitionsById) {
        JsonSchemaElement fieldSchema =
                toJsonSchemaElement(fieldDef.getFieldType(), definitionsById);
        if (!fieldDef.getIsNullable()) {
            return fieldSchema;
        }

        return JsonAnyOfSchema.builder()
                .anyOf(fieldSchema, new JsonNullSchema())
                .build();
    }

    private static JsonSchemaElement toJsonSchemaElement(
            TypeDefinition typeDefinition, Map<StructDefId, StructDef> definitionsById) {
        return switch (typeDefinition.getDefinedTypeCase()) {
            case PRIMITIVE_TYPE -> toJsonSchemaElement(typeDefinition.getPrimitiveType());
            case INLINE_ARRAY_DEF ->
                JsonArraySchema.builder()
                        .items(toJsonSchemaElement(
                                typeDefinition.getInlineArrayDef().getArrayType(), definitionsById))
                        .build();
            case INLINE_STRUCT_DEF ->
                toJsonObjectSchema(typeDefinition.getInlineStructDef(), definitionsById);
            case INLINE_MAP_DEF ->
                JsonObjectSchema.builder()
                        .required(List.of())
                        .additionalProperties(true)
                        .build();
            case STRUCT_DEF_ID -> toJsonReference(typeDefinition.getStructDefId(), definitionsById);
            case DEFINEDTYPE_NOT_SET -> toJsonSchemaElement(VariableType.JSON_OBJ);
        };
    }

    private static JsonSchemaElement toJsonReference(
            StructDefId id, Map<StructDefId, StructDef> definitionsById) {
        if (definitionsById == null) {
            throw new IllegalArgumentException(
                    "Cannot transform referenced StructDef '%s' without its definition"
                            .formatted(id.getName()));
        }
        requireDefinition(id, definitionsById);
        return JsonReferenceSchema.builder().reference(referenceName(id)).build();
    }

    private static StructDef requireDefinition(
            StructDefId id, Map<StructDefId, StructDef> definitionsById) {
        StructDef definition = definitionsById.get(id);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "StructDef '%s@%d' is not present in the resolved definitions"
                            .formatted(id.getName(), id.getVersion()));
        }
        return definition;
    }

    private static Set<StructDefId> referencedStructDefIds(InlineStructDef structDef) {
        return structDef.getFieldsMap().values().stream()
                .flatMap(field -> referencedStructDefIds(field.getFieldType()).stream())
                .collect(Collectors.toSet());
    }

    private static Set<StructDefId> referencedStructDefIds(TypeDefinition type) {
        return switch (type.getDefinedTypeCase()) {
            case STRUCT_DEF_ID -> Set.of(type.getStructDefId());
            case INLINE_ARRAY_DEF ->
                referencedStructDefIds(type.getInlineArrayDef().getArrayType());
            case INLINE_STRUCT_DEF -> referencedStructDefIds(type.getInlineStructDef());
            case INLINE_MAP_DEF -> {
                Set<StructDefId> references = new java.util.HashSet<>(
                        referencedStructDefIds(type.getInlineMapDef().getKeyType()));
                references.addAll(referencedStructDefIds(type.getInlineMapDef().getValueType()));
                yield references;
            }
            case PRIMITIVE_TYPE, DEFINEDTYPE_NOT_SET -> Set.of();
        };
    }

    private static String referenceName(StructDefId id) {
        return "%s_v%d".formatted(id.getName(), id.getVersion());
    }

    private static JsonSchemaElement toJsonSchemaElement(VariableType type) {
        return switch (type) {
            case DOUBLE -> new JsonNumberSchema();
            case BOOL -> new JsonBooleanSchema();
            case INT -> new JsonIntegerSchema();
            case STR, BYTES, WF_RUN_ID, TIMESTAMP -> new JsonStringSchema();
            case JSON_OBJ ->
                JsonObjectSchema.builder()
                        .required(List.of())
                        .additionalProperties(true)
                        .build();
            case JSON_ARR -> JsonArraySchema.builder().build();
            case UNRECOGNIZED ->
                throw new IllegalArgumentException(
                        "Cannot transform unrecognized LittleHorse type");
        };
    }
}
