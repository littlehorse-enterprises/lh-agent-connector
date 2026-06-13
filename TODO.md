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

- [ ] Integrate interactions with user tasks (LH `UserTask`s) so a human can be involved in the
      agent flow (e.g. review/approve LLM output).
- [ ] Add MCP servers to expose tools to the agents via the Model Context Protocol.
