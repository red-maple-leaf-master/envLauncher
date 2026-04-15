# Portable Windows Runtime Bundle Design

## Context

`env-launcher` is a Windows-first JavaFX desktop application built with JDK 17 and Maven.
The current project already uses `javafx-maven-plugin`, but the packaging flow is still described in a generic way and does not explicitly guarantee that end users can run the application on machines without a preinstalled JDK/JRE.

The new goal is to produce a portable Windows distribution directory that:

- includes the application launcher
- includes a bundled Java runtime
- can be unpacked and launched directly by double-clicking the `.exe`
- does not require the target machine to have JDK or JRE installed

This change must preserve the current Java 17 baseline and keep the build on Maven.

## Recommended Approach

Use the existing `javafx-maven-plugin` to generate a `jlink` runtime image directory.

Why this approach:

- it fits the current JavaFX modular project structure
- it produces a portable directory with an embedded runtime
- it avoids introducing third-party EXE wrappers or a separate installer toolchain
- it keeps the scope limited to build and documentation changes

Rejected alternatives:

- `jpackage` app-image: viable, but adds extra packaging complexity that is not required for the current "portable directory" goal
- jar + external JRE directory + custom wrapper: higher maintenance cost and less aligned with the existing modular JavaFX setup

## Output Design

The build should generate a Windows portable directory under `target/` that contains:

- a launcher executable in `bin/`
- the bundled runtime image
- the application modules and dependencies needed by the launcher

Expected user flow:

1. Build the project on a machine with JDK 17.
2. Take the generated output directory from `target/`.
3. Copy or zip that directory for distribution.
4. On another Windows machine, unpack the directory and run the launcher `.exe` directly.

The portable bundle is explicitly not an installer. It is a self-contained runtime directory.

## Build Configuration Changes

`pom.xml` will be updated so the JavaFX plugin configuration clearly supports portable runtime image generation.

Planned changes:

- keep Java 17 and current JavaFX versions unchanged
- keep `winLauncherType=gui`
- ensure the launcher name is stable and readable for Windows users
- ensure the runtime image output name is stable and aligned with the project name
- document the Maven command that generates the portable directory

The build should prefer an explicit packaging command such as:

```bash
mvn clean javafx:jlink
```

If the existing `package` phase can be wired safely to produce the same portable image without surprising side effects, that can be considered during implementation. The default recommendation remains an explicit `javafx:jlink` command because it is clearer and less risky.

## Runtime and Resource Behavior

The bundled runtime must include what the current modular application needs to start successfully on Windows:

- Java base/runtime modules
- JavaFX modules already used by the app
- XML support already required by the project

No application behavior changes are intended. This work only affects how the app is packaged and launched.

Resource expectations remain unchanged:

- FXML and stylesheet resources must continue resolving from the packaged application
- icon resources must remain available to the launcher and application window

## Verification Plan

Implementation will be considered complete only if all of the following are checked:

1. `mvn clean compile` succeeds.
2. `mvn test` succeeds.
3. `mvn clean javafx:jlink` succeeds.
4. The generated portable directory contains a Windows launcher `.exe`.
5. The generated portable directory contains a bundled runtime.
6. The launcher starts successfully from the generated directory on the build machine without relying on `java` from `PATH`.

Manual verification on a second clean Windows machine is desirable but not required for the first implementation pass.

## Risks and Mitigations

Risk: the plugin generates a directory name or launcher name that is confusing for release use.
Mitigation: set explicit image and launcher names in Maven config and document the exact output path.

Risk: a required module is missing from the runtime image.
Mitigation: keep module declarations aligned with `module-info.java` and validate startup from the generated bundle.

Risk: documentation still tells users to rely on a system JDK/JRE.
Mitigation: update README packaging and running instructions to distinguish developer build requirements from end-user runtime requirements.
