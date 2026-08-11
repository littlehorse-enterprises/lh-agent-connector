# Text-to-Text Agent Example

This Quarkus application registers a LittleHorse workflow that accepts text in the `input`
variable, passes it to the connector's text-to-text agent task, and saves the model response in the
`output` variable.

```text
input (STR) -> text-to-text-agent -> output (STR)
```

The example uses Ollama's OpenAI-compatible API and the `qwen3:4b` model. Its complete agent
configuration is in `src/main/resources/application.properties`; no API credentials are required.
The model response timeout is four minutes, with connector retries disabled, so local inference
stays within the workflow's five-minute task timeout.

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

The application registers the `text-to-text-agent` TaskDef, starts its worker, and registers the
`text-to-text-example` WfSpec. The workflow allows five minutes for the agent task because the first
local model invocation can take longer. In another terminal, start a workflow run:

```shell
lhctl run text-to-text-example input "Explain LittleHorse in one sentence"
```

Inspect the resulting WfRun in the dashboard or with `lhctl`. A successful run reaches `COMPLETED`,
and its `output` variable contains the response produced by `qwen3:4b`. Use the WfRun id printed by
the run command:

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
