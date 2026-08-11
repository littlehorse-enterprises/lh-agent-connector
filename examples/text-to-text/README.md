# Text-to-Text Agent Example

This Quarkus application registers a LittleHorse workflow that accepts text in the `input`
variable, passes it to the connector's text-to-text agent task, and saves the model response in the
`output` variable.

```text
input (STR) -> text-to-text-agent -> output (STR)
```

The example uses Ollama's OpenAI-compatible API and the `qwen3:4b` model. It also connects the agent
to the remote Yahoo Finance MCP server using streamable HTTP. Although the server advertises tools
for quotes, search, company financials, and price history, this example deliberately exposes only
the `get_quote` tool needed to answer its primary question. The connector presents that tool to the
model as `yahoo_get_quote`.

The complete agent configuration is in `src/main/resources/application.properties`; no API
credentials are required. Its system message directs the model to use Yahoo Finance tools for
questions requiring current or historical market data instead of relying on model knowledge. If a
request needs an excluded capability, the model must explain that limitation rather than use an
unrelated tool or guess. The model response timeout is four minutes, with connector retries
disabled, so local inference stays within the workflow's five-minute task timeout.

The configured endpoint is a third-party service and is not presented as an officially supported
Yahoo Finance MCP server. Running the example requires internet access, and endpoint availability
and returned market data are controlled by that external service.

## Setup

Install Ollama before running the example:

```shell
brew install ollama
```

From the repository root, create the local kind cluster, install LittleHorse, and ensure the
`qwen3:4b` model is available in local Ollama:

```shell
./local-dev/setup.sh
```

The setup is idempotent. It starts an installed Ollama service when necessary and validates it at
`localhost:11434`, but never installs Ollama. LittleHorse is exposed at `localhost:2023`, and its
dashboard is available at [http://localhost:8080](http://localhost:8080).

## Run

Start the Quarkus application and leave it running:

```shell
./gradlew :example-text-to-text:quarkusDev
```

The application registers the `text-to-text-agent` TaskDef, starts its worker, connects to the Yahoo
Finance MCP server, and registers the `text-to-text-example` WfSpec. The workflow allows five
minutes for the agent task because local model inference and remote tool calls can take longer.

The MCP allowlist uses the server-side tool name because filtering happens before the connector
adds its client prefix:

```properties
agent.mcp.clients.yahoo.tools.mode=include
agent.mcp.clients.yahoo.tools.names=get_quote
```

### Allowed quote lookup

In another terminal, start a workflow run that can be answered with the allowed `get_quote` tool:

```shell
lhctl run text-to-text-example input \
  "What is Apple's latest available stock price and how does it compare with its previous close?"
```

The agent can call `yahoo_get_quote` and save its final natural-language comparison in `output`.

### Deliberately unavailable company lookup

Run a second workflow that requires the server's excluded `quote_summary` tool:

```shell
lhctl run text-to-text-example input \
  "Retrieve Apple's company profile and latest full-time employee count from Yahoo Finance."
```

The agent cannot call `quote_summary` because its specification and executor are excluded from the
agent's tool catalog. The workflow should still reach `COMPLETED`; its `output` should explain that
the required Yahoo Finance capability is unavailable without inventing company data.

Inspect either resulting WfRun in the dashboard or with `lhctl`. Use the WfRun id printed by the run
command:

```shell
lhctl get wfRun <wf-run-id>
lhctl get variable <wf-run-id> 0 output
```

## Cleanup

Stop Quarkus, then delete the dedicated local cluster when it is no longer needed:

```shell
./local-dev/setup.sh --clean
```

This leaves the host-level Ollama service running. To stop it explicitly during cleanup:

```shell
./local-dev/setup.sh --clean --stop-ollama
```
