# lh-agent-connector

LH LangChain4j Task Worker Agent.

A [Quarkus](https://quarkus.io/) application that acts as a [LittleHorse](https://littlehorse.io/)
task worker and AI agent. It uses [LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
with OpenAI to expose LLM-backed task workers, plus example workflows registered only in the
`dev` profile.

## What's inside

- **`general` package** — a general purpose assistant.
  - `ask-llm` task: forwards a prompt to the LLM and returns its response.
  - `ask-llm-workflow` (dev only): runs `ask-llm` and exposes the answer.
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
./gradlew quarkusDev -Dquarkus.langchain4j.openai.api-key=sk-your-openai-token
```

Using Ollama:

```bash
./gradlew quarkusDev -Dquarkus.langchain4j.chat-model.provider=ollama
```

## Trying the workflows

With the application running and `lhctl` pointed at the same LittleHorse server:

```bash
# General purpose assistant
lhctl run ask-llm-workflow prompt "List all star wars movies"

# Email classifier — job opportunity (triggers a Slack notification)
lhctl run email-workflow email "
Subject: Software Engineer position at Acme Corp

Hi, we came across your profile and would love to talk about a Senior Backend
Engineer role at Acme Corp. The position is remote and the salary range is
competitive. Are you available for a quick call this week?
"

# Email classifier — spam
lhctl run email-workflow email "
Subject: You WON a FREE iPhone.

Congratulations. Click this link http://totally-legit.example to claim your
free prize now before it expires. Limited time only.
"
```

> The `email-workflow` job-opportunity branch calls the `saddle-bag-slack-post-message`
> task, which must be served by another worker for the Slack notification to be delivered.

```bash
# Support agent — plain feedback (just logged)
lhctl run support-workflow ticket "Just wanted to say your new dashboard looks great, keep it up."

# Support agent — actionable request (pauses on a UserTask for a human agent)
lhctl run support-workflow ticket "I was charged twice for my subscription this month, please help."
```

> For a support request the `support-workflow` pauses on the `resolve-support-ticket` UserTask.
> Complete it (e.g. from the LittleHorse dashboard) to resume the workflow; the LLM then drafts the
> customer reply from the human agent's resolution notes.

## Building

```bash
./gradlew build
```
