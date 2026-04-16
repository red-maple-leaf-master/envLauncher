# Unified Environment UAC Rollout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify JDK, Maven, and Node environment-variable setup so machine-level writes always trigger UAC when needed, without elevating npm configuration or other non-environment tasks.

**Architecture:** Extract the JDK environment elevation flow into a reusable service-layer template that returns a shared result object. Migrate Maven and Node to that template, then update service and controller flows so incomplete environment setup never reports a false install success and Node skips npm configuration when environment writes are cancelled or fail.

**Tech Stack:** Java 17, JavaFX, Maven, JUnit 4, Windows PowerShell UAC elevation, `setx`, `reg`

---

### Task 1: Extract a shared elevated environment configuration template

**Files:**
- Create: `src/main/java/top/oneyi/envLauncher/service/EnvironmentSetupResult.java`
- Create: `src/main/java/top/oneyi/envLauncher/service/AbstractEnvSetupService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void jdkUsesSharedEnvironmentSetupResultAfterElevation() throws Exception {
    FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
    commandService.elevated = false;
    commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
    JdkEnvService service = new JdkEnvService(commandService);

    EnvironmentSetupResult result = service.configureJdkEnvironment("C:\\env\\jdk-17", "%JAVA_HOME%\\bin");

    assertTrue(result.isCompleted());
    assertTrue(result.usedElevation());
    assertEquals("completed", result.status());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: FAIL because `EnvironmentSetupResult` and the shared template base class do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
public final class EnvironmentSetupResult {
    private final String status;
    private final boolean completed;
    private final boolean usedElevation;
    private final String message;
}

public abstract class AbstractEnvSetupService extends AbstractPathEnvService {
    protected EnvironmentSetupResult configureEnvironment(String toolName,
                                                          Map<String, String> machineVariables,
                                                          List<String> pathEntries,
                                                          String... excludeKeywords) throws Exception {
        // Shared elevated machine-write flow.
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=JdkEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/top/oneyi/envLauncher/service/EnvironmentSetupResult.java src/main/java/top/oneyi/envLauncher/service/AbstractEnvSetupService.java src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java
git commit -m "refactor: AI生成-抽取统一环境提权模板"
```

### Task 2: Move Windows elevated writes from JDK-only to shared commands

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void elevatedMachineWriteAcceptsMultipleVariablesAndPathEntries() throws Exception {
    FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
    commandService.elevated = false;
    commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();

    EnvironmentSetupResult result = new MavenEnvService(commandService)
            .configureMavenEnvironment("C:\\env\\maven-3.9.10", "C:\\env\\maven-3.9.10\\bin");

    assertTrue(result.isCompleted());
    assertEquals("C:\\env\\maven-3.9.10", commandService.elevatedMachineVariables.get("MAVEN_HOME"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=JdkEnvServiceTest,MavenEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: FAIL because the Windows elevation command still only supports JDK-specific variable/path writes.

- [ ] **Step 3: Write minimal implementation**

```java
public ElevationResult applyMachineEnvironmentWithElevation(Map<String, String> variables,
                                                            String pathValue) throws IOException {
    // Build a controlled elevated PowerShell script that writes all variables plus machine Path.
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=JdkEnvServiceTest,MavenEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java
git commit -m "refactor: AI生成-扩展通用环境变量提权写入"
```

### Task 3: Migrate Maven to the shared UAC result flow

**Files:**
- Create: `src/test/java/top/oneyi/envLauncher/service/MavenEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/MavenEnvService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void mavenRequestsElevationWhenMachineEnvironmentNeedsAdminRights() throws Exception {
    FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
    commandService.elevated = false;
    commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
    MavenEnvService service = new MavenEnvService(commandService);

    EnvironmentSetupResult result = service.configureMavenEnvironment("C:\\env\\maven-3.9.10", "C:\\env\\maven-3.9.10\\bin");

    assertTrue(result.isCompleted());
    assertTrue(result.usedElevation());
    assertEquals("C:\\env\\maven-3.9.10", commandService.elevatedMachineVariables.get("MAVEN_HOME"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=MavenEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: FAIL because `MavenEnvService` still returns `void` and still falls back to current user scope.

- [ ] **Step 3: Write minimal implementation**

```java
public class MavenEnvService extends AbstractEnvSetupService {
    public EnvironmentSetupResult configureMavenEnvironment(String mavenHome, String mavenBinPath) throws Exception {
        return configureEnvironment(
                "Maven",
                Map.of("MAVEN_HOME", mavenHome),
                List.of(mavenBinPath),
                "maven"
        );
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=MavenEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/top/oneyi/envLauncher/service/MavenEnvServiceTest.java src/main/java/top/oneyi/envLauncher/service/MavenEnvService.java
git commit -m "feat: AI生成-统一Maven环境提权流程"
```

### Task 4: Migrate Node environment writes and keep npm configuration non-elevated

**Files:**
- Create: `src/test/java/top/oneyi/envLauncher/service/NodeEnvServiceTest.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/NodeEnvService.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void nodeSkipsNpmConfigurationWhenElevationIsCancelled() throws Exception {
    FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
    commandService.elevated = false;
    commandService.elevationResult = WindowsEnvCommandService.ElevationResult.cancelled("UAC cancelled");
    RecordingNodeEnvService service = new RecordingNodeEnvService(commandService);

    EnvironmentSetupResult result = service.configureNodeEnvironment("C:\\env\\node-v20", "%NODE_HOME%");

    assertFalse(result.isCompleted());
    assertFalse(service.npmConfigured);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=NodeEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: FAIL because `NodeEnvService` still falls back to user scope and still continues into npm configuration.

- [ ] **Step 3: Write minimal implementation**

```java
public EnvironmentSetupResult configureNodeEnvironment(String nodeHome, String nodePathEntry) throws Exception {
    EnvironmentSetupResult envResult = configureEnvironment(
            "Node",
            Map.of("NODE_HOME", nodeHome),
            List.of(nodePathEntry, globalInstallPath, cachePath)
    );
    if (!envResult.isCompleted()) {
        LoggerUtil.info("Node environment setup incomplete. Skip npm configuration.");
        return envResult;
    }
    configureNpmPaths(cachePath, globalInstallPath);
    return envResult;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=NodeEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/top/oneyi/envLauncher/service/NodeEnvServiceTest.java src/main/java/top/oneyi/envLauncher/service/NodeEnvService.java
git commit -m "feat: AI生成-统一Node环境提权并保留普通npm配置"
```

### Task 5: Update service and controller success criteria

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/NodeEnvServiceTest.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/MavenEnvServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void nodeInstallDoesNotReportSuccessWhenEnvironmentSetupIsIncomplete() {
    // Expect install callback to receive false after cancelled elevation.
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=MavenEnvServiceTest,NodeEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: FAIL because installer flows still treat “no exception” as success and controllers still log generic completion.

- [ ] **Step 3: Write minimal implementation**

```java
EnvironmentSetupResult envResult = nodeEnvService.configureNodeEnvironment(nodeHome, "%NODE_HOME%");
if (!envResult.isCompleted()) {
    LoggerUtil.info("Node files are installed, but environment setup is incomplete.");
    return false;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=MavenEnvServiceTest,NodeEnvServiceTest,JdkEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/service/MavenEnvServiceTest.java src/test/java/top/oneyi/envLauncher/service/NodeEnvServiceTest.java
git commit -m "fix: AI生成-统一安装完成状态与环境提权结果"
```

### Task 6: Verify the full build

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/service/AbstractEnvSetupService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvironmentSetupResult.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/MavenEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/NodeEnvService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`
- Modify: `src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/JdkEnvServiceTest.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/MavenEnvServiceTest.java`
- Modify: `src/test/java/top/oneyi/envLauncher/service/NodeEnvServiceTest.java`

- [ ] **Step 1: Run targeted environment setup tests**

Run: `mvn -Dtest=JdkEnvServiceTest,MavenEnvServiceTest,NodeEnvServiceTest test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: PASS

- [ ] **Step 2: Run compile verification**

Run: `mvn clean compile -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run full test verification**

Run: `mvn test -Dmaven.repo.local=C:\Users\yi.wan\.m2\repository`
Expected: BUILD SUCCESS

- [ ] **Step 4: Manual smoke check**

Run the app and verify:
- JDK/Maven/Node only prompt UAC during environment variable setup
- approving UAC completes machine-level environment setup
- cancelling UAC keeps files installed but logs environment setup incomplete
- Node does not run npm configuration after cancelled or failed elevation

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-04-16-unified-env-uac-rollout.md
git commit -m "docs: AI生成-补充统一环境提权实施计划"
```
