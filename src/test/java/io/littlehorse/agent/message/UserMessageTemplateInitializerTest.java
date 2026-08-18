package io.littlehorse.agent.message;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;

class UserMessageTemplateInitializerTest {

    @Test
    void initializesTheRendererAtStartupWhenStructInputEnablesIt() {
        Instance<UserMessageTemplateRenderer> renderer = rendererInstance(true);

        new UserMessageTemplateInitializer().initialize(null, renderer);

        verify(renderer).get();
    }

    @Test
    void ignoresTheRendererAtStartupWhenTextInputDisablesIt() {
        Instance<UserMessageTemplateRenderer> renderer = rendererInstance(false);

        new UserMessageTemplateInitializer().initialize(null, renderer);

        verify(renderer, never()).get();
    }

    @SuppressWarnings("unchecked")
    private static Instance<UserMessageTemplateRenderer> rendererInstance(boolean resolvable) {
        Instance<UserMessageTemplateRenderer> renderer = mock(Instance.class);
        when(renderer.isResolvable()).thenReturn(resolvable);
        return renderer;
    }
}
