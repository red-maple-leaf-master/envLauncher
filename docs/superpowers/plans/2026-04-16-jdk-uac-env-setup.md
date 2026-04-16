# JDK UAC Environment Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trigger Windows UAC only when the JDK installation flow writes machine-level environment variables so `JAVA_HOME` and system `Path` do not silently fall back to the current user.

**Architecture:** Keep JDK download and extraction unchanged. Add a dedicated Windows elevation path in the environment command layer, return explicit result states from the JDK environment service, and let the controller log success, cancellation, or failure without reporting a false full install success.

**Tech Stack:** Java 17, JavaFX, Maven, JUnit 4, Windows `setx` and `reg` commands, PowerShell `Start-Process -Verb RunAs`

---

### Task 1: Define the expected JDK environment result states

**Files:**
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void requestsElevationWhenMachineScopeNeedsAdminRights() throws Exception {
    // Verify the service returns a result that indicates elevation was used.
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: FAIL because the new result type and elevation branch do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
public JdkEnvironmentResult configureJdkEnvironment(String javaHome, String jdkBinPath) throws Exception {
    // Return explicit result objects instead of relying on implicit fallback logs.
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java
git commit -m "feat: AI生成-补充JDK环境设置结果语义"
```

### Task 2: Add the Windows elevation executor

**Files:**
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/utils/CmdUtil.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void reportsCancellationWhenElevationIsDeclined() throws Exception {
    // Verify declined UAC is not treated as user-scope success.
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: FAIL because cancellation handling is missing.

- [ ] **Step 3: Write minimal implementation**

```java
public ElevationResult applyJdkMachineEnvironmentWithElevation(String javaHome, String pathEntry) throws IOException {
    // Launch a controlled elevated PowerShell command and decode exit status.
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java src/main/java/top/oneyi/envLauncher/utils/CmdUtil.java
git commit -m "feat: AI生成-新增JDK环境写入提权执行器"
```

### Task 3: Update controller logging and install completion messaging

**Files:**
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void marksInstallIncompleteWhenElevationIsCancelled() throws Exception {
    // Verify the controller-facing result does not report full install success.
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: FAIL because the controller still logs generic success.

- [ ] **Step 3: Write minimal implementation**

```java
if (result.isCompleted()) {
    LoggerUtil.info("JDK install completed.");
} else {
    LoggerUtil.info(result.message());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=.m2repo`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java
git commit -m "fix: AI生成-修正JDK提权取消后的安装日志"
```

### Task 4: Verify the full build

**Files:**
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] **Step 1: Run targeted tests**

Run: `mvn -Dtest=JdkEnvServiceTest,CmdUtilTest test -Dmaven.repo.local=.m2repo`
Expected: PASS

- [ ] **Step 2: Run compile verification**

Run: `mvn clean compile -Dmaven.repo.local=.m2repo`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run full test verification**

Run: `mvn test -Dmaven.repo.local=.m2repo`
Expected: BUILD SUCCESS

- [ ] **Step 4: Manual smoke check**

Run the app and confirm one JDK install flow shows:
- UAC only appears during environment setup
- approve: machine-level environment write succeeds
- cancel: logs clearly say JDK files exist but environment setup is incomplete

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-04-16-jdk-uac-env-setup.md
git commit -m "docs: AI生成-补充JDK提权环境设置计划"
```
