# Agent Examples

Each directory is a standalone Quarkus application and is included as an `example-<directory>`
Gradle project. The applications reuse the agent connector and add a workflow that demonstrates one
input/output combination.

| Example | Input | Output | Status |
| --- | --- | --- | --- |
| [text-to-text](text-to-text/README.md) | Text | Text | Available |
| `text-to-struct` | Text | Struct | Planned |
| `struct-to-text` | Struct | Text | Planned |
| `struct-to-struct` | Struct | Struct | Planned |

The examples use the LittleHorse and Ollama environment described in
[DEVELOPMENT.md](../DEVELOPMENT.md).
