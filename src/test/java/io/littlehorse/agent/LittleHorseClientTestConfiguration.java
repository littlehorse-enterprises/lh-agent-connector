package io.littlehorse.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
class LittleHorseClientTestConfiguration {

    private final LittleHorseBlockingStub client = mock(LittleHorseBlockingStub.class);

    LittleHorseClientTestConfiguration() {
        when(client.getStructDef(any())).thenAnswer(invocation -> StructDef.newBuilder()
                .setId(invocation.getArgument(0, StructDefId.class))
                .build());
    }

    @Produces
    @Mock
    LittleHorseBlockingStub littleHorseClient() {
        return client;
    }
}
