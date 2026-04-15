# Unified Install Directory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the top directory selector a required shared install directory and align JDK/Maven/Node actions around unified install behavior.

**Architecture:** Reuse the current controller and service structure. Limit changes to FXML labels/layout, controller validation/state, and path utilities plus service method signatures needed to route all installs through the selected base directory.

**Tech Stack:** Java 17, JavaFX FXML, Maven

---

### Task 1: Update UI text and action wiring

**Files:**
- Modify: `src/main/resources/top/oneyi/envLauncher/jdk-installer.fxml`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] Rename the top section from JDK-specific wording to a required shared install directory.
- [ ] Rename the JDK action button text from `Download JDK` to `Install JDK`.
- [ ] Keep Maven and Node actions in the same `version + Install` shape.

### Task 2: Enforce required install directory in controller flow

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] Add a shared install-directory validation method.
- [ ] Block JDK, Maven, Node, and one-click install when the directory is missing.
- [ ] Keep the manual directory chooser as the source of truth for the shared base path.
- [ ] Update progress-step text to reflect install semantics instead of JDK-only download wording where needed.

### Task 3: Route component paths through the selected directory

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/utils/PathUtils.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] Add path helpers that build zip and extraction paths from a provided base directory.
- [ ] Update JDK, Maven, and Node install service entry points to accept the selected install directory.
- [ ] Reuse existing unzip and environment-variable logic after switching the path source.

### Task 4: Verify and document limits

**Files:**
- Modify: `README.md` if wording needs correction after the UI change

- [ ] Run `mvn clean compile`.
- [ ] Run `mvn test`.
- [ ] If local Java/Maven mismatch still blocks verification, record the exact failure and keep the code changes scoped.
