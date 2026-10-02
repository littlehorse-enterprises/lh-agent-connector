package io.littlehorse.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextResourceToolResultExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TextResourceToolResultExtractor extractor = new TextResourceToolResultExtractor();

    @Test
    void preservesTextAndResourcesInOrderAndKeepsTheErrorFlag() throws Exception {
        var result = extractor.extract(mapper.readTree("""
                [
                  {"type":"text","text":"Before"},
                  {"type":"resource","resource":{"uri":"repo://owner/repo/file.py","mimeType":"text/x-python","text":"raise ValueError()"}},
                  {"type":"resource","resource":{"uri":"repo://owner/repo/empty.py","text":""}},
                  {"type":"text","text":"After"}
                ]
                """), true);

        assertThat(result.resultText()).isEqualTo("""
                Before
                Resource: repo://owner/repo/file.py (text/x-python)
                raise ValueError()
                Resource: repo://owner/repo/empty.py

                After""");
        assertThat(result.isError()).isTrue();
    }

    @Test
    void preservesExistingTextOnlyResults() throws Exception {
        var result = extractor.extract(mapper.readTree("""
                [{"type":"text","text":"First"},{"type":"text","text":"Second"}]
                """), false);
        assertThat(result.resultText()).isEqualTo("First\nSecond");
        assertThat(result.isError()).isFalse();
    }

    @Test
    void rejectsBinaryResourcesInsteadOfDroppingThem() throws Exception {
        var content = mapper.readTree("""
                [{"type":"resource","resource":{"uri":"repo://binary","blob":"AA=="}}]
                """);
        assertThatThrownBy(() -> extractor.extract(content, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binary resource");
    }

    @Test
    void rejectsUnsupportedContentInsteadOfReturningPartialText() throws Exception {
        var content = mapper.readTree("""
                [{"type":"text","text":"Partial"},{"type":"image","data":"AA==","mimeType":"image/png"}]
                """);
        assertThatThrownBy(() -> extractor.extract(content, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported MCP tool content type: image");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "[{\"type\":\"resource\",\"resource\":{\"uri\":\"repo://file\"}}]",
                "[{\"type\":\"resource\",\"resource\":{\"uri\":\"repo://file\",\"text\":null}}]",
                "[{\"type\":\"text\",\"text\":123}]"
            })
    void rejectsMalformedContent(String json) throws Exception {
        var content = mapper.readTree(json);
        assertThatThrownBy(() -> extractor.extract(content, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MCP tool content");
    }
}
