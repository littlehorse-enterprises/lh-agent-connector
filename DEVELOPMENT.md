# Development

The local development environment runs the agent and Ollama on the host and LittleHorse in Docker
Compose. The setup command creates or reuses the Compose environment, waits for LittleHorse to
become ready, and ensures the configured model is available in local Ollama. Start the agent
separately in Quarkus development mode or from a packaged artifact.

## Prerequisites

Install the following tools:

- Docker
- Docker Compose plugin
- Java 25
- Ollama installed with Homebrew

Install Ollama before running the local setup:

```shell
brew install ollama
```

`local-dev/setup.sh` validates the `ollama` command and the API at `localhost:11434`. If Ollama is
installed but stopped, setup starts it with `brew services start ollama` and waits up to 30 seconds
for its API. Setup never installs Ollama.

The Compose environment binds the LittleHorse API and dashboard to the loopback interface, so it
does not change Kubernetes contexts or expose the services on other network interfaces.
The LittleHorse standalone image tag comes from the root `gradle.properties` `version` property.

The agent also needs credentials for one supported chat-model provider. For OpenAI:

```shell
export AGENT_CHAT_MODEL_PROVIDER=openai
export AGENT_CHAT_MODEL_OPENAI_API_KEY=...
export AGENT_CHAT_MODEL_OPENAI_MODEL=...
```

For Anthropic:

```shell
export AGENT_CHAT_MODEL_PROVIDER=anthropic
export AGENT_CHAT_MODEL_ANTHROPIC_API_KEY=...
export AGENT_CHAT_MODEL_ANTHROPIC_MODEL=...
```

To use the local Ollama model through its OpenAI-compatible API:

```shell
export AGENT_CHAT_MODEL_PROVIDER=openai
export AGENT_CHAT_MODEL_OPENAI_API_KEY=ollama
export AGENT_CHAT_MODEL_OPENAI_MODEL=qwen3:4b
export AGENT_CHAT_MODEL_OPENAI_BASE_URL=http://localhost:11434/v1
```

Set `OLLAMA_MODEL` before running setup to pull a different model into local Ollama. Use the same
model name in `AGENT_CHAT_MODEL_OPENAI_MODEL` when starting the agent.

Do not commit credentials to an application properties file. Quarkus maps these environment
variables to the corresponding `agent.*` properties.

## Start the environment

Run the previously built JVM artifact:

```shell
./local-dev/run.sh
```

Build the JVM artifact, native executable, and local container image before running:

```shell
./local-dev/run.sh --build
```

Run the previously built native executable:

```shell
./local-dev/run.sh --native
```

The native build is a Linux executable, and the native run command is intended for Linux. The
container image is also tagged as `littlehorse/lh-agent-connector:latest` for container-based use.

Build everything and then run the native executable:

```shell
./local-dev/run.sh --build --native
```

The build can also be run independently. It creates the Quarkus JVM and native artifacts and tags
the container image as `littlehorse/lh-agent-connector:latest`; it does not add the agent to the
local Compose environment.

```shell
./local-dev/build.sh
```

For live coding, invoke Quarkus directly:

```shell
./local-dev/setup.sh
./gradlew quarkusDev
```

Run `local-dev/setup.sh` before either packaged artifacts or `quarkusDev`. Setup is idempotent:
subsequent runs reuse the `lh-agent-connector` Compose project and its LittleHorse data volume.

When startup completes, the local services are available at:

- LittleHorse API: `localhost:2023`
- LittleHorse dashboard: [http://localhost:8080](http://localhost:8080)
- Ollama API: [http://localhost:11434](http://localhost:11434)
- Ollama OpenAI-compatible API: `http://localhost:11434/v1`

The Compose-side `littlehorse` service exposes the internal listener on port `2024`. Ollama is not
deployed in Compose; host-run applications access its local API at port `11434`.

## Test the agent

The text-to-text example registers the connector's task and a `text-to-text-example` WfSpec. Start
the example and leave it running:

```shell
./gradlew :example-text-to-text:quarkusDev
```

In another terminal, start a run:

```shell
lhctl run text-to-text-example input "Explain LittleHorse in one sentence"
```

Inspect the resulting WfRun in the dashboard or with `lhctl`. The workflow reaches `COMPLETED`, and
its `output` variable contains the model response. See
[examples/text-to-text/README.md](examples/text-to-text/README.md) for the full example walkthrough.

## Manage the local environment

Provision LittleHorse and ensure the configured model is available in local Ollama, without
starting Quarkus:

```shell
./local-dev/setup.sh
```

Stop LittleHorse and delete its Compose network and data volume:

```shell
./local-dev/setup.sh --clean
```

Cleanup permanently removes local LittleHorse workflow definitions and run history. Ordinary
cleanup leaves the host-level Ollama service running because other projects may use it. Stop Ollama
explicitly with:

```shell
./local-dev/setup.sh --clean --stop-ollama
```

If port `2023` or `8080` is already in use, stop the conflicting process before running setup.

Optionally configure `lhctl` to use this local LittleHorse server:

```shell
./local-dev/setup.sh --lhctl
```

This standalone command creates `~/.config` when needed. If `littlehorse.config` already exists, it
is copied to `littlehorse.config.backup` before the command writes `localhost:2023` as the active
LittleHorse endpoint.

To run Quarkus against a different LittleHorse environment without invoking the local Compose
setup, provide the desired LittleHorse configuration:

```shell
export LHC_API_HOST=example.littlehorse.internal
export LHC_API_PORT=2023
./gradlew quarkusDev -PlocalDev.skipSetup=true
```

## Verification

Run the project checks with:

```shell
./gradlew test
./gradlew spotlessCheck
```

Validate the local shell scripts without executing them:

```shell
bash -n local-dev/build.sh local-dev/setup.sh local-dev/run.sh
LH_VERSION="$(sed -n 's/^version=//p' gradle.properties)" \
  docker compose --file local-dev/compose.yaml config --quiet
```
