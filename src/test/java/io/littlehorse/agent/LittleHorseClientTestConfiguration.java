package io.littlehorse.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.littlehorse.sdk.common.adapter.LHTypeAdapter;
import io.littlehorse.sdk.common.adapter.LHTypeAdapterRegistry;
import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseFutureStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.quarkus.arc.All;
import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.List;

@ApplicationScoped
class LittleHorseClientTestConfiguration {

    private final LittleHorseBlockingStub client = mock(LittleHorseBlockingStub.class);
    private final LittleHorseFutureStub futureClient = mock(LittleHorseFutureStub.class);
    private final LHConfig configuration = mock(LHConfig.class);

    LittleHorseClientTestConfiguration() {
        when(configuration.getBlockingStub()).thenReturn(client);
        when(configuration.getFutureStub()).thenReturn(futureClient);
        when(client.getStructDef(any())).thenAnswer(invocation -> StructDef.newBuilder()
                .setId(invocation.getArgument(0, StructDefId.class))
                .build());
    }

    @Produces
    @Mock
    LHConfig littleHorseConfiguration(@All List<LHTypeAdapter<?>> adapters) {
        when(configuration.getTypeAdapterRegistry())
                .thenReturn(LHTypeAdapterRegistry.from(adapters));
        return configuration;
    }

    @Produces
    @Mock
    LittleHorseBlockingStub littleHorseClient() {
        return client;
    }
}
