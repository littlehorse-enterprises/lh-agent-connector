# Commit Messages

Always use the Conventional Commits standard for every commit message.

Required format:

`<type>(<optional scope>): <description>`

Allowed types:

- `feat`: new functionality
- `fix`: bug fixes
- `docs`: documentation changes
- `style`: formatting or style changes without functional impact
- `refactor`: code restructuring without adding features or fixing bugs
- `perf`: performance improvements
- `test`: adding or updating tests
- `build`: build system or dependency changes
- `ci`: continuous integration or deployment changes
- `chore`: maintenance tasks
- `revert`: reverting a previous commit

Rules:

- Write the type and description in lowercase.
- Use a short, clear description in the imperative mood.
- Do not end the description with a period.
- Use a scope when it helps identify the affected area.
- Keep the first line preferably under 72 characters.
- Use `!` after the type or scope for breaking changes.
- Include a body when needed to explain the reason or context.
- Use a `BREAKING CHANGE:` footer when applicable.
- Analyze the changes before choosing the type and scope.
- Never generate commit messages that do not follow Conventional Commits.
- Do not wrap the final commit message in Markdown code blocks or quotation marks.
- Always add the following footer at the end of the commit, preceded by a blank line:

  `Assisted-by: <model> <version>`

Replace `<model>` and `<version>` with the actual name and version of the model that generated the
commit. Do not invent this information. If the exact version cannot be determined, include only the
available model name.

Example:

```text
feat(auth): add password reset flow

Add token expiration validation and reset email handling.

Assisted-by: Codex GPT-5
```
