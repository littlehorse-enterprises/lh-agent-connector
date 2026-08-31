# LittleHorse LangChain4j agent

This project is a small Quarkus application built with the LittleHorse Quarkus extension. It
registers and runs one configurable agent task with text or Struct input and output.

The agent uses LangChain4j's low-level `ChatModel`, `ChatRequest`, and `ChatResponse` APIs with
Anthropic or the official OpenAI integration. It does not use LangChain4j AI Services.

It can also expose tools from any number of Model Context Protocol (MCP) servers. The agent uses
LangChain4j's low-level MCP client and tool-provider APIs: mapped MCP tool specifications are added
directly to each `ChatRequest`, and returned tool calls are routed to the matching MCP client.

## Quarkus and LittleHorse integration

Each typed task adapter is annotated with `@LHTask`. Runtime property conditions make exactly one
task adapter resolvable. The LittleHorse Quarkus extension discovers the annotations and manages
task registration, worker startup, and shutdown.

All classes annotated with `@LHStructDef` are discovered and registered automatically. The
`FinishReason` type adapter is a CDI `@Singleton`, so the extension includes it in the injected
LittleHorse configuration. Quarkus injects the enabled Anthropic or OpenAI `ChatModel` into
`AgentExecutor`.

The task checkpoints user messages, AI messages, and tool execution results as their corresponding
LittleHorse structs. On retry, `CheckpointMessages` restores completed messages from checkpoints
and rebuilds the conversation without repeating completed model or tool calls.

Exactly one agent task is registered and started. Its typed implementation is inferred from
`agent.task.input.type` and `agent.task.output.type`; both support `TEXT` and `STRUCT`. Struct input is
rendered through the statically configured user-message template before it is sent to the model.
Struct output constrains the model response with the configured output StructDef and converts the
returned JSON into an `InlineStruct`.

Set the shared task name, types, and any required StructDef names using any Quarkus configuration
source:

```properties
agent.task.name=agent
agent.task.input.type=STRUCT
agent.task.input.struct.name=agent-input
agent.user-message-template=Summarize {{struct.customer}} using these details: {{struct.details}}
agent.task.output.type=STRUCT
agent.task.output.struct.name=agent-output
agent.task.output.struct.version=2
```

The configured StructDefs must already exist in the target LittleHorse cluster; this application
does not register those external StructDefs. The input and output types default to `TEXT`. When
input is set to `STRUCT`, `agent.user-message-template` is required. When output is set to `STRUCT`,
`agent.task.output.struct.name` is required and its StructDef is fetched and exposed as a
conditional application-scoped `OutputStructDefCache` CDI bean. The cache contains the root
StructDef, all transitively referenced version-pinned StructDefs, and the generated JSON schema.
The output version is optional; when `agent.task.output.struct.version` is omitted, the latest root
version (`-1`) is checked on each schema request and the snapshot is refreshed only when its
concrete version changes.

## Configuration

All repository-owned `agent.*` properties are mapped through the root `AgentConfiguration`
interface. Its `task`, `chat-model`, and `mcp` groups retain the property names documented below.

Select one provider and configure its API key and model beneath the same `agent.chat-model`
configuration tree. For Anthropic:

```shell
export AGENT_CHAT_MODEL_PROVIDER=anthropic
export AGENT_CHAT_MODEL_ANTHROPIC_API_KEY=...
export AGENT_CHAT_MODEL_ANTHROPIC_MODEL=...
```

For OpenAI:

```shell
export AGENT_CHAT_MODEL_PROVIDER=openai
export AGENT_CHAT_MODEL_OPENAI_API_KEY=...
export AGENT_CHAT_MODEL_OPENAI_MODEL=...
```

Custom endpoints can optionally be configured with `AGENT_CHAT_MODEL_ANTHROPIC_BASE_URL` or
`AGENT_CHAT_MODEL_OPENAI_BASE_URL`. Configure the OpenAI response timeout with
`AGENT_CHAT_MODEL_OPENAI_TIMEOUT` when a local or remote model can take longer than the default.
Only the selected provider's configuration is required. The provider is read when the application
starts, so switching it requires a restart but not a rebuild.

An optional system message can be supplied through Quarkus configuration. When present, it is
checkpointed and sent as the first message in every new agent conversation:

```shell
export AGENT_SYSTEM_MESSAGE="You are a concise and helpful assistant."
```

For `STRUCT` input, the required `agent.user-message-template` is compiled once when the application
starts. Its `struct` root exposes fields using Handlebars paths:

```properties
agent.user-message-template=Customer {{struct.name}} lives at {{struct.address}}.
```

Scalar paths render as text. Rendering an object or array path, including `{{struct}}`, produces
compact JSON. Direct null values render as empty text, while null values contained in rendered JSON
objects or arrays remain JSON `null`. Output is not HTML-escaped.

The template supports only the built-in `if`, `unless`, `each`, `with`, and `lookup` helpers.
External helpers, `log`, partials (including dynamic, inline, and partial-block forms), decorators,
and filesystem or classpath template loading are disabled. Changing the template requires an
application restart.

The LittleHorse Quarkus extension reads standard LittleHorse client and worker configuration from
Quarkus configuration. For example:

```shell
export LHC_API_HOST=localhost
export LHC_API_PORT=2023
```

### MCP servers

Configure each MCP server under a unique `agent.mcp.clients` key. Streamable HTTP, legacy HTTP/SSE,
and WebSocket transports are supported. This example configures two independent clients:

```properties
agent.mcp.clients."github".transport=streamable-http
agent.mcp.clients."github".url=https://mcp.example.com/github/mcp
agent.mcp.clients."github".auth.type=oauth2
agent.mcp.clients."github".auth.oidc-client=github-oauth
agent.mcp.clients."github".headers[0].name=X-Client-Name
agent.mcp.clients."github".headers[0].value=lh-agent
agent.mcp.clients."github".timeout=PT30S
agent.mcp.clients."github".initialization-timeout=PT30S
agent.mcp.clients."github".tool-execution-timeout=PT60S
agent.mcp.clients."notifications".transport=websocket
agent.mcp.clients."notifications".url=wss://mcp.example.com/notifications/ws
agent.mcp.clients."notifications".auth.type=bearer
agent.mcp.clients."notifications".auth.token=${NOTIFICATIONS_MCP_TOKEN}
agent.mcp.clients."public".url=https://mcp.example.com/public/mcp
agent.mcp.clients."public".auth.type=none
```

Custom headers use indexed name/value entries so punctuation in a header name is preserved when
configuration comes from environment variables. For example:

```shell
export AGENT_MCP_CLIENTS_GITHUB_HEADERS_0__NAME=X-API-Key
export AGENT_MCP_CLIENTS_GITHUB_HEADERS_0__VALUE=secret
```

The previous `headers.<header-name>=<value>` syntax is not supported. Exact duplicate names are
processed in index order and the last value wins. For environment-only configuration, use MCP
client names containing only lowercase ASCII letters and digits. Other client names remain
supported when their exact names are declared in a properties file. SSE request logging includes
headers, so enabling `log-requests` can expose configured header values and authentication
credentials.

MCP configuration is validated during Quarkus startup. Every configured client requires a nonblank,
supported URL. Authentication type is `none` by default; `bearer` requires only `auth.token`, and
`oauth2` requires only `auth.oidc-client`. An explicit `Authorization` header cannot be combined
with `bearer` or `oauth2`. Durations must be valid, and configured header, filter, and mapping values
cannot be blank. Supported transport values are `streamable-http`, `sse`, and `websocket`.

For a direct bearer token, reference an environment variable rather than storing the token in the
properties file:

```properties
agent.mcp.clients."github".auth.type=bearer
agent.mcp.clients."github".auth.token=${GITHUB_MCP_TOKEN}
```

```shell
export GITHUB_MCP_TOKEN=...
```

OAuth is supplied by a named Quarkus OIDC client. All Quarkus OIDC client grant settings remain
available as application properties; the MCP transport obtains and refreshes its bearer token:

```properties
quarkus.oidc-client."github-oauth".auth-server-url=https://identity.example.com/realms/tools
quarkus.oidc-client."github-oauth".client-id=lh-agent
quarkus.oidc-client."github-oauth".credentials.secret=${MCP_GITHUB_CLIENT_SECRET}
quarkus.oidc-client."github-oauth".grant.type=client
quarkus.oidc-client."github-oauth".scopes=mcp:tools
```

Filters use the original server-side tool names and run before mappings. The default `all` mode
exposes every tool. Use `include` to expose only the named tools, or `exclude` to expose every tool
except the named tools:

```properties
agent.mcp.clients."github".tools.mode=include
agent.mcp.clients."github".tools.names=get_issue,list_issues
```

Each client uses `<client-name>_` as its default tool-name prefix. Configure a different prefix or
override individual exposed names and descriptions while preserving the MCP server's parameter
schema:

```properties
agent.mcp.clients."github".tools.name-prefix=github_
agent.mcp.clients."github".tools.specification-mapping.get_issue.name=find_issue
agent.mcp.clients."github".tools.specification-mapping.list_issues.description=Lists issues visible to the authenticated user.
```

Set `enabled=false` on an individual client to keep its configuration without connecting it.
Request/response logging, redirect following, and streamable-HTTP subsidiary channels are also
configurable through the corresponding kebab-case client properties.

The application requires a complete tool catalog from every enabled MCP client; partial catalogs
are not supported. MCP clients cache their tool lists, and the application-scoped `ToolsManager`
loads the combined, filtered, and mapped catalog lazily when an agent first needs it. When an MCP
server sends `notifications/tools/list_changed`, the registered client listener invalidates that
catalog. The next checkpointed chat or tool execution reloads the complete catalog before
continuing. If any enabled server is unavailable during a load or reload, that agent invocation
fails rather than proceeding with an incomplete set of tools.

## Run

Export the selected model's credentials, then run the application in Quarkus development mode:

```shell
./gradlew quarkusDev
```

For local development, install Ollama separately, then provision LittleHorse with
`./local-dev/setup.sh` before starting Quarkus. The setup script starts an installed Ollama service
when needed, pulls the configured model, and runs LittleHorse through Docker Compose, but never
installs Ollama. Optionally run `./local-dev/setup.sh --lhctl` to back up and configure
`~/.config/littlehorse.config` for the local server. Packaged JVM and native artifacts can be built
and run through the scripts in `local-dev`. See
[DEVELOPMENT.md](DEVELOPMENT.md) for prerequisites, lifecycle commands, build options, and
smoke-test instructions.

## Examples

Standalone Quarkus applications for the four agent input/output combinations live under
[`examples/`](examples/README.md). The first available example runs a text-to-text workflow against
the local Ollama `qwen3:4b` model:

```shell
./local-dev/setup.sh
./gradlew :example-text-to-text:quarkusDev
```

See the [text-to-text example](examples/text-to-text/README.md) for its workflow invocation and
output-inspection instructions.

## Test and build

```shell
./gradlew test
./gradlew quarkusBuild
```
