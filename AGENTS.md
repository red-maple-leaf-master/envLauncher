<!-- OPENSPEC:START -->
# OpenSpec Instructions

## Goals
This project is a Windows environment installer (JDK/Maven/Node) based on JavaFX. All changes should prioritize "installable, runnable, and rollback-capable".

## Hard Constraints
1. Java and build version are fixed at JDK 17 (see `pom.xml`). Do not upgrade Java or core dependency versions without explicit requirements.
2. Build tool is fixed at Maven; use `mvn clean compile` and `mvn test` as basic validation commands by default.
3. Module system must remain functional: after adding packages/controllers, if FXML reflection access is involved, synchronously update the `opens` directives in `src/main/java/module-info.java`.
4. FXML, styles, and icon paths must remain resolvable: do not arbitrarily rename resources under `src/main/resources/top/oneyi/envLauncher/` and `src/main/resources/icons/`.
5. This is a Windows-first project: environment variable and PATH logic should be compatible with `setx`/registry behavior; do not introduce Linux/macOS-only commands.
6. Long-running tasks involving downloads, extraction, and environment variable writes must run on background threads/`Task`; UI updates must go through `Platform.runLater(...)`.
7. Do not hardcode development machine absolute paths in code; use `PathUtils` or `File`/`Path` API for path concatenation.
8. Without explicit requirements, do not modify download source policies (TUNA, npmmirror, Apache archive) and installation directory conventions to avoid impacting existing user environments.
9. Comment requirements: new or modified key logic must have concise comments explaining "why this is done"; avoid meaningless comments.
10. Logging requirements: code involving downloads, extraction, environment variables, external commands, and exception branches must log entries; success, failure, and cancellation states must all be traceable.

## Change Boundaries
1. Prefer minimal changes; avoid combining "large refactoring + behavioral changes" simultaneously.
2. When modifying system-related code like `EnvUtil`, `CmdUtil`, or `EnvInstallerService`, add failure scenario handling and logging.
3. When adding new UI interactions, maintain minimum window size and main workflow usability without blocking the main thread.
4. Any feature changes lacking necessary comments or critical logging are considered incomplete and cannot be submitted.

## Pre-submission Checks
1. Compilation passes: `mvn clean compile`
2. Tests pass: `mvn test`
3. Manually verify at least one installation path (JDK/Maven/Node) does not break existing workflows
4. Commit messages must be in Chinese, follow the commit specification format, and indicate AI generation
<!-- OPENSPEC:END -->