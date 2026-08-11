# Development

The local development environment runs the agent on the host and LittleHorse and Ollama in a
dedicated kind cluster. Both supported launch commands create or reuse that cluster, wait for its
services to become ready, and then start the agent in Quarkus development mode. The setup installs
`qwen3:4b` in Ollama and retains it in a persistent volume between setup runs.

## Prerequisites

Install the following tools:

- Docker
- kind
- kubectl
- kubectx
- Java 25

The setup exports the kind context to the default kubeconfig, selects `kind-lh-agent-connector` with
`kubectx`, and sets its default namespace to `littlehorse`. Use `kubectx -` after setup if you want
to return to the previously selected Kubernetes context.

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

Set `OLLAMA_MODEL` before running setup to install a different model. Use the same model name in
`AGENT_CHAT_MODEL_OPENAI_MODEL` when starting the agent.

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

The native build is a Linux executable. On Linux, the script runs `build/quarkus-run` directly. On
macOS, it runs the native executable from `littlehorse/lh-agent-connector:latest` with Docker host
networking so it can reach the kind listener at `localhost:2023`.

Build everything and then run the native executable:

```shell
./local-dev/run.sh --build --native
```

The build can also be run independently. It creates the Quarkus JVM and native artifacts and tags
the container image as `littlehorse/lh-agent-connector:latest`; it does not install or roll out the
agent inside Kubernetes.

```shell
./local-dev/build.sh
```

For live coding, invoke Quarkus directly:

```shell
./gradlew quarkusDev
```

The packaged-artifact wrapper invokes `local-dev/setup.sh`, while `quarkusDev` runs the
`localDevSetup` Gradle task first. Both paths are idempotent: subsequent runs reuse the
`lh-agent-connector` kind cluster and reapply the LittleHorse manifests.

When startup completes, the local services are available at:

- LittleHorse API: `localhost:2023`
- LittleHorse dashboard: [http://localhost:8080](http://localhost:8080)
- Ollama API: [http://localhost:11434](http://localhost:11434)
- Ollama OpenAI-compatible API: `http://localhost:11434/v1`

The cluster-side `littlehorse` service exposes the internal listener on port `2024`, and the
cluster-side `ollama` service exposes its API on port `11434` for workloads running inside
Kubernetes.

## Test the agent

Quarkus development mode registers an `agent-example` WfSpec when the agent uses its default text
input and text output. Start a run with:

```shell
lhctl run agent-example input "Explain LittleHorse in one sentence"
```

Inspect the resulting WfRun in the dashboard or with `lhctl`. The workflow output variable contains
the model response.

## Manage the cluster

Provision LittleHorse and Ollama without starting Quarkus:

```shell
./local-dev/setup.sh
```

Delete the dedicated cluster:

```shell
./local-dev/setup.sh --clean
```

If port `2023` or `8080` is already in use, stop the conflicting process and recreate the cluster.
The kind host-port mappings are fixed when the cluster is created, so changes to
`local-dev/kind.yaml` also require deleting and recreating the cluster.

To run Quarkus against a different LittleHorse environment without invoking kind, skip the setup
task and provide the desired LittleHorse configuration:

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
```
