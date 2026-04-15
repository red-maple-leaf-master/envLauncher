# Unified Install Directory Design

**Goal**

Make the top path selector a required shared install directory for JDK, Maven, and Node, and unify the component actions around `Install` semantics.

**Scope**

- Replace the current JDK-only top path with a required shared install directory.
- Keep JDK, Maven, and Node as separate install actions plus the existing one-click flow.
- Route download and extraction paths through the selected install directory instead of the current drive root default.
- Keep existing download source behavior and background-task behavior unchanged.

**Behavior**

- The user must choose an install directory before starting any component install or one-click install.
- JDK, Maven, and Node each use `version + Install` in the main install area.
- Download archives are stored under `<installDir>\\downloads`.
- Extracted component folders are created directly under `<installDir>`.
- JDK path field is repurposed to hold the selected shared install directory.

**Path Rules**

- JDK zip: `<installDir>\\downloads\\jdk-<version>.zip`
- JDK extract dir: `<installDir>\\jdk-<version>`
- Maven zip: `<installDir>\\downloads\\apache-maven-<version>.zip`
- Maven extract dir: `<installDir>\\apache-maven-<version>`
- Node zip: `<installDir>\\downloads\\node-<version>.zip`
- Node extract dir: `<installDir>\\node-<version>`

**Constraints**

- Use minimal code changes.
- Preserve JavaFX threading rules.
- Preserve existing installation logic where possible.
- Keep resource paths and module configuration unchanged.
