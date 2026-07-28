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
  - `read-email` task: classifies an email as `SPAM`, `SALES_OPPORTUNITY` or `NOT_IMPORTANT`
    (the email assistant has no memory).
  - `send-alert` task: dummy alert sender that logs the alert message.
  - `email-workflow` (dev only): classifies an email and sends an alert when
    it is a sales opportunity.
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

Conversation memory for the stateful agents (`general`, `filesystem`) is persisted in PostgreSQL via
a custom `PostgresChatMemoryStore`, so chat history survives application/container restarts.

## Prerequisites

- JDK 25
- A running LittleHorse server (defaults to `localhost:2023`)
- A running PostgreSQL instance for persistent chat memory (defaults to `localhost:5432`, database
  `lh_agent`, set via `quarkus.datasource.*`).
- Optionally [Ollama](https://ollama.com/): if it is installed and running locally, Quarkus uses
  that instance instead of starting an Ollama dev service automatically.
- Node.js (only for the `filesystem` package): its MCP server is launched with `npx`. The agent's
  allowed directory defaults to `${java.io.tmpdir}/lh-agent-workspace` (override with the
  `lhc.agent.workspace` property).

## Choosing the LLM provider

The agents can run against OpenAI (GPT), Anthropic (Claude), or a local Ollama model. Select the
active provider with the `quarkus.langchain4j.chat-model.provider` property (defaults to `openai`):

- `openai` — requires an API key (`quarkus.langchain4j.openai.api-key`); model set via
  `quarkus.langchain4j.openai.chat-model.model-name`.
- `anthropic` — requires an API key (`quarkus.langchain4j.anthropic.api-key`); model set via
  `quarkus.langchain4j.anthropic.chat-model.model-name`.
- `ollama` — requires a running [Ollama](https://ollama.com/) server; model set via
  `quarkus.langchain4j.ollama.chat-model.model-name`.

## Infrastructure

The LittleHorse server, Kafka and PostgreSQL can be started locally with the bundled Compose file via
Gradle (versions are taken from `gradle.properties`):

```bash
./gradlew dockerComposeUp    # start LittleHorse, Kafka and PostgreSQL
./gradlew dockerComposeDown  # stop them and remove volumes
```


Using OpenAI (default):

```bash
./gradlew quarkusDev -Dquarkus.langchain4j.openai.api-key="${OPENAI_API_KEY}"
```

Using Anthropic:

```bash
./gradlew quarkusDev \
  -Dquarkus.langchain4j.chat-model.provider=anthropic \
  -Dquarkus.langchain4j.anthropic.api-key="${ANTHROPIC_API_KEY}"
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
# Sales opportunity (triggers an alert)
lhctl run email-workflow email "
Subject: Interested in your product for Acme Corp

Hi, we came across your product and would love to talk about adopting it at
Acme Corp. We have a team of 50 engineers and a budget approved for this
quarter. Are you available for a quick call this week?
"

# Spam
lhctl run email-workflow email "
Subject: You WON a FREE iPhone.

Congratulations. Click this link http://totally-legit.example to claim your
free prize now before it expires. Limited time only.
"
```

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

This walks through a multi-turn run where the agent first asks what to create, then you answer via a
UserTask, and the agent produces the files.

```bash
# 1. Start the workflow. The agent pauses and asks what it should create.
lhctl run filesystem-workflow task "Create some files in the workspace, but first ask me exactly what files to create and what each one should contain."
# -> prints the WfRunId, e.g. 4d1a0c9e6f4b4b0e9c2f1a2b3c4d5e6f
```

```bash
# 2. Find the pending UserTask for that WfRun (returns its userTaskGuid).
lhctl search userTaskRun <wfRunId>

# Optional: read the task to see the agent's question in the notes.
lhctl get userTaskRun <wfRunId> <userTaskGuid>
```

```bash
# 3. Complete the UserTask. lhctl prompts for your userId, then the "Answer" field.
lhctl execute userTaskRun <wfRunId> <userTaskGuid>
# userId: alice
# Answer: Create one file per Star Wars movie. Name each file after the movie
#         and write that movie's score inside the file.
```

The agent resumes, creates one file per movie (each containing the movie's score), and — if it needs
another confirmation — pauses on a new UserTask. Repeat steps 2–3 until the workflow finishes.

## Building

```bash
./gradlew build
```
