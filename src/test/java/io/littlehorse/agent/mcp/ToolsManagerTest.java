package io.littlehorse.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.IllegalConfigurationException;
import dev.langchain4j.service.tool.ToolExecutionResult;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Function;

class ToolsManagerTest {

    @Test
    void loadsToolsOnceAndKeepsSpecificationsPairedWithTheirExecutors() {
        ToolSpecification lookup = tool("lookup");
        ToolSpecification create = tool("create");
        McpClient client = mcpClient(
                "remote", List.of(lookup, create), ignored -> ToolExecutionResult.builder()
                        .resultText("MCP result")
                        .build());
        ToolsManager manager = toolsManager(client);

        assertThat(manager.getToolsSpecification())
                .extracting(ToolSpecification::name)
                .containsExactly("lookup", "create");
        assertThat(manager.getToolsSpecification()).hasSize(2);
        verify(client).listTools();
        assertThat(manager.getToolExecutor("missing")).isEmpty();

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1")
                .name("lookup")
                .arguments("{}")
                .build();
        ToolExecutionResult result = manager.getToolExecutor("lookup")
                .orElseThrow()
                .executeWithContext(request, InvocationContext.builder().build());

        assertThat(result.resultText()).isEqualTo("MCP result");
        ArgumentCaptor<ToolExecutionRequest> requestCaptor =
                ArgumentCaptor.forClass(ToolExecutionRequest.class);
        verify(client).executeTool(requestCaptor.capture(), any(InvocationContext.class));
        assertThat(requestCaptor.getValue().name()).isEqualTo("lookup");
        verify(client).listTools();
    }

    @Test
    void reloadsTheCatalogOnlyAfterItIsInvalidated() {
        McpClient client = mcpClient(
                "remote",
                List.of(tool("before")),
                ignored -> ToolExecutionResult.builder().resultText("done").build());
        when(client.listTools())
                .thenReturn(List.of(tool("before")))
                .thenReturn(List.of(tool("after")));
        ToolsManager manager = toolsManager(client);

        assertThat(manager.getToolsSpecification())
                .extracting(ToolSpecification::name)
                .containsExactly("before");

        assertThat(manager.getToolsSpecification())
                .extracting(ToolSpecification::name)
                .containsExactly("before");

        manager.invalidateCache();

        assertThat(manager.getToolsSpecification())
                .extracting(ToolSpecification::name)
                .containsExactly("after");
        assertThat(manager.getToolExecutor("before")).isEmpty();
        assertThat(manager.getToolExecutor("after")).isPresent();
        verify(client, times(2)).listTools();
    }

    @Test
    void returnsAnUnmodifiableSpecificationSnapshot() {
        ToolsManager manager = toolsManager(mcpClient(
                "remote",
                List.of(tool("lookup")),
                ignored -> ToolExecutionResult.builder().resultText("done").build()));

        List<ToolSpecification> specifications = manager.getToolsSpecification();

        assertThatThrownBy(() -> specifications.add(tool("another")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateToolNamesAcrossMcpClients() {
        McpClient first =
                mcpClient("first", List.of(tool("lookup")), ignored -> ToolExecutionResult.builder()
                        .resultText("first")
                        .build());
        McpClient second = mcpClient(
                "second",
                List.of(tool("lookup")),
                ignored -> ToolExecutionResult.builder().resultText("second").build());
        ToolsManager manager = new ToolsManager(McpToolProvider.builder()
                .mcpClients(first, second)
                .failIfOneServerFails(true)
                .build());

        assertThatThrownBy(manager::getToolsSpecification)
                .isInstanceOf(IllegalConfigurationException.class)
                .hasMessage("Duplicated definition for tool: lookup");
    }

    private static ToolsManager toolsManager(McpClient client) {
        return new ToolsManager(McpToolProvider.builder()
                .mcpClients(client)
                .failIfOneServerFails(true)
                .build());
    }

    private static McpClient mcpClient(
            String key,
            List<ToolSpecification> tools,
            Function<ToolExecutionRequest, ToolExecutionResult> execution) {
        McpClient client = mock(McpClient.class);
        when(client.key()).thenReturn(key);
        when(client.listTools()).thenReturn(tools);
        when(client.executeTool(any(ToolExecutionRequest.class), any(InvocationContext.class)))
                .thenAnswer(invocation -> execution.apply(invocation.getArgument(0)));
        return client;
    }

    private static ToolSpecification tool(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description(name + " description")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }
}
