# TODO

## LLM agents

- [x] Manage memory / session id for `GeneralPurposesLLM`. Use `@MemoryId` on the method (e.g. the
      `WfRunId`) so each conversation has its own isolated context instead of sharing a single
      in-memory window.
- [x] When running in Docker, persist/back up chat sessions (e.g. a `ChatMemoryStore` backed by
      Redis/Infinispan/DB) so memory survives container restarts. Implemented
      `RedisChatMemoryStore` backed by the `quarkus-redis-client`.
- [ ] Make the `@SystemMessage` configurable (e.g. `SystemMessageProvider` or a config property) so
      the persona/instructions can be changed without recompiling. Applies to both
      `GeneralPurposesLLM` and `EmailReaderLLM` (classification rules).

## Features

- [x] Integrate interactions with user tasks (LH `UserTask`s) so a human can be involved in the
      agent flow (e.g. review/approve LLM output). Implemented the `support` package: an LLM triages
      a ticket as FEEDBACK or SUPPORT_REQUEST; support requests pause on a `UserTask` for a human
      agent, whose resolution is fed back to the LLM to draft the customer reply.
- [x] Add MCP servers to expose tools to the agents via the Model Context Protocol. Added the
      `filesystem` package: a conversational agent with filesystem MCP tools that loops through
      `UserTask`s, asking the human for more info or approval before continuing a task.
