package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.request.json.JsonSchema;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.configuration.AgentTaskConfiguration;
import io.littlehorse.agent.configuration.OutputConfiguration;
import io.littlehorse.sdk.common.proto.InlineArrayDef;
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

class StructDefResolverTest {

    @Test
    void eagerlyCachesAPinnedRootItsReferencesAndJsonSchema() {
        StructDefId orderId = id("order", 5);
        StructDefId customerId = id("customer", 2);
        StructDefId addressId = id("address", 7);
        StructDef address = struct(addressId, Map.of("city", primitive(VariableType.STR)));
        StructDef customer = struct(customerId, Map.of("address", reference(addressId)));
        StructDef order = struct(
                orderId,
                Map.of(
                        "customer", reference(customerId),
                        "customers", array(reference(customerId)),
                        "customerById", map(reference(customerId)),
                        "shipping", inlineStruct("address", reference(addressId))));
        Map<StructDefId, StructDef> available =
                Map.of(orderId, order, customerId, customer, addressId, address);
        LittleHorseBlockingStub client = client(available, new AtomicReference<>());
        StructDefResolver resolver = new StructDefResolver(client, outputStruct("order", 5));

        resolver.initialize();

        StructDefSnapshot snapshot = resolver.resolve();
        assertThat(snapshot.root()).isSameAs(order);
        assertThat(snapshot.definitions()).containsOnlyKeys(orderId, customerId, addressId);
        JsonSchema jsonSchema = snapshot.jsonSchema();
        assertThat(resolver.resolve().jsonSchema()).isSameAs(jsonSchema);
        assertThatThrownBy(() -> snapshot.definitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        verify(client).getStructDef(orderId);
        verify(client).getStructDef(customerId);
        verify(client).getStructDef(addressId);
        verifyNoMoreInteractions(client);
    }

    @Test
    void refreshesLatestOnlyWhenTheConcreteRootVersionChanges() {
        StructDefId latestId = id("order", -1);
        StructDefId customerId = id("customer", 2);
        StructDef customer = struct(customerId, Map.of("name", primitive(VariableType.STR)));
        StructDef firstRoot = struct(id("order", 5), Map.of("customer", reference(customerId)));
        StructDef secondRoot = struct(
                id("order", 6),
                Map.of(
                        "customer", reference(customerId),
                        "status", primitive(VariableType.STR)));
        AtomicReference<StructDef> latestRoot = new AtomicReference<>(firstRoot);
        LittleHorseBlockingStub client = client(Map.of(customerId, customer), latestRoot);
        StructDefResolver resolver = new StructDefResolver(client, outputStruct("order", null));

        resolver.initialize();
        verifyNoInteractions(client);
        StructDefSnapshot firstSnapshot = resolver.resolve();
        StructDefSnapshot reusedSnapshot = resolver.resolve();
        latestRoot.set(secondRoot);
        StructDefSnapshot refreshedSnapshot = resolver.resolve();

        assertThat(reusedSnapshot).isSameAs(firstSnapshot);
        assertThat(refreshedSnapshot).isNotSameAs(firstSnapshot);
        assertThat(refreshedSnapshot.root()).isSameAs(secondRoot);
        assertThat(refreshedSnapshot.definitions())
                .containsOnlyKeys(secondRoot.getId(), customerId);
        verify(client, times(3)).getStructDef(latestId);
        verify(client, times(2)).getStructDef(customerId);
        verifyNoMoreInteractions(client);
    }

    @Test
    void rejectsAnUnpinnedReferencedStructDef() {
        StructDefId rootId = id("order", 5);
        StructDef root = struct(rootId, Map.of("customer", reference(id("customer", -1))));
        LittleHorseBlockingStub client = client(Map.of(rootId, root), new AtomicReference<>());
        StructDefResolver resolver = new StructDefResolver(client, outputStruct("order", 5));

        assertThatThrownBy(resolver::initialize)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Referenced StructDef 'customer' must be pinned to a version");
    }

    private static LittleHorseBlockingStub client(
            Map<StructDefId, StructDef> definitions, AtomicReference<StructDef> latestRoot) {
        LittleHorseBlockingStub client = mock(LittleHorseBlockingStub.class);
        when(client.getStructDef(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            StructDefId requestedId = invocation.getArgument(0);
            return requestedId.getVersion() == -1 ? latestRoot.get() : definitions.get(requestedId);
        });
        return client;
    }

    private static AgentConfiguration outputStruct(String name, Integer version) {
        AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
        AgentTaskConfiguration taskConfiguration = mock(AgentTaskConfiguration.class);
        OutputConfiguration output = mock(OutputConfiguration.class);
        OutputConfiguration.StructConfiguration struct =
                mock(OutputConfiguration.StructConfiguration.class);
        when(agentConfiguration.task()).thenReturn(taskConfiguration);
        when(taskConfiguration.output()).thenReturn(output);
        when(output.struct()).thenReturn(Optional.of(struct));
        when(struct.name()).thenReturn(name);
        when(struct.version()).thenReturn(Optional.ofNullable(version));
        return agentConfiguration;
    }

    private static StructDef struct(StructDefId id, Map<String, TypeDefinition> fields) {
        InlineStructDef.Builder definition = InlineStructDef.newBuilder();
        fields.forEach((name, type) -> definition.putFields(
                name, StructFieldDef.newBuilder().setFieldType(type).build()));
        return StructDef.newBuilder().setId(id).setStructDef(definition).build();
    }

    private static StructDefId id(String name, int version) {
        return StructDefId.newBuilder().setName(name).setVersion(version).build();
    }

    private static TypeDefinition reference(StructDefId id) {
        return TypeDefinition.newBuilder().setStructDefId(id).build();
    }

    private static TypeDefinition primitive(VariableType type) {
        return TypeDefinition.newBuilder().setPrimitiveType(type).build();
    }

    private static TypeDefinition array(TypeDefinition type) {
        return TypeDefinition.newBuilder()
                .setInlineArrayDef(InlineArrayDef.newBuilder().setArrayType(type))
                .build();
    }

    private static TypeDefinition map(TypeDefinition valueType) {
        return TypeDefinition.newBuilder()
                .setInlineMapDef(InlineMapDef.newBuilder()
                        .setKeyType(primitive(VariableType.STR))
                        .setValueType(valueType))
                .build();
    }

    private static TypeDefinition inlineStruct(String fieldName, TypeDefinition type) {
        return TypeDefinition.newBuilder()
                .setInlineStructDef(InlineStructDef.newBuilder()
                        .putFields(
                                fieldName,
                                StructFieldDef.newBuilder().setFieldType(type).build()))
                .build();
    }
}
