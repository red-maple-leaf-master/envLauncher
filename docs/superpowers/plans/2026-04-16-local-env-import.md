# Local Env Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-tool "use local directory" actions for JDK, Maven, and Node so users can skip downloads and only apply environment configuration, while moving `Show Config` onto its own row.

**Architecture:** Keep the existing download/install services unchanged and add a parallel "local import" path in `EnvInstallerController`. The new path uses `DirectoryChooser`, validates the selected tool root locally, then reuses the existing JDK/Maven/Node environment setup services so elevation, PATH updates, cancellation handling, and broadcast refresh stay centralized.

**Tech Stack:** JavaFX FXML, JavaFX controller/task APIs, existing `JdkEnvService` / `MavenEnvService` / `NodeEnvService`, JUnit 4

---

## File Map

- Modify: `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`
  - Add `Use Local JDK`, `Use Local Maven`, `Use Local Node` buttons next to the existing install buttons.
  - Move `Show Config` to its own row in the environment section.
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
  - Add local directory selection actions.
  - Add lightweight validation helpers for JDK/Maven/Node roots.
  - Reuse existing environment configuration services in background `Task`s.
  - Keep `busy` state and logging behavior consistent with the rest of the UI.
- Add: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`
  - Add focused controller-level tests for local root validation helpers and local env configuration flow boundaries.

### Task 1: Reshape The UI For Local Import Actions

**Files:**
- Modify: `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`

- [ ] **Step 1: Write the failing UI structure test by asserting the new FXML ids exist**

Add a new JUnit test method to `src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java`:

```java
    @Test
    public void envInstallerLayoutContainsLocalImportButtons() throws Exception {
        URL resource = MainApp.class.getResource("env-installer.fxml");
        assertNotNull("env-installer.fxml should exist", resource);

        String fxml = Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8);

        assertTrue(fxml.contains("fx:id=\"useLocalJdkButton\""));
        assertTrue(fxml.contains("fx:id=\"useLocalMavenButton\""));
        assertTrue(fxml.contains("fx:id=\"useLocalNodeButton\""));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerLayoutTest#envInstallerLayoutContainsLocalImportButtons" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the three new button ids do not exist in `env-installer.fxml`.

- [ ] **Step 3: Add the minimal FXML changes**

Update the component sections in `src/main/resources/top/oneyi/envLauncher/env-installer.fxml` so the button rows look like this:

```xml
            <VBox spacing="10" styleClass="card-panel">
                <Label text="Component Install" styleClass="section-title"/>
                <FlowPane hgap="10" vgap="10" styleClass="action-flow" prefWrapLength="760">
                    <ComboBox fx:id="jdkVersionCombo" prefWidth="110" prefHeight="36" styleClass="env-combo"/>
                    <Button fx:id="installJdkButton" text="Install JDK" onAction="#onInstallJdk" prefHeight="36" styleClass="action-button primary"/>
                    <Button fx:id="useLocalJdkButton" text="Use Local JDK" onAction="#onUseLocalJdk" prefHeight="36" styleClass="action-button secondary"/>
                    <ComboBox fx:id="mavenVersionCombo" prefWidth="170" prefHeight="36" styleClass="env-combo"/>
                    <Button fx:id="installMavenButton" text="Install Maven" onAction="#onInstallMaven" prefHeight="36" styleClass="action-button primary"/>
                    <Button fx:id="useLocalMavenButton" text="Use Local Maven" onAction="#onUseLocalMaven" prefHeight="36" styleClass="action-button secondary"/>
                </FlowPane>
                <FlowPane hgap="10" vgap="10" styleClass="action-flow" prefWrapLength="760">
                    <ComboBox fx:id="nodeVersionCombo" prefWidth="120" prefHeight="36" styleClass="env-combo"/>
                    <Button fx:id="installNodeButton" text="Install Node" onAction="#onInstallNode" prefHeight="36" styleClass="action-button primary"/>
                    <Button fx:id="useLocalNodeButton" text="Use Local Node" onAction="#onUseLocalNode" prefHeight="36" styleClass="action-button secondary"/>
                </FlowPane>
            </VBox>

            <VBox spacing="10" styleClass="card-panel">
                <Label text="Environment" styleClass="section-title"/>
                <FlowPane hgap="10" vgap="10" styleClass="action-flow" prefWrapLength="760">
                    <Button fx:id="showConfigButton" text="Show Config" onAction="#onShowCurrentConfig" prefHeight="36" styleClass="action-button neutral"/>
                </FlowPane>
            </VBox>
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerLayoutTest#envInstallerLayoutContainsLocalImportButtons" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/top/oneyi/envLauncher/env-installer.fxml src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java
git commit -m "feat: AI生成-补充本地环境导入界面"
```

### Task 2: Add Root Validation Helpers For Local JDK Maven And Node

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Add: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write failing tests for local root validation**

Create `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java` with these tests:

```java
package top.oneyi.envLauncher.controller;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnvInstallerControllerTest {

    @Test
    public void acceptsJdkRootWhenJavaExeExists() throws IOException {
        Path root = Files.createTempDirectory("jdk-root");
        Files.createDirectories(root.resolve("bin"));
        Files.createFile(root.resolve("bin").resolve("java.exe"));

        assertTrue(EnvInstallerController.isValidLocalJdkRoot(root.toFile()));
    }

    @Test
    public void rejectsMavenRootWhenConfDirectoryIsMissing() throws IOException {
        Path root = Files.createTempDirectory("maven-root");
        Files.createDirectories(root.resolve("bin"));
        Files.createFile(root.resolve("bin").resolve("mvn.cmd"));

        assertFalse(EnvInstallerController.isValidLocalMavenRoot(root.toFile()));
    }

    @Test
    public void acceptsNodeRootWhenNodeExeExists() throws IOException {
        Path root = Files.createTempDirectory("node-root");
        Files.createFile(root.resolve("node.exe"));

        assertTrue(EnvInstallerController.isValidLocalNodeRoot(root.toFile()));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the static helper methods do not exist yet.

- [ ] **Step 3: Add minimal validation helpers to the controller**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`, add these static helpers near the bottom of the class:

```java
    static boolean isValidLocalJdkRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "bin" + File.separator + "java.exe").isFile();
    }

    static boolean isValidLocalMavenRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "bin" + File.separator + "mvn.cmd").isFile()
                && new File(root, "conf").isDirectory();
    }

    static boolean isValidLocalNodeRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "node.exe").isFile();
    }
```

Add a short comment immediately above them:

```java
    // Local import requires the user to point at the tool root so validation stays deterministic.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java
git commit -m "test: AI生成-补充本地环境目录校验"
```

### Task 3: Add The Local JDK Import Flow

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Test: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing JDK local import tests**

Append these tests to `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`:

```java
    @Test
    public void returnsSelectedJdkRootWhenItIsValid() throws IOException {
        Path root = Files.createTempDirectory("jdk-root");
        Files.createDirectories(root.resolve("bin"));
        Files.createFile(root.resolve("bin").resolve("java.exe"));

        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertEquals(root.toFile().getAbsolutePath(), controller.resolveLocalJdkHome(root.toFile()));
    }

    @Test
    public void rejectsSelectedJdkRootWhenJavaExeIsMissing() throws IOException {
        Path root = Files.createTempDirectory("jdk-root");
        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertNull(controller.resolveLocalJdkHome(root.toFile()));
    }
```

Also add this inner helper class:

```java
    private static final class TestableEnvInstallerController extends EnvInstallerController {
        private String resolveLocalJdkHome(File selectedDir) {
            return resolveLocalJdkHomeForTest(selectedDir);
        }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest#returnsSelectedJdkRootWhenItIsValid+EnvInstallerControllerTest#rejectsSelectedJdkRootWhenJavaExeIsMissing" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the controller does not expose a JDK local resolution method yet.

- [ ] **Step 3: Implement the minimal local JDK flow**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Add a new `@FXML private Button useLocalJdkButton;`
2. Add `public void onUseLocalJdk()`
3. Add a helper `String resolveLocalJdkHomeForTest(File selectedDir)`
4. Add a chooser helper `private File chooseLocalDirectory(String title)`
5. Add a helper `private void applyJdkEnvironment(String javaHome)`

Use this implementation shape:

```java
    public void onUseLocalJdk() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local JDK directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local JDK selection cancelled.");
            return;
        }

        String javaHome = resolveLocalJdkHomeForTest(selectedDir);
        if (javaHome == null) {
            LoggerUtil.info("Selected directory is not a valid JDK root. Missing bin\\java.exe.");
            return;
        }

        LoggerUtil.info("Local JDK root selected: " + javaHome);
        applyJdkEnvironment(javaHome);
    }

    String resolveLocalJdkHomeForTest(File selectedDir) {
        return isValidLocalJdkRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    private File chooseLocalDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        return chooser.showDialog(null);
    }
```

Refactor the existing no-arg `applyJdkEnvironment()` to delegate:

```java
    private void applyJdkEnvironment() {
        String javaHome = resolveInstalledJdkHome();
        if (javaHome == null || javaHome.isBlank()) {
            LoggerUtil.info("Please install JDK first.");
            setBusy(false);
            return;
        }

        applyJdkEnvironment(javaHome);
    }
```

Move the current `Task<EnvironmentSetupResult>` body into the new `private void applyJdkEnvironment(String javaHome)` method.

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java
git commit -m "feat: AI生成-支持本地JDK环境导入"
```

### Task 4: Add The Local Maven Import Flow

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Test: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing Maven local import tests**

Append these tests:

```java
    @Test
    public void returnsSelectedMavenRootWhenItIsValid() throws IOException {
        Path root = Files.createTempDirectory("maven-root");
        Files.createDirectories(root.resolve("bin"));
        Files.createDirectories(root.resolve("conf"));
        Files.createFile(root.resolve("bin").resolve("mvn.cmd"));

        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertEquals(root.toFile().getAbsolutePath(), controller.resolveLocalMavenHome(root.toFile()));
    }

    @Test
    public void rejectsSelectedMavenRootWhenMvnCmdIsMissing() throws IOException {
        Path root = Files.createTempDirectory("maven-root");
        Files.createDirectories(root.resolve("conf"));
        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertNull(controller.resolveLocalMavenHome(root.toFile()));
    }
```

Extend the helper class:

```java
        private String resolveLocalMavenHome(File selectedDir) {
            return resolveLocalMavenHomeForTest(selectedDir);
        }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the Maven local resolution method does not exist yet.

- [ ] **Step 3: Implement the minimal local Maven flow**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Add `@FXML private Button useLocalMavenButton;`
2. Add `public void onUseLocalMaven()`
3. Add `String resolveLocalMavenHomeForTest(File selectedDir)`
4. Add `private void applyMavenEnvironment(String mavenHome)`

Use this implementation shape:

```java
    public void onUseLocalMaven() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local Maven directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local Maven selection cancelled.");
            return;
        }

        String mavenHome = resolveLocalMavenHomeForTest(selectedDir);
        if (mavenHome == null) {
            LoggerUtil.info("Selected directory is not a valid Maven root. Missing bin\\mvn.cmd or conf directory.");
            return;
        }

        LoggerUtil.info("Local Maven root selected: " + mavenHome);
        applyMavenEnvironment(mavenHome);
    }

    String resolveLocalMavenHomeForTest(File selectedDir) {
        return isValidLocalMavenRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    private void applyMavenEnvironment(String mavenHome) {
        setBusy(true);

        Task<EnvironmentSetupResult> task = new Task<>() {
            @Override
            protected EnvironmentSetupResult call() throws Exception {
                return mavenEnvService.configureMavenEnvironment(mavenHome, mavenHome + "\\bin");
            }
        };

        task.setOnSucceeded(event -> {
            EnvironmentSetupResult result = task.getValue();
            if (result != null && result.isCompleted()) {
                LoggerUtil.info("Maven install completed.");
            } else {
                LoggerUtil.info("Maven files are installed, but environment setup is incomplete.");
            }
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set Maven environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-maven-env-thread").start();
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java
git commit -m "feat: AI生成-支持本地Maven环境导入"
```

### Task 5: Add The Local Node Import Flow

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Test: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing Node local import tests**

Append these tests:

```java
    @Test
    public void returnsSelectedNodeRootWhenItIsValid() throws IOException {
        Path root = Files.createTempDirectory("node-root");
        Files.createFile(root.resolve("node.exe"));

        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertEquals(root.toFile().getAbsolutePath(), controller.resolveLocalNodeHome(root.toFile()));
    }

    @Test
    public void rejectsSelectedNodeRootWhenNodeExeIsMissing() throws IOException {
        Path root = Files.createTempDirectory("node-root");
        TestableEnvInstallerController controller = new TestableEnvInstallerController();

        assertNull(controller.resolveLocalNodeHome(root.toFile()));
    }
```

Extend the helper class:

```java
        private String resolveLocalNodeHome(File selectedDir) {
            return resolveLocalNodeHomeForTest(selectedDir);
        }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the Node local resolution method does not exist yet.

- [ ] **Step 3: Implement the minimal local Node flow**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Add `@FXML private Button useLocalNodeButton;`
2. Add `public void onUseLocalNode()`
3. Add `String resolveLocalNodeHomeForTest(File selectedDir)`
4. Add `private void applyNodeEnvironment(String nodeHome)`

Use this implementation shape:

```java
    public void onUseLocalNode() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local Node directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local Node selection cancelled.");
            return;
        }

        String nodeHome = resolveLocalNodeHomeForTest(selectedDir);
        if (nodeHome == null) {
            LoggerUtil.info("Selected directory is not a valid Node root. Missing node.exe.");
            return;
        }

        LoggerUtil.info("Local Node root selected: " + nodeHome);
        applyNodeEnvironment(nodeHome);
    }

    String resolveLocalNodeHomeForTest(File selectedDir) {
        return isValidLocalNodeRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    private void applyNodeEnvironment(String nodeHome) {
        setBusy(true);

        Task<EnvironmentSetupResult> task = new Task<>() {
            @Override
            protected EnvironmentSetupResult call() throws Exception {
                return nodeEnvService.configureNodeEnvironment(nodeHome, "%NODE_HOME%");
            }
        };

        task.setOnSucceeded(event -> {
            EnvironmentSetupResult result = task.getValue();
            if (result != null && result.isCompleted()) {
                LoggerUtil.info("Node install completed.");
            } else {
                LoggerUtil.info("Node files are installed, but environment setup is incomplete.");
            }
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set Node environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-node-env-thread").start();
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java
git commit -m "feat: AI生成-支持本地Node环境导入"
```

### Task 6: Update Busy State Wiring And Final Regression Verification

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Test: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing busy-state coverage test**

Append this test:

```java
    @Test
    public void localImportButtonsFollowBusyState() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.installJdkButton = new Button();
        controller.installMavenButton = new Button();
        controller.installNodeButton = new Button();
        controller.useLocalJdkButton = new Button();
        controller.useLocalMavenButton = new Button();
        controller.useLocalNodeButton = new Button();
        controller.chooseInstallDirButton = new Button();
        controller.showConfigButton = new Button();
        controller.reloadSourcesButton = new Button();
        controller.saveSourcesButton = new Button();
        controller.jdkVersionCombo = new ComboBox<>();
        controller.mavenVersionCombo = new ComboBox<>();
        controller.nodeVersionCombo = new ComboBox<>();
        controller.jdkSourceField = new TextField("x");
        controller.mavenSourceField = new TextField("x");
        controller.nodeSourceField = new TextField("x");
        controller.installDirField = new TextField("D:\\tools");

        controller.forceBusyForTest(true);

        assertTrue(controller.useLocalJdkButton.isDisable());
        assertTrue(controller.useLocalMavenButton.isDisable());
        assertTrue(controller.useLocalNodeButton.isDisable());
        assertTrue(controller.showConfigButton.isDisable());
    }
```

Extend the helper class:

```java
        private void forceBusyForTest(boolean busy) {
            setBusyForTest(busy);
        }
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest#localImportButtonsFollowBusyState" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the local buttons and test hook are not fully wired into `refreshUiState()`.

- [ ] **Step 3: Implement the minimal busy-state wiring**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Make the existing FXML fields package-private instead of `private` so the controller test can assign them directly, or add package-private setters if preferred.
2. Add `@FXML Button useLocalJdkButton`, `useLocalMavenButton`, `useLocalNodeButton`.
3. In `refreshUiState()`, disable them with `busy`.
4. Add a package-private test hook:

```java
    void setBusyForTest(boolean busyState) {
        setBusy(busyState);
    }
```

Keep the production `setBusy(boolean busyState)` private.

- [ ] **Step 4: Run the focused controller test and full test suite**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest,EnvInstallerLayoutTest,DownloadProgressDialogControllerTest,EnvInstallerServiceTest,JdkEnvServiceTest,MavenEnvServiceTest,NodeEnvServiceTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS for the targeted regression suite.

Then run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn clean compile '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: BUILD SUCCESS.

Then run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: BUILD SUCCESS with all tests passing.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java src/main/resources/top/oneyi/envLauncher/env-installer.fxml
git commit -m "fix: AI生成-完善本地环境导入状态控制"
```

## Self-Review

- Spec coverage:
  - JDK/Maven/Node local root import paths are covered in Tasks 3, 4, 5.
  - `Show Config` moved to its own row is covered in Task 1.
  - Root validation, logging path, and busy-state consistency are covered in Tasks 2 and 6.
  - Existing download/install flows are protected by the Task 6 regression suite.
- Placeholder scan:
  - No `TODO`, `TBD`, or generic “handle appropriately” steps remain.
- Type consistency:
  - Helper names are consistent across tests and controller tasks: `resolveLocalJdkHomeForTest`, `resolveLocalMavenHomeForTest`, `resolveLocalNodeHomeForTest`, and `setBusyForTest`.
