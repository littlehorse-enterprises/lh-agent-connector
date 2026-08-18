package io.littlehorse.agent.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.validator.BeanValidationConfigValidatorImpl;

import java.util.HashMap;
import java.util.Map;

final class AgentConfigurationTestSupport {

    private static final Map<String, String> VALID_CONFIGURATION = Map.of(
            "agent.task.name", "test-agent",
            "agent.chat-model.provider", "anthropic",
            "agent.chat-model.anthropic.api-key", "test-api-key",
            "agent.chat-model.anthropic.model", "test-model");

    private AgentConfigurationTestSupport() {}

    static AgentConfiguration buildConfiguration(Map<String, String> properties) {
        Map<String, String> allProperties = new HashMap<>(VALID_CONFIGURATION);
        allProperties.putAll(properties);
        return buildExactConfiguration(allProperties);
    }

    static AgentConfiguration buildExactConfiguration(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withMapping(AgentConfiguration.class)
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(new MapBackedConfigSource("test", properties) {})
                .build()
                .getConfigMapping(AgentConfiguration.class);
    }
}
