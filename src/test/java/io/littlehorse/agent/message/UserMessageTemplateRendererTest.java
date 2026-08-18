package io.littlehorse.agent.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonParser;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class UserMessageTemplateRendererTest {

    @Test
    void rendersScalarObjectAndRootStructValues() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("name", field(VariableValue.newBuilder().setStr("Ada")))
                .putFields(
                        "address",
                        field(VariableValue.newBuilder()
                                .setJsonObj("{\"city\":\"Quito\",\"zip\":170101}")))
                .build();

        String[] rendered =
                render("{{struct.name}}|{{struct.address}}|{{struct}}", input).split("\\|", 3);

        assertThat(rendered[0]).isEqualTo("Ada");
        assertThat(JsonParser.parseString(rendered[1]))
                .isEqualTo(JsonParser.parseString("{\"city\":\"Quito\",\"zip\":170101}"));
        assertThat(JsonParser.parseString(rendered[2]))
                .isEqualTo(JsonParser.parseString(
                        "{\"address\":{\"city\":\"Quito\",\"zip\":170101},\"name\":\"Ada\"}"));
    }

    @Test
    void supportsOnlyTheAllowlistedBuiltInHelpers() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("active", field(VariableValue.newBuilder().setBool(true)))
                .putFields("name", field(VariableValue.newBuilder().setStr("Ada")))
                .putFields(
                        "address",
                        field(VariableValue.newBuilder().setJsonObj("{\"city\":\"Quito\"}")))
                .putFields(
                        "tags",
                        field(VariableValue.newBuilder().setJsonArr("[\"workflow\",\"agent\"]")))
                .build();

        String template = "{{#if struct.active}}"
                + "{{#each struct.tags}}{{@index}}={{this}};{{/each}}"
                + "{{#with struct.address}}{{city}}{{/with}}|"
                + "{{lookup struct \"name\"}}"
                + "{{else}}inactive{{/if}}"
                + "{{#unless struct.missing}}|present{{/unless}}";

        assertThat(render(template, input)).isEqualTo("0=workflow;1=agent;Quito|Ada|present");
    }

    @Test
    void preservesLittleHorseIntegerPrecision() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("number", field(VariableValue.newBuilder().setInt(Long.MAX_VALUE)))
                .build();

        assertThat(render("{{struct.number}}|{{struct}}", input))
                .isEqualTo("9223372036854775807|{\"number\":9223372036854775807}");
    }

    @Test
    void doesNotHtmlEscapeRenderedValues() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields(
                        "instruction",
                        field(VariableValue.newBuilder().setStr("Use <xml> & \"quotes\"")))
                .build();

        assertThat(render("{{struct.instruction}}", input)).isEqualTo("Use <xml> & \"quotes\"");
    }

    @Test
    void rendersDirectNullAsEmptyAndPreservesNullInsideJsonContainers() {
        InlineStruct input = InlineStruct.newBuilder()
                .putFields("empty", StructField.getDefaultInstance())
                .putFields(
                        "object", field(VariableValue.newBuilder().setJsonObj("{\"value\":null}")))
                .build();

        assertThat(render("[{{struct.empty}}]|{{struct.object}}|{{struct}}", input))
                .isEqualTo("[]|{\"value\":null}|{\"empty\":null,\"object\":{\"value\":null}}");
    }

    @Test
    void rejectsMalformedTemplatesAtConstructionTime() {
        assertThatThrownBy(() -> renderer("{{#if struct.active}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid agent.user-message-template");
    }

    @Test
    void requiresATemplateAtConstructionTime() {
        assertThatThrownBy(() -> new UserMessageTemplateRenderer(configuration(Optional.empty())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void rejectsLogAndUnregisteredHelpersAtConstructionTime() {
        assertTemplateIsRejected("{{log struct.name}}");
        assertTemplateIsRejected("{{uppercase struct.name}}");
    }

    @Test
    void rejectsStaticDynamicInlineAndBlockPartialsAtConstructionTime() {
        assertTemplateIsRejected("{{> secret}}");
        assertTemplateIsRejected("{{> (lookup struct \"partial\")}}");
        assertTemplateIsRejected("{{#*inline \"secret\"}}hidden{{/inline}}{{> secret}}");
        assertTemplateIsRejected("{{#> secret}}fallback{{/secret}}");
    }

    private static void assertTemplateIsRejected(String template) {
        assertThatThrownBy(() -> renderer(template)).isInstanceOf(IllegalArgumentException.class);
    }

    private static String render(String template, InlineStruct input) {
        return renderer(template).render(input);
    }

    private static UserMessageTemplateRenderer renderer(String template) {
        return new UserMessageTemplateRenderer(configuration(Optional.of(template)));
    }

    private static AgentConfiguration configuration(Optional<String> template) {
        AgentConfiguration configuration = mock(AgentConfiguration.class);
        when(configuration.userMessageTemplate()).thenReturn(template);
        return configuration;
    }

    private static StructField field(VariableValue.Builder value) {
        return StructField.newBuilder().setValue(value).build();
    }
}
