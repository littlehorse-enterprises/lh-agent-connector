# lh-agent-connector

LH LangChain4j Task Worker Agent.

A [Quarkus](https://quarkus.io/) application that acts as a [LittleHorse](https://littlehorse.io/)
task worker and AI agent. It uses [LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
with OpenAI to expose LLM-backed task workers, plus example workflows registered only in the
`dev` profile.

## What's inside

- **`general` package** — a general purpose assistant.
  - `ask-llm` task: forwards a prompt to the LLM and returns its response. The assistant keeps a
    per-`WfRun` conversation memory (keyed by `WfRunId`) and its persona is configurable via the
    `lhc.general.system-message` property (no recompile needed).
  - `print-topic` task: asks the LLM what the current session is about and returns the summary,
    demonstrating the shared per-`WfRun` memory.
  - `ask-llm-workflow` (dev only): runs `ask-llm`, then `print-topic`, exposing both the answer and
    the inferred topic.
- **`email` package** — an email classifier.
  - `read-email` task: classifies an email as `SPAM`, `JOB_OPPORTUNITY` or `NOT_IMPORTANT`
    (the email assistant has no memory).
  - `email-workflow` (dev only): classifies an email and posts a Slack notification when
    it is a job opportunity.
- **`support` package** — a human-in-the-loop support agent.
  - `classify-support-ticket` task: classifies a ticket as `FEEDBACK` or `SUPPORT_REQUEST`.
  - `support-workflow` (dev only): logs plain feedback, or — for a support request — pauses on a
    LittleHorse `UserTask` so a human agent can resolve it, then feeds the agent's notes back to
    the LLM to draft a customer reply.
- **`filesystem` package** — a conversational agent with filesystem tools (via MCP).
  - `run-filesystem-agent` task: runs one agent turn; the agent has memory and the tools of the
    `filesystem` MCP server, and replies either `DONE` or `NEEDS_INPUT` with a question.
  - `filesystem-workflow` (dev only): loops the agent and the human — whenever the agent
    needs more info or approval it pauses on a `UserTask`; the human's answer is fed back and the
    conversation continues until the task is done.

Conversation memory for the stateful agents (`general`, `filesystem`) is persisted in Redis via a
custom `RedisChatMemoryStore`, so chat history survives application/container restarts.

## Prerequisites

- JDK 25
- A running LittleHorse server (defaults to `localhost:2023`)
- A running Redis instance for persistent chat memory (defaults to `localhost:6379`, set via
  `quarkus.redis.hosts`).
- Optionally [Ollama](https://ollama.com/): if it is installed and running locally, Quarkus uses
  that instance instead of starting an Ollama dev service automatically.
- Node.js (only for the `filesystem` package): its MCP server is launched with `npx`. The agent's
  allowed directory defaults to `${java.io.tmpdir}/lh-agent-workspace` (override with the
  `lhc.agent.workspace` property).

## Choosing the LLM provider

The agents can run against either OpenAI (GPT) or a local Ollama model. Select the active provider
with the `quarkus.langchain4j.chat-model.provider` property (defaults to `openai`):

- `openai` — requires an API key (`quarkus.langchain4j.openai.api-key`); model set via
  `quarkus.langchain4j.openai.chat-model.model-name`.
- `ollama` — requires a running [Ollama](https://ollama.com/) server; model set via
  `quarkus.langchain4j.ollama.chat-model.model-name`.

## Infrastructure

The LittleHorse server, Kafka and Redis can be started locally with the bundled Compose file via
Gradle (versions are taken from `gradle.properties`):

```bash
./gradlew dockerComposeUp    # start LittleHorse, Kafka and Redis
./gradlew dockerComposeDown  # stop them and remove volumes
```


Using OpenAI (default):

```bash
./gradlew quarkusDev -Dquarkus.langchain4j.openai.api-key=sk-your-openai-token
```

Using Ollama:

```bash
./gradlew quarkusDev -Dquarkus.langchain4j.chat-model.provider=ollama
```

## Trying the workflows

With the application running and `lhctl` pointed at the same LittleHorse server:

### General purpose assistant (`ask-llm-workflow`)

```bash
lhctl run ask-llm-workflow prompt "List all star wars movies"
```

### Email classifier (`email-workflow`)

```bash
# Job opportunity (triggers a Slack notification)
lhctl run email-workflow email "
Subject: Software Engineer position at Acme Corp

Hi, we came across your profile and would love to talk about a Senior Backend
Engineer role at Acme Corp. The position is remote and the salary range is
competitive. Are you available for a quick call this week?
"

# Spam
lhctl run email-workflow email "
Subject: You WON a FREE iPhone.

Congratulations. Click this link http://totally-legit.example to claim your
free prize now before it expires. Limited time only.
"
```

> The `email-workflow` job-opportunity branch calls the `saddle-bag-slack-post-message`
> task, which must be served by another worker for the Slack notification to be delivered. See [lh-saddle-bags](https://github.com/littlehorse-enterprises/lh-saddle-bags).

### Human-in-the-loop support agent (`support-workflow`)

```bash
# Plain feedback (just logged)
lhctl run support-workflow ticket "Just wanted to say your new dashboard looks great, keep it up."

# Actionable request (pauses on a UserTask for a human agent)
lhctl run support-workflow ticket "I was charged twice for my subscription this month, please help."
```

> For a support request the `support-workflow` pauses on the `resolve-support-ticket` UserTask.
> Complete it (e.g. from the LittleHorse dashboard) to resume the workflow; the LLM then drafts the
> customer reply from the human agent's resolution notes.

### Conversational filesystem agent (`filesystem-workflow`)

```bash
# Needs approval before a destructive action
lhctl run filesystem-workflow task "Delete every .log file in the workspace directory."

# Needs more info to continue
lhctl run filesystem-workflow task "Create a notes.txt file, but ask me what to write in it."
```

> The `filesystem-workflow` loops: whenever the agent needs more info or approval it pauses on
> the `provide-agent-info` UserTask (the agent's question is shown in the task notes). Complete it
> to feed your answer back to the agent and continue the conversation until the task is done.

## Building

```bash
./gradlew build
```
