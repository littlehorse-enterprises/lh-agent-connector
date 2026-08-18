package io.littlehorse.agent.configuration;

import io.littlehorse.agent.configuration.validation.NotBlankIfPresent;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigGroup
public interface AnthropicChatModelConfiguration {

    @NotBlank(message = "must be configured")
    String apiKey();

    Optional<@NotBlankIfPresent String> baseUrl();

    @NotBlank(message = "must be configured")
    String model();

    /**
     * The Anthropic version
     */
    Optional<@NotBlankIfPresent String> version();

    /**
     * Timeout for Anthropic calls
     */
    @ConfigDocDefault("10s")
    Optional<Duration> timeout();

    /**
     * What sampling temperature to use, between 0.0 and 1.0. Higher values like 0.8 will make the output more random, while
     * lower values like 0.2 will make it more focused and deterministic.
     * <p>
     * It is generally recommended to set this or the {@code top-k} property but not both.
     */
    Optional<Double> temperature();

    /**
     * The maximum number of tokens to generate in the completion.
     * <p>
     * The token count of your prompt plus {@code max_tokens} cannot exceed the model's context length
     */
    Optional<Integer> maxTokens();

    /**
     * Double (0.0-1.0). Nucleus sampling, where the model considers the results of the tokens with top_p probability mass.
     * So 0.1 means only the tokens comprising the top 10% probability mass are considered.
     * <p>
     * It is generally recommended to set this or the {@code temperature} property but not both.
     */
    @ConfigDocDefault("1.0")
    Optional<Double> topP();

    /**
     * Reduces the probability of generating nonsense. A higher value (e.g. 100) will give more diverse answers, while a lower
     * value (e.g. 10) will be more conservative
     */
    @ConfigDocDefault("40")
    Optional<Integer> topK();

    /**
     * The maximum number of times to retry. 1 means exactly one attempt, with retrying disabled.
     */
    Optional<Integer> maxRetries();

    /**
     * The custom text sequences that will cause the model to stop generating
     */
    Optional<List<String>> stopSequences();

    /**
     * Whether chat model requests should be logged
     */
    Optional<Boolean> logRequests();

    /**
     * Whether chat model responses should be logged
     */
    Optional<Boolean> logResponses();

    /**
     * Cache system messages to reduce costs for repeated prompts.
     * Requires minimum 1024 tokens (Claude Opus/Sonnet) or 2048-4096 tokens (Haiku).
     * Supported models: Claude Opus 4.1, Sonnet 4.5, Haiku 4.5, and later models.
     */
    Optional<Boolean> cacheSystemMessages();

    /**
     * Cache tool definitions to reduce costs.
     * Requires minimum 1024 tokens (Claude Opus/Sonnet) or 2048-4096 tokens (Haiku).
     * Supported models: Claude Opus 4.1, Sonnet 4.5, Haiku 4.5, and later models.
     */
    Optional<Boolean> cacheTools();
}
