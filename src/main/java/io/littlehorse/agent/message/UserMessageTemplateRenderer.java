package io.littlehorse.agent.message;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Decorator;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.HelperRegistry;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.EachHelper;
import com.github.jknack.handlebars.helper.IfHelper;
import com.github.jknack.handlebars.helper.LookupHelper;
import com.github.jknack.handlebars.helper.UnlessHelper;
import com.github.jknack.handlebars.helper.WithHelper;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.ToNumberPolicy;

import io.littlehorse.agent.configuration.AgentConfiguration;
import io.littlehorse.agent.structuredoutput.JsonTransformer;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Renders the statically configured user message for Struct-input agent tasks. */
@ApplicationScoped
@LookupIfProperty(name = "agent.task.input.type", stringValue = "STRUCT")
public class UserMessageTemplateRenderer {

    private static final Pattern PARTIAL_TAG = Pattern.compile("\\{\\{~?\\s*(?:>|#\\s*[>*])");

    static final Gson TEMPLATE_GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private final Template template;

    public UserMessageTemplateRenderer(AgentConfiguration configuration) {
        String templateText = configuration
                .userMessageTemplate()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "agent.user-message-template must be configured when agent.task.input.type=STRUCT"));

        if (PARTIAL_TAG.matcher(templateText).find()) {
            throw new IllegalArgumentException(
                    "Invalid agent.user-message-template: Handlebars partials are disabled");
        }

        Handlebars handlebars = new Handlebars(new DenyAllTemplateLoader())
                .with(new RestrictedHelperRegistry())
                .with(EscapingStrategy.NOOP)
                .with((value, next) ->
                        isJsonContainer(value) ? TEMPLATE_GSON.toJson(value) : next.format(value));
        try {
            template = handlebars.compileInline(templateText);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid agent.user-message-template: " + exception.getMessage(), exception);
        }
    }

    public String render(InlineStruct input) {
        JsonElement json = JsonTransformer.fromStruct(input);
        Object templateValue = TEMPLATE_GSON.fromJson(json, Object.class);
        Context context = Context.newBuilder(Map.of("struct", templateValue))
                .resolver(MapValueResolver.INSTANCE)
                .build();
        try {
            return template.apply(context);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Could not render agent.user-message-template: " + exception.getMessage(),
                    exception);
        } finally {
            context.destroy();
        }
    }

    private static boolean isJsonContainer(Object value) {
        return value instanceof Map<?, ?> || value instanceof List<?>;
    }

    private static final class RestrictedHelperRegistry implements HelperRegistry {

        private final Map<String, Helper<?>> helpers = Map.of(
                "if", IfHelper.INSTANCE,
                "unless", UnlessHelper.INSTANCE,
                "each", EachHelper.INSTANCE,
                "with", WithHelper.INSTANCE,
                "lookup", LookupHelper.INSTANCE);

        @SuppressWarnings("unchecked")
        @Override
        public <C> Helper<C> helper(String name) {
            return (Helper<C>) helpers.get(name);
        }

        @Override
        public Set<Map.Entry<String, Helper<?>>> helpers() {
            return helpers.entrySet();
        }

        @Override
        public Decorator decorator(String name) {
            return null;
        }

        @Override
        public <H> HelperRegistry registerHelper(String name, Helper<H> helper) {
            throw unsupported();
        }

        @Override
        public <H> HelperRegistry registerHelperMissing(Helper<H> helper) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(Object helperSource) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(Class<?> helperSource) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(URI location) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(File file) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(String filename, Reader source) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(String filename, InputStream source) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerHelpers(String filename, String source) {
            throw unsupported();
        }

        @Override
        public HelperRegistry registerDecorator(String name, Decorator decorator) {
            throw unsupported();
        }

        @Override
        public HelperRegistry setCharset(Charset charset) {
            return this;
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("External Handlebars extensions are disabled");
        }
    }

    private static final class DenyAllTemplateLoader implements TemplateLoader {

        private Charset charset = StandardCharsets.UTF_8;

        @Override
        public TemplateSource sourceAt(String location) throws IOException {
            throw new IOException("External Handlebars templates and partials are disabled");
        }

        @Override
        public String resolve(String location) {
            return location;
        }

        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public String getSuffix() {
            return "";
        }

        @Override
        public void setPrefix(String prefix) {
            throw new UnsupportedOperationException("External Handlebars templates are disabled");
        }

        @Override
        public void setSuffix(String suffix) {
            throw new UnsupportedOperationException("External Handlebars templates are disabled");
        }

        @Override
        public void setCharset(Charset charset) {
            this.charset = Objects.requireNonNull(charset);
        }

        @Override
        public Charset getCharset() {
            return charset;
        }
    }
}
