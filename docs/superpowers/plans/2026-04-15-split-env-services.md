# Split Environment Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the mixed environment-variable utility with component-specific JDK, Maven, and Node services plus a Windows command service that isolates platform command details.

**Architecture:** Keep installation flow unchanged and move only environment-related orchestration. Component services own JDK, Maven, and Node behavior, while one Windows command service owns `setx`, registry, PATH, and PATH-read commands.

**Tech Stack:** Java 17, JavaFX, Maven, Windows command execution

---

### Task 1: Add Windows command boundary

**Files:**
- Create: `src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/utils/CmdUtil.java`

- [ ] Add a focused service that wraps PATH reads, `setx`, registry writes, and PATH registry updates.
- [ ] Move environment-mutation call sites away from direct component knowledge.
- [ ] Keep `CmdUtil` only as a low-level helper if any shared process execution remains necessary.

### Task 2: Split JDK environment behavior

**Files:**
- Create: `src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] Move JDK environment read and apply behavior out of `EnvUtil`.
- [ ] Update controller code to use `JdkEnvService` for JDK environment application and lookup.
- [ ] Preserve existing logging and behavior.

### Task 3: Split Maven environment behavior

**Files:**
- Create: `src/main/java/top/oneyi/envLauncher/service/MavenEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`

- [ ] Move Maven environment read and apply behavior out of `EnvUtil`.
- [ ] Update installer and controller callers to use `MavenEnvService`.
- [ ] Keep Maven settings.xml update logic in `EnvInstallerService` for now.

### Task 4: Split Node environment behavior

**Files:**
- Create: `src/main/java/top/oneyi/envLauncher/service/NodeEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`

- [ ] Move Node environment and npm/cnpm configuration behavior out of `EnvUtil`.
- [ ] Update installer code to use `NodeEnvService`.
- [ ] Preserve current Node side effects exactly in this refactor.

### Task 5: Remove obsolete utility surface

**Files:**
- Modify or Delete: `src/main/java/top/oneyi/envLauncher/utils/EnvUtil.java`

- [ ] Remove migrated logic from `EnvUtil`.
- [ ] Delete `EnvUtil` if it becomes empty.
- [ ] Make sure no caller still depends on the old utility.

### Task 6: Verification and cleanup

**Files:**
- Modify: documentation only if the new service structure needs a note

- [ ] Run `mvn clean compile`.
- [ ] Run `mvn test`.
- [ ] Record the current Java 8 / Java 17 build-environment blocker if verification still fails before app code compilation completes.
- [ ] Manually inspect the JDK, Maven, and Node call paths to confirm each now routes through its own service.
