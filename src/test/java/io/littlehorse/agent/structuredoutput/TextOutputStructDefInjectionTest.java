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
@TestProfile(TextOutputStructDefInjectionTest.TextOutputProfile.class)
class TextOutputStructDefInjectionTest {

    @Inject
    Instance<StructDefResolver> structDefResolver;

    @Test
    void doesNotExposeTheStructDefBeanForTextOutput() {
        assertThat(structDefResolver.isResolvable()).isFalse();
    }

    public static class TextOutputProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agent.task.output.type", "TEXT",
                    "agent.task.output.struct.name", "unused-struct");
        }
    }
}
