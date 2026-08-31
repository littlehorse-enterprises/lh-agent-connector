# Configurations

## Table of Contents

- [Agent](#agent)
- [Task](#task)
- [Chat Model](#chat-model)
  - [Provider Selection](#provider-selection)
  - [Anthropic](#anthropic)
  - [OpenAI](#openai)
- [Model Context Protocol](#model-context-protocol)
  - [Client](#client)
  - [Authentication](#authentication)
  - [Tools and Specification Mappings](#tools-and-specification-mappings)

All repository-owned configuration uses the `agent` prefix and can be supplied through any
[Quarkus configuration source](https://quarkus.io/guides/config-reference). The tables document the
effective default when a property is omitted.
The Required column uses `Yes`, `No`, or the condition that makes a property required. Unless stated
otherwise, validations are enforced when the application starts.

## Agent

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.max-tool-rounds` | Maximum number of model-response iterations in an agent execution, including the final response iteration. | Integer | No | Must be greater than zero. | `10` |
| `agent.system-message` | System instruction sent as the first message in every new agent conversation. | String | No | Must not be blank when set. | |
| `agent.user-message-template` | Handlebars template used to render structured task input into a user message. The input struct is available under the `struct` root. | String | When `agent.task.input.type` is `STRUCT` | Must not be blank when set. | |

## Task

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.task.name` | Name of the LittleHorse task registered and executed by the agent. | String | Yes | Must not be blank. | |
| `agent.task.input.type` | Representation accepted by the agent task. | Enum: `TEXT`, `STRUCT` | No | Must be a supported enum value. | `TEXT` |
| `agent.task.input.struct.name` | Name of the LittleHorse StructDef accepted as structured task input. | String | When `agent.task.input.type` is `STRUCT` | Must not be blank when set. | |
| `agent.task.output.type` | Representation returned by the agent task. | Enum: `TEXT`, `STRUCT` | No | Must be a supported enum value. | `TEXT` |
| `agent.task.output.struct.name` | Name of the LittleHorse StructDef used to constrain and convert structured model output. | String | When `agent.task.output.type` is `STRUCT` | Must not be blank when set. | |
| `agent.task.output.struct.version` | Version of the output StructDef. `-1` resolves the latest version. | Integer | No | Must be greater than or equal to `-1`. | `-1` (latest) |

The configured input and output StructDefs must already exist in the target LittleHorse cluster.
When an output version is omitted, the connector treats it as `-1` and refreshes its cached schema
when the latest concrete version changes.

## Chat Model

### Provider Selection

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.chat-model.provider` | Chat-model provider used by the agent. | Enum: `ANTHROPIC`, `OPENAI` | Yes | Must be a supported enum value, and the corresponding provider configuration group must be present. | |

Only the selected provider is used to create the chat model. Provider changes take effect after an
application restart.

### Anthropic

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.chat-model.anthropic.api-key` | API key used to authenticate Anthropic requests. | String | When Anthropic is selected | Must not be blank. | |
| `agent.chat-model.anthropic.base-url` | Base URL of the Anthropic API or an Anthropic-compatible endpoint. | String | No | Must not be blank when set. | `https://api.anthropic.com/v1/` |
| `agent.chat-model.anthropic.model` | Anthropic model identifier used for chat requests. | String | When Anthropic is selected | Must not be blank. | |
| `agent.chat-model.anthropic.version` | Value sent in the `anthropic-version` request header. | String | No | Must not be blank when set. | `2023-06-01` |
| `agent.chat-model.anthropic.timeout` | Maximum duration to wait for an Anthropic call. | Duration | No | | `10s` |
| `agent.chat-model.anthropic.temperature` | Sampling temperature from `0.0` to `1.0`. Higher values produce more varied responses; lower values are more focused and deterministic. | Double | No | | |
| `agent.chat-model.anthropic.max-tokens` | Maximum number of tokens generated in a completion. | Integer | No | | `1024` |
| `agent.chat-model.anthropic.top-p` | Nucleus-sampling probability mass from `0.0` to `1.0`. Anthropic recommends configuring either this property or `temperature`, not both. | Double | No | | `1.0` |
| `agent.chat-model.anthropic.top-k` | Number of the most likely tokens considered during sampling. Higher values increase diversity. | Integer | No | | `40` |
| `agent.chat-model.anthropic.max-retries` | Maximum number of attempts. A value of `1` performs exactly one attempt and disables retries. | Integer | No | | `2` |
| `agent.chat-model.anthropic.stop-sequences` | Custom text sequences that stop response generation. | List of strings | No | | |
| `agent.chat-model.anthropic.log-requests` | Logs requests sent to the Anthropic model. | Boolean | No | | `false` |
| `agent.chat-model.anthropic.log-responses` | Logs responses received from the Anthropic model. | Boolean | No | | `false` |
| `agent.chat-model.anthropic.cache-system-messages` | Caches eligible system messages to reduce costs for repeated prompts. Anthropic model and minimum-token requirements apply. | Boolean | No | | `false` |
| `agent.chat-model.anthropic.cache-tools` | Caches eligible tool definitions to reduce costs. Anthropic model and minimum-token requirements apply. | Boolean | No | | `false` |

### OpenAI

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.chat-model.openai.api-key` | API key used to authenticate OpenAI requests. | String | When OpenAI is selected | Must not be blank. | |
| `agent.chat-model.openai.base-url` | Base URL of the OpenAI API or an OpenAI-compatible endpoint. | String | No | Must not be blank when set. | `https://api.openai.com/v1` |
| `agent.chat-model.openai.model` | OpenAI model identifier used for chat requests. | String | When OpenAI is selected | Must not be blank. | |
| `agent.chat-model.openai.timeout` | Maximum duration to wait for a model response. When configured, the value is used for both connection and read timeouts. | Duration | No | | SDK defaults: `15s` connection, `60s` read |
| `agent.chat-model.openai.max-retries` | Maximum number of attempts. A value of `1` performs exactly one attempt and disables retries. | Integer | No | | `2` |
| `agent.chat-model.openai.organization-id` | OpenAI organization associated with requests. | String | No | Must not be blank when set. | |
| `agent.chat-model.openai.project-id` | OpenAI project associated with requests. | String | No | Must not be blank when set. | |
| `agent.chat-model.openai.temperature` | Sampling temperature from `0` to `2`. Higher values produce more varied responses; lower values are more deterministic. | Double | No | | |
| `agent.chat-model.openai.top-p` | Nucleus-sampling probability mass. OpenAI recommends configuring either this property or `temperature`, not both. | Double | No | | |
| `agent.chat-model.openai.max-tokens` | Deprecated maximum number of tokens generated in a completion. Use `agent.chat-model.openai.max-completion-tokens` for newer models. | Integer | No | | |
| `agent.chat-model.openai.max-completion-tokens` | Upper bound for generated visible output and reasoning tokens. | Integer | No | | |
| `agent.chat-model.openai.presence-penalty` | Value from `-2.0` to `2.0` that penalizes tokens that have already appeared, encouraging discussion of new topics. | Double | No | | |
| `agent.chat-model.openai.frequency-penalty` | Value from `-2.0` to `2.0` that penalizes tokens according to their frequency in the response so far. | Double | No | | |
| `agent.chat-model.openai.log-requests` | Logs requests sent to the OpenAI model. | Boolean | No | | `false` |
| `agent.chat-model.openai.log-responses` | Logs responses received from the OpenAI model. | Boolean | No | | `false` |
| `agent.chat-model.openai.stop` | Text sequences that stop response generation. | List of strings | No | | |
| `agent.chat-model.openai.reasoning-effort` | Effort used by reasoning models. Supported values are `minimal`, `low`, `medium`, and `high`; model-specific restrictions may apply. | String | No | Must not be blank when set. | |
| `agent.chat-model.openai.service-tier` | Processing tier used to serve requests. Supported OpenAI values include `auto`, `default`, `flex`, and `priority`. | String | No | Must not be blank when set. | |

## Model Context Protocol

MCP clients are configured under `agent.mcp.clients.<client-name>`. Replace `<client-name>` with a
unique, nonblank map key. Quote keys containing characters that have special meaning in Quarkus
configuration, for example `agent.mcp.clients."github.com".url`.
For environment-only configuration, use lowercase client names containing only ASCII letters and
digits so SmallRye Config can reconstruct the dynamic map key without punctuation or case
ambiguity. Other client names remain supported when their exact dotted names are declared in a
properties file.

### Client

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.mcp.clients.<client-name>.enabled` | Enables creation and connection of this MCP client. | Boolean | No | The client name must not be blank. The value must be a valid boolean. | `true` |
| `agent.mcp.clients.<client-name>.transport` | Transport used to connect to the MCP server. | Enum: `streamable-http`, `sse`, `websocket` | No | Must be a supported enum value. | `streamable-http` |
| `agent.mcp.clients.<client-name>.url` | HTTP(S) or WebSocket endpoint of the MCP server. | String | For each configured client | Must not be blank. Must start with `http://`, `https://`, `ws://`, or `wss://` and contain no whitespace. | |
| `agent.mcp.clients.<client-name>.headers[<index>].name` | Name of an indexed static header added to requests. | String | For each configured header | Must not be blank. `Authorization` cannot be set when authentication type is `bearer` or `oauth2`. | |
| `agent.mcp.clients.<client-name>.headers[<index>].value` | Value of an indexed static header added to requests. | String | For each configured header | Must not be blank. | |
| `agent.mcp.clients.<client-name>.timeout` | Timeout used by the selected MCP transport. | Duration | No | Must be a valid, nonnegative duration. | `30s` |
| `agent.mcp.clients.<client-name>.initialization-timeout` | Maximum duration allowed for MCP client initialization. | Duration | No | Must be a valid, nonnegative duration. | `30s` |
| `agent.mcp.clients.<client-name>.tool-execution-timeout` | Maximum duration allowed for one MCP tool execution. | Duration | No | Must be a valid, nonnegative duration. | `60s` |
| `agent.mcp.clients.<client-name>.log-requests` | Logs requests sent through the MCP transport. | Boolean | No | Must be a valid boolean. | `false` |
| `agent.mcp.clients.<client-name>.log-responses` | Logs responses received through the MCP transport. | Boolean | No | Must be a valid boolean. | `false` |
| `agent.mcp.clients.<client-name>.follow-redirects` | Follows HTTP redirects for the streamable HTTP transport. | Boolean | No | Must be a valid boolean. Used only by `streamable-http`. | `true` |
| `agent.mcp.clients.<client-name>.subsidiary-channel` | Enables the streamable HTTP subsidiary channel. | Boolean | No | Must be a valid boolean. Used only by `streamable-http`. | `false` |

When no clients are configured, the MCP client map is empty. A client configured with
`enabled=false` remains in the configuration but is not connected or exposed to the agent.

Headers use indexed name/value entries so names containing punctuation can be supplied as values,
including through environment variables:

```properties
agent.mcp.clients.github.headers[0].name=X-API-Key
agent.mcp.clients.github.headers[0].value=${GITHUB_MCP_API_KEY}
```

```shell
export AGENT_MCP_CLIENTS_GITHUB_HEADERS_0__NAME=X-API-Key
export AGENT_MCP_CLIENTS_GITHUB_HEADERS_0__VALUE=secret
```

The previous `headers.<header-name>=<value>` map syntax is not supported. Header entries are
processed in index order; when the same case-sensitive name is repeated, the last entry wins.
HTTP treats header names case-insensitively. For the SSE transport, enabling `log-requests` logs
request headers and can expose configured header values or authentication credentials.

### Authentication

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.mcp.clients.<client-name>.auth.type` | Authentication mechanism used for the MCP client. | Enum: `none`, `bearer`, `oauth2` | No | Must be a supported enum value. Authentication properties must match the selected type. | `none` |
| `agent.mcp.clients.<client-name>.auth.token` | Static bearer token added to the `Authorization` header. | String | When authentication type is `bearer` | Must be nonblank when set and absent for `none` and `oauth2`. | |
| `agent.mcp.clients.<client-name>.auth.oidc-client` | Name of the Quarkus OIDC client that supplies OAuth 2 access tokens. | String | When authentication type is `oauth2` | Must be nonblank when set and absent for `none` and `bearer`. | |

The authentication combinations are:

| Authentication type | Required property | Properties that must be absent |
| --- | --- | --- |
| `none` | None | `auth.token`, `auth.oidc-client` |
| `bearer` | `auth.token` | `auth.oidc-client`, an explicit `Authorization` header |
| `oauth2` | `auth.oidc-client` | `auth.token`, an explicit `Authorization` header |

### Tools and Specification Mappings

| Configuration | Description | Type | Required | Validations | Default |
| --- | --- | --- | --- | --- | --- |
| `agent.mcp.clients.<client-name>.tools.mode` | Controls which tools from this MCP client are exposed to the agent. `include` allows only named tools; `exclude` allows every tool except the named tools. | Enum: `all`, `include`, `exclude` | No | Must be a supported enum value. | `all` |
| `agent.mcp.clients.<client-name>.tools.names` | MCP server tool names used by the `include` or `exclude` policy. | List of strings | When mode is `include` or `exclude` | Must be absent when mode is `all`, must be nonempty when set, and every name must be nonblank. | |
| `agent.mcp.clients.<client-name>.tools.name-prefix` | Prefix added to exposed tool names that do not have an explicit specification name mapping. | String | No | Must not be blank when set. | `<client-name>_` |
| `agent.mcp.clients.<client-name>.tools.specification-mapping.<tool-name>.name` | Replacement name exposed to the model for the original MCP tool. | String | No | Mapping keys must not be blank. The replacement name must not be blank when set. | `<name-prefix><tool-name>` |
| `agent.mcp.clients.<client-name>.tools.specification-mapping.<tool-name>.description` | Replacement description exposed to the model for the original MCP tool. | String | No | Mapping keys must not be blank. The replacement description must not be blank when set. | Description supplied by the MCP server |
