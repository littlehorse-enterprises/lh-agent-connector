# lh-agent-connector

LH LangChain4j Task Worker Agent.

A [Quarkus](https://quarkus.io/) application that acts as a [LittleHorse](https://littlehorse.io/)
task worker and AI agent. It uses [LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
with OpenAI to expose LLM-backed task workers, plus example workflows registered only in the
`dev` profile.

## What's inside

- **`general` package** — a general purpose assistant.
  - `ask-llm` task: forwards a prompt to the LLM and returns its response.
  - `agent-workflow` (dev only): runs `ask-llm` and exposes the answer.
- **`email` package** — an email classifier.
  - `read-email` task: classifies an email as `SPAM`, `JOB_OPPORTUNITY` or `NOT_IMPORTANT`
    (the email assistant has no memory).
  - `email-agent-workflow` (dev only): classifies an email and posts a Slack notification when
    it is a job opportunity.

## Prerequisites

- JDK 25
- A running LittleHorse server (defaults to `localhost:2023`)

## Choosing the LLM provider

The agents can run against either OpenAI (GPT) or a local Ollama model. Select the active provider
with the `quarkus.langchain4j.chat-model.provider` property (defaults to `openai`):

- `openai` — requires an API key (`quarkus.langchain4j.openai.api-key`); model set via
  `quarkus.langchain4j.openai.chat-model.model-name`.
- `ollama` — requires a running [Ollama](https://ollama.com/) server; model set via
  `quarkus.langchain4j.ollama.chat-model.model-name`.

## Running

Start the application in dev mode (registers the dev-profile workflows).

Using OpenAI (default):

```bash
./gradlew quarkusDev \
  -Dquarkus.log.category.\"io.littlehorse.connector\".level=DEBUG \
  -Dlhc.api.host=localhost \
  -Dlhc.api.port=2023 \
  -Dquarkus.http.port=9091 \
  -Dquarkus.langchain4j.openai.api-key=sk-your-openai-token
```

Using Ollama:

```bash
./gradlew quarkusDev \
  -Dquarkus.log.category.\"io.littlehorse.connector\".level=DEBUG \
  -Dlhc.api.host=localhost \
  -Dlhc.api.port=2023 \
  -Dquarkus.http.port=9091 \
  -Dquarkus.langchain4j.chat-model.provider=ollama
```

## Trying the workflows

With the application running and `lhctl` pointed at the same LittleHorse server:

```bash
# General purpose assistant
lhctl run agent-workflow prompt "List all star wars movies"

# Email classifier — job opportunity (triggers a Slack notification)
lhctl run email-agent-workflow email "
Subject: Software Engineer position at Acme Corp

Hi, we came across your profile and would love to talk about a Senior Backend
Engineer role at Acme Corp. The position is remote and the salary range is
competitive. Are you available for a quick call this week?
"

# Email classifier — spam
lhctl run email-agent-workflow email "
Subject: You WON a FREE iPhone.

Congratulations. Click this link http://totally-legit.example to claim your
free prize now before it expires. Limited time only.
"
```

> The `email-agent-workflow` job-opportunity branch calls the `saddle-bag-slack-post-message`
> task, which must be served by another worker for the Slack notification to be delivered.

## Building

```bash
./gradlew build
```
