package io.littlehorse.agent.structuredoutput;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.OutputConfiguration;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves and caches the output StructDef graph and its generated JSON schema. */
@ApplicationScoped
@LookupIfProperty(name = "agent.task.output.type", stringValue = "STRUCT")
public class StructDefResolver {

    private final LittleHorseBlockingStub littleHorseClient;
    private final StructDefId configuredId;
    private final boolean latest;

    private volatile StructDefSnapshot snapshot;

    @Inject
    public StructDefResolver(
            LittleHorseBlockingStub littleHorseClient, AgentConfiguration agentConfiguration) {
        this.littleHorseClient = Objects.requireNonNull(littleHorseClient);
        var configuration = agentConfiguration.task();
        OutputConfiguration.StructConfiguration structConfiguration = configuration
                .output()
                .struct()
                .orElseThrow(() ->
                        new IllegalStateException("agent.task.output.struct must be configured"));
        int version = structConfiguration.version().orElse(-1);
        this.configuredId = StructDefId.newBuilder()
                .setName(structConfiguration.name())
                .setVersion(version)
                .build();
        this.latest = version == -1;
    }

    @PostConstruct
    void initialize() {
        if (!latest) {
            StructDef root = littleHorseClient.getStructDef(configuredId);
            snapshot = createSnapshot(root);
        }
    }

    public StructDefSnapshot resolve() {
        if (!latest) {
            return Objects.requireNonNull(snapshot, "StructDef resolver was not initialized");
        }

        StructDef latestRoot = littleHorseClient.getStructDef(configuredId);
        if (snapshot != null && snapshot.root().getId().equals(latestRoot.getId())) {
            return snapshot;
        }
        return refresh(latestRoot);
    }

    private synchronized StructDefSnapshot refresh(StructDef latestRoot) {
        StructDefSnapshot current = snapshot;
        if (current != null) {
            StructDefId currentId = current.root().getId();
            StructDefId latestId = latestRoot.getId();
            if (currentId.equals(latestId) || currentId.getVersion() > latestId.getVersion()) {
                return current;
            }
        }

        StructDefSnapshot refreshed = createSnapshot(latestRoot);
        snapshot = refreshed;
        return refreshed;
    }

    private StructDefSnapshot createSnapshot(StructDef root) {
        Map<StructDefId, StructDef> definitions = new LinkedHashMap<>();
        definitions.put(root.getId(), root);
        resolveReferences(root.getStructDef(), definitions);
        return new StructDefSnapshot(
                root, definitions, JsonSchemaTransformer.fromStruct(root, definitions));
    }

    private void resolveReferences(
            InlineStructDef inlineStructDef, Map<StructDefId, StructDef> definitions) {
        inlineStructDef
                .getFieldsMap()
                .values()
                .forEach(field -> resolveReferences(field.getFieldType(), definitions));
    }

    private void resolveReferences(TypeDefinition type, Map<StructDefId, StructDef> definitions) {
        switch (type.getDefinedTypeCase()) {
            case STRUCT_DEF_ID -> {
                StructDefId id = type.getStructDefId();
                if (id.getVersion() == -1) {
                    throw new IllegalArgumentException(
                            "Referenced StructDef '%s' must be pinned to a version"
                                    .formatted(id.getName()));
                }
                if (definitions.containsKey(id)) {
                    return;
                }

                StructDef definition = littleHorseClient.getStructDef(id);
                definitions.put(id, definition);
                resolveReferences(definition.getStructDef(), definitions);
            }
            case INLINE_ARRAY_DEF ->
                resolveReferences(type.getInlineArrayDef().getArrayType(), definitions);
            case INLINE_STRUCT_DEF -> resolveReferences(type.getInlineStructDef(), definitions);
            case INLINE_MAP_DEF -> {
                resolveReferences(type.getInlineMapDef().getKeyType(), definitions);
                resolveReferences(type.getInlineMapDef().getValueType(), definitions);
            }
            case PRIMITIVE_TYPE, DEFINEDTYPE_NOT_SET -> {
                // These types cannot reference another StructDef.
            }
        }
    }
}
