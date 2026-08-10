package io.littlehorse.agent.structuredoutput;

import dev.langchain4j.model.chat.request.json.JsonSchema;

import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable resolved StructDef graph and its generated JSON schema. */
public record StructDefSnapshot(
        StructDef root, Map<StructDefId, StructDef> definitions, JsonSchema jsonSchema) {

    public StructDefSnapshot {
        Objects.requireNonNull(root, "root StructDef must not be null");
        Objects.requireNonNull(definitions, "StructDef definitions must not be null");
        Objects.requireNonNull(jsonSchema, "JSON schema must not be null");
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }
}
