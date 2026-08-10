package io.littlehorse.agent.structuredoutput;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
@TestProfile(StructDefResolverInjectionTest.OutputStructProfile.class)
class StructDefResolverInjectionTest {

    @Inject
    Instance<StructDefResolver> structDefResolver;

    @Test
    void exposesTheOutputStructDefWhenItsNameIsConfigured() {
        assertThat(structDefResolver.isResolvable()).isTrue();
        var structDef = structDefResolver.get().resolve().root();
        assertThat(structDef.getId().getName()).isEqualTo("test-agent-output");
        assertThat(structDef.getId().getVersion()).isEqualTo(4);
    }

    public static class OutputStructProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.task.output.type", "STRUCT",
                    "agent.task.output.struct.name", "test-agent-output",
                    "agent.task.output.struct.version", "4");
        }
    }
}
