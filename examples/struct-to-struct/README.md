# Struct-to-Struct Agent Example

This Quarkus application registers a LittleHorse workflow that accepts an
`equity-comparison-request` Struct, asks the agent to research two exact ticker symbols, and saves
an `equity-comparison-report` Struct.

```text
input (equity-comparison-request) -> struct-to-struct-agent -> output (equity-comparison-report)
```

The input embeds an inline `AnalysisWindow`. The output reuses the named `InstrumentSnapshot`
StructDef for the subject and benchmark, references `WindowPerformance` from an array, embeds
inline profile and summary structs, and includes nullable fields, a string array, and a
`Map<String, String>` for source provenance. The `@LHStructField` descriptions become part of the
LittleHorse definitions; the connector uses those definitions to build the model's JSON output
schema and validate its returned Struct.

The user-message template names the `requestId`, `subjectSymbol`, and `benchmarkSymbol` fields
individually. It renders `{{struct.window}}` as a JSON object, so the sample input below produces
a message like:

```text
Research request comparison-001. Compare subject AAPL against benchmark MSFT. Parse this analysis window as JSON: {"fromDate":"2026-09-01","toDate":"2026-09-15"}
```

The agent connects to the same community-hosted Yahoo Finance MCP endpoint as the
[text-to-text example](../text-to-text/README.md). It uses `get_quote` for both symbols in one
call, `get_chart` for each symbol's daily price history, and `quote_summary` with `assetProfile`
and `financialData` for the subject company. The server also exposes `search`, which this example
**explicitly excludes** because the input already supplies exact symbols:

```properties
agent.mcp.clients.yahoo.tools.mode=exclude
agent.mcp.clients.yahoo.tools.names=search
```

The exclusion uses the server-side name `search`. The connector filters that name before adding
the client prefix, so the model can see `yahoo_get_quote`, `yahoo_get_chart`, and
`yahoo_quote_summary`, but not `yahoo_search`.

Market data and server availability depend on the external service. The endpoint is not an
official Yahoo Finance MCP service. The example requires internet access, but no Yahoo API key.

## Setup

Install Ollama if needed:

```shell
brew install ollama
```

From the repository root, start LittleHorse and make the `qwen3:4b` model available:

```shell
./local-dev/setup.sh
```

The setup starts an installed Ollama service when necessary and validates it at
`localhost:11434`. LittleHorse listens at `localhost:2023`; the dashboard is at
[http://localhost:8080](http://localhost:8080). To point `lhctl` at the local server, optionally
run `./local-dev/setup.sh --lhctl` after backing up any existing LittleHorse config.

## Run

Start Quarkus in one terminal and leave it running:

```shell
./gradlew :example-struct-to-struct:quarkusDev
```

This registers the four named StructDefs (`equity-comparison-request`,
`equity-comparison-report`, `instrument-snapshot`, and `window-performance`), the
`struct-to-struct-agent` TaskDef, and the `equity-comparison-example` WfSpec. The workflow gives
the task five minutes for local model inference and remote tool calls.

In another terminal, run a comparison. The end date is exclusive for the chart request, so this
example includes trading days from September 1 through September 14, 2026:

```shell
lhctl run equity-comparison-example input \
  '{"requestId":"comparison-001","subjectSymbol":"AAPL","benchmarkSymbol":"MSFT","window":{"fromDate":"2026-09-01","toDate":"2026-09-15"}}'
```

Use the returned WfRun id to inspect the result:

```shell
lhctl get wfRun <wf-run-id>
lhctl get variable <wf-run-id> 0 output
```

Look for quote observation times, two window-performance entries, a subject profile, and
`sourcesBySection`. Historical returns use the first and last **returned trading-day closes** in
the requested window; they do not use the latest quote as the window endpoint. If a source lacks a
value or a tool fails, the corresponding numeric field should be null and `warnings` should
explain the gap. Quotes can be delayed, so `observedAt` is more informative than the workflow's
run time.

The `search` exclusion can be checked by looking at the agent's exposed tool specifications:
`yahoo_search` must be absent while the other three Yahoo tools are present. The repository's MCP
policy test also checks that excluded tool names are filtered before prefixing.

## Cleanup

Stop Quarkus, then stop the local LittleHorse environment when no longer needed:

```shell
./local-dev/setup.sh --clean
```

This removes local LittleHorse definitions and run history. To stop the host-level Ollama service
too, use `./local-dev/setup.sh --clean --stop-ollama`.
