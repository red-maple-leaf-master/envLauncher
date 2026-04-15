# Split Environment Services Design

**Goal**

Split the current mixed environment-variable logic into component-specific services for JDK, Maven, and Node, while keeping Windows command execution concerns isolated in a dedicated command service.

**Problem**

The current `EnvUtil` mixes three different responsibilities:

- JDK environment setup
- Maven environment setup and Maven-specific file/config updates
- Node environment setup and npm/cnpm configuration

This makes the code harder to change safely because component logic and Windows command details are coupled together.

**Design**

Create four focused services under `src/main/java/top/oneyi/envLauncher/service/`:

- `JdkEnvService`
  - Read current JDK environment information
  - Apply JDK environment variables
- `MavenEnvService`
  - Read current Maven environment information
  - Apply Maven environment variables
- `NodeEnvService`
  - Apply Node environment variables
  - Apply npm-related configuration
- `WindowsEnvCommandService`
  - Execute `setx`, registry writes, PATH updates, and PATH reads
  - Keep Windows-specific command details out of the component services

**Boundaries**

- `EnvInstallerController` and `EnvInstallerService` should call the new services instead of `EnvUtil`.
- `CmdUtil` can remain for now if needed, but Windows environment mutation logic should move behind `WindowsEnvCommandService`.
- `PathUtils` should not gain environment-variable responsibilities.
- Maven settings.xml update logic can stay in `EnvInstallerService` for this change because it is installation-time configuration, not shared environment-variable orchestration.

**Migration Plan**

1. Add `WindowsEnvCommandService` with the current low-level environment command behavior.
2. Move JDK behavior out of `EnvUtil` into `JdkEnvService`.
3. Move Maven behavior out of `EnvUtil` into `MavenEnvService`.
4. Move Node behavior and npm config out of `EnvUtil` into `NodeEnvService`.
5. Update callers to use the new services.
6. Remove obsolete logic from `EnvUtil`, or delete `EnvUtil` if nothing remains.

**Non-Goals**

- Do not redesign environment-variable behavior in this task.
- Do not change download sources, install directories, or UI flow in this task.
- Do not broaden this into a full command execution refactor outside environment setup.

**Risks**

- Because the code currently mixes system-level and user-level writes, the refactor must preserve behavior exactly before any bug-fix improvements.
- Node setup is the most coupled part because it also mutates npm config and installs `cnpm`.

**Verification**

- Build with `mvn clean compile`
- Run `mvn test`
- Manually verify one setup path still reaches the environment setup branch without controller/runtime errors
