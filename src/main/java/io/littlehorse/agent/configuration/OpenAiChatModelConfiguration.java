package io.littlehorse.agent.configuration;

import io.littlehorse.agent.configuration.validation.NotBlankIfPresent;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Application configuration for the official OpenAI chat model. */
@ConfigGroup
public interface OpenAiChatModelConfiguration {

    @NotBlank(message = "must be configured")
    String apiKey();

    Optional<@NotBlankIfPresent String> baseUrl();

    @NotBlank(message = "must be configured")
    String model();

    /** Maximum duration to wait for a model response. */
    Optional<Duration> timeout();

    /**
     * The maximum number of times to retry. 1 means exactly one attempt, with retrying disabled.
     */
    Optional<Integer> maxRetries();

    /**
     * <a href="https://platform.openai.com/docs/api-reference">OpenAI Organization ID</a>
     */
    Optional<@NotBlankIfPresent String> organizationId();

    /**
     * <a href="https://platform.openai.com/docs/api-reference">OpenAI Project ID</a>
     */
    Optional<@NotBlankIfPresent String> projectId();

    /**
     * What sampling temperature to use, with values between 0 and 2.
     * Higher values means the model will take more risks.
     * A value of 0.9 is good for more creative applications, while 0 (argmax sampling) is good for ones with a well-defined
     * answer.
     * It is recommended to alter this or topP, but not both.
     */
    Optional<Double> temperature();

    /**
     * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens
     * with topP probability mass.
     * 0.1 means only the tokens comprising the top 10% probability mass are considered.
     * It is recommended to alter this or temperature, but not both.
     */
    Optional<Double> topP();

    /**
     * The maximum number of tokens to generate in the completion. The token count of your prompt plus max_tokens can't exceed
     * the model's context length.
     * Most models have a context length of 2048 tokens (except for the newest models, which support 4096).
     *
     * @deprecated For newer OpenAI models, use {@code maxCompletionTokens} instead
     */
    @Deprecated
    Optional<Integer> maxTokens();

    /**
     * An upper bound for the number of tokens that can be generated for a completion, including visible output tokens and
     * reasoning tokens.
     */
    Optional<Integer> maxCompletionTokens();

    /**
     * Number between -2.0 and 2.0.
     * Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to
     * talk about new topics.
     */
    Optional<Double> presencePenalty();

    /**
     * Number between -2.0 and 2.0.
     * Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's
     * likelihood to repeat the same line verbatim.
     */
    Optional<Double> frequencyPenalty();

    /**
     * Whether chat model requests should be logged
     */
    Optional<Boolean> logRequests();

    /**
     * Whether chat model responses should be logged
     */
    Optional<Boolean> logResponses();

    /**
     * The list of stop words to use.
     */
    Optional<List<String>> stop();

    /**
     * Constrains effort on reasoning for reasoning models.
     * Currently supported values are {@code minimal}, {@code low}, {@code medium}, and {@code high}.
     * Reducing reasoning effort can result in faster responses and fewer tokens used on reasoning in a response.
     * <p>
     * Note: The {@code gpt-5-pro} model defaults to (and only supports) high reasoning effort.
     */
    Optional<@NotBlankIfPresent String> reasoningEffort();

    /**
     * Specifies the processing type used for serving the request.
     * <p>
     * If set to {@code auto}, then the request will be processed with the service tier configured in the Project settings.
     * If set to {@code default}, then the request will be processed with the standard pricing and performance for the selected
     * model.
     * If set to {@code flex} or {@code priority}, then the request will be processed with the corresponding service tier.
     * When not set, the default behavior is {@code auto}.
     * <p>
     * When the service tier parameter is set, the response body will include the {@code service_tier} value based on the
     * processing mode actually used to serve the request.
     * This response value may be different from the value set in the parameter.
     */
    @ConfigDocDefault("default")
    Optional<@NotBlankIfPresent String> serviceTier();
}
