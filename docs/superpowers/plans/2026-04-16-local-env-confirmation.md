# Local Env Confirmation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change local JDK/Maven/Node import into a two-step flow where selecting a local directory only caches and displays the path, and a separate apply action performs environment configuration.

**Architecture:** Keep the existing JDK/Maven/Node environment services unchanged and move the confirmation state into `EnvInstallerController`. The FXML will gain per-tool local path display fields and separate select/apply buttons, while controller logic will cache the chosen local root, enable apply only when a valid path exists, and reuse the current environment configuration tasks on explicit confirmation.

**Tech Stack:** JavaFX FXML, JavaFX controller/task APIs, existing `JdkEnvService` / `MavenEnvService` / `NodeEnvService`, JUnit 4

---

## File Map

- Modify: `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`
  - Replace the current one-click local import buttons with select/apply pairs and read-only local path fields.
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
  - Split local import into select and apply paths.
  - Cache per-tool selected local paths in UI fields.
  - Gate apply buttons on cached paths and `busy`.
- Modify: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`
  - Add tests for select-only behavior, apply gating, and busy-state wiring.

### Task 1: Reshape FXML For Two-Step Local Import

**Files:**
- Modify: `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`
- Test: `src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java`

- [ ] **Step 1: Write the failing layout test**

Append this test to `src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java`:

```java
    @Test
    public void envInstallerLayoutContainsLocalPathFieldsAndApplyButtons() throws IOException {
        String fxml = readEnvInstallerFxml();

        assertTrue(fxml.contains("fx:id=\"localJdkPathField\""));
        assertTrue(fxml.contains("fx:id=\"localMavenPathField\""));
        assertTrue(fxml.contains("fx:id=\"localNodePathField\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalJdkButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalJdkButton\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalMavenButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalMavenButton\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalNodeButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalNodeButton\""));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerLayoutTest#envInstallerLayoutContainsLocalPathFieldsAndApplyButtons" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the new ids do not exist yet.

- [ ] **Step 3: Update the FXML with read-only local path rows**

In `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`, replace each current local-import button row with a dedicated row like this:

```xml
                <HBox spacing="10" alignment="CENTER_LEFT">
                    <Button fx:id="selectLocalJdkButton" text="Select Local JDK" onAction="#onSelectLocalJdk" prefHeight="36" styleClass="action-button secondary"/>
                    <TextField fx:id="localJdkPathField" editable="false" prefHeight="36" styleClass="path-field" promptText="No local JDK selected" HBox.hgrow="ALWAYS"/>
                    <Button fx:id="applyLocalJdkButton" text="Apply Local JDK" onAction="#onApplyLocalJdk" prefHeight="36" styleClass="action-button primary"/>
                </HBox>
```

Add equivalent rows for Maven and Node:

```xml
                <HBox spacing="10" alignment="CENTER_LEFT">
                    <Button fx:id="selectLocalMavenButton" text="Select Local Maven" onAction="#onSelectLocalMaven" prefHeight="36" styleClass="action-button secondary"/>
                    <TextField fx:id="localMavenPathField" editable="false" prefHeight="36" styleClass="path-field" promptText="No local Maven selected" HBox.hgrow="ALWAYS"/>
                    <Button fx:id="applyLocalMavenButton" text="Apply Local Maven" onAction="#onApplyLocalMaven" prefHeight="36" styleClass="action-button primary"/>
                </HBox>
                <HBox spacing="10" alignment="CENTER_LEFT">
                    <Button fx:id="selectLocalNodeButton" text="Select Local Node" onAction="#onSelectLocalNode" prefHeight="36" styleClass="action-button secondary"/>
                    <TextField fx:id="localNodePathField" editable="false" prefHeight="36" styleClass="path-field" promptText="No local Node selected" HBox.hgrow="ALWAYS"/>
                    <Button fx:id="applyLocalNodeButton" text="Apply Local Node" onAction="#onApplyLocalNode" prefHeight="36" styleClass="action-button primary"/>
                </HBox>
```

Remove the old one-step ids `useLocalJdkButton`, `useLocalMavenButton`, and `useLocalNodeButton`.

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerLayoutTest#envInstallerLayoutContainsLocalPathFieldsAndApplyButtons" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/top/oneyi/envLauncher/env-installer.fxml src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java
git commit -m "feat: AI生成-补充本地环境二段式确认界面"
```

### Task 2: Split JDK Local Import Into Select And Apply

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Modify: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing JDK confirmation tests**

Append these tests to `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`:

```java
    @Test
    public void selectingLocalJdkOnlyStoresThePath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localJdkPathField = new TextField();

        controller.storeSelectedLocalJdkPath("D:\\tools\\jdk-17");

        assertEquals("D:\\tools\\jdk-17", controller.localJdkPathField.getText());
        assertFalse(controller.jdkApplyTriggered);
    }

    @Test
    public void applyLocalJdkRequiresSelectedPath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localJdkPathField = new TextField();

        assertFalse(controller.canApplyLocalJdk());
    }
```

Extend the helper class:

```java
        private boolean jdkApplyTriggered;

        private void storeSelectedLocalJdkPath(String path) {
            setSelectedLocalJdkPathForTest(path);
        }

        private boolean canApplyLocalJdk() {
            return canApplyLocalJdkForTest();
        }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because the controller has no cached-path helpers yet.

- [ ] **Step 3: Implement the minimal JDK select/apply split**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Replace `useLocalJdkButton` with:

```java
    @FXML Button selectLocalJdkButton;
    @FXML Button applyLocalJdkButton;
    @FXML TextField localJdkPathField;
```

2. Replace `onUseLocalJdk()` with:

```java
    public void onSelectLocalJdk() {
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

        setSelectedLocalJdkPathForTest(javaHome);
        LoggerUtil.info("Local JDK root selected and pending apply: " + javaHome);
        refreshUiState();
    }

    public void onApplyLocalJdk() {
        if (!canApplyLocalJdkForTest()) {
            LoggerUtil.info("Please select a local JDK directory first.");
            return;
        }

        LoggerUtil.info("Applying local JDK environment...");
        applyJdkEnvironment(safeTrim(localJdkPathField.getText()));
    }
```

3. Add package-private helpers:

```java
    void setSelectedLocalJdkPathForTest(String path) {
        localJdkPathField.setText(safeTrim(path));
    }

    boolean canApplyLocalJdkForTest() {
        return !safeTrim(localJdkPathField.getText()).isBlank();
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
git commit -m "feat: AI生成-拆分本地JDK选择与应用"
```

### Task 3: Split Maven And Node Local Import Into Select And Apply

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Modify: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing Maven and Node confirmation tests**

Append these tests:

```java
    @Test
    public void selectingLocalMavenOnlyStoresThePath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localMavenPathField = new TextField();

        controller.storeSelectedLocalMavenPath("D:\\tools\\apache-maven-3.9.10");

        assertEquals("D:\\tools\\apache-maven-3.9.10", controller.localMavenPathField.getText());
    }

    @Test
    public void selectingLocalNodeOnlyStoresThePath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localNodePathField = new TextField();

        controller.storeSelectedLocalNodePath("D:\\tools\\node-v20");

        assertEquals("D:\\tools\\node-v20", controller.localNodePathField.getText());
    }
```

Extend the helper class:

```java
        private void storeSelectedLocalMavenPath(String path) {
            setSelectedLocalMavenPathForTest(path);
        }

        private void storeSelectedLocalNodePath(String path) {
            setSelectedLocalNodePathForTest(path);
        }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because Maven/Node selected-path helpers do not exist yet.

- [ ] **Step 3: Implement the minimal Maven and Node select/apply split**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Replace `useLocalMavenButton` and `useLocalNodeButton` with:

```java
    @FXML Button selectLocalMavenButton;
    @FXML Button applyLocalMavenButton;
    @FXML TextField localMavenPathField;
    @FXML Button selectLocalNodeButton;
    @FXML Button applyLocalNodeButton;
    @FXML TextField localNodePathField;
```

2. Replace the current one-step methods with:

```java
    public void onSelectLocalMaven() { ... setSelectedLocalMavenPathForTest(mavenHome); ... }
    public void onApplyLocalMaven() { ... applyMavenEnvironment(safeTrim(localMavenPathField.getText())); }
    public void onSelectLocalNode() { ... setSelectedLocalNodePathForTest(nodeHome); ... }
    public void onApplyLocalNode() { ... applyNodeEnvironment(safeTrim(localNodePathField.getText())); }
```

3. Add package-private helpers:

```java
    void setSelectedLocalMavenPathForTest(String path) {
        localMavenPathField.setText(safeTrim(path));
    }

    void setSelectedLocalNodePathForTest(String path) {
        localNodePathField.setText(safeTrim(path));
    }

    boolean canApplyLocalMavenForTest() {
        return !safeTrim(localMavenPathField.getText()).isBlank();
    }

    boolean canApplyLocalNodeForTest() {
        return !safeTrim(localNodePathField.getText()).isBlank();
    }
```

Apply methods should refuse empty paths and log:

```java
        LoggerUtil.info("Please select a local Maven directory first.");
        LoggerUtil.info("Please select a local Node directory first.");
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
git commit -m "feat: AI生成-拆分本地Maven与Node选择和应用"
```

### Task 4: Wire Button State And Run Full Verification

**Files:**
- Modify: `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- Modify: `src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java`

- [ ] **Step 1: Write the failing apply-button state test**

Append this test:

```java
    @Test
    public void applyButtonsDependOnCachedLocalPathsAndBusyState() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.selectLocalJdkButton = new Button();
        controller.applyLocalJdkButton = new Button();
        controller.selectLocalMavenButton = new Button();
        controller.applyLocalMavenButton = new Button();
        controller.selectLocalNodeButton = new Button();
        controller.applyLocalNodeButton = new Button();
        controller.installJdkButton = new Button();
        controller.installMavenButton = new Button();
        controller.installNodeButton = new Button();
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
        controller.localJdkPathField = new TextField();
        controller.localMavenPathField = new TextField();
        controller.localNodePathField = new TextField();
        controller.installDirField = new TextField("D:\\tools");

        controller.refreshUiStateForTest();

        assertTrue(controller.applyLocalJdkButton.isDisable());
        assertTrue(controller.applyLocalMavenButton.isDisable());
        assertTrue(controller.applyLocalNodeButton.isDisable());

        controller.setSelectedLocalJdkPathForTest("D:\\tools\\jdk-17");
        controller.setSelectedLocalMavenPathForTest("D:\\tools\\apache-maven-3.9.10");
        controller.setSelectedLocalNodePathForTest("D:\\tools\\node-v20");
        controller.refreshUiStateForTest();

        assertFalse(controller.applyLocalJdkButton.isDisable());
        assertFalse(controller.applyLocalMavenButton.isDisable());
        assertFalse(controller.applyLocalNodeButton.isDisable());

        controller.forceBusyForTest(true);

        assertTrue(controller.selectLocalJdkButton.isDisable());
        assertTrue(controller.applyLocalJdkButton.isDisable());
        assertTrue(controller.selectLocalMavenButton.isDisable());
        assertTrue(controller.applyLocalMavenButton.isDisable());
        assertTrue(controller.selectLocalNodeButton.isDisable());
        assertTrue(controller.applyLocalNodeButton.isDisable());
    }
```

Extend the helper class:

```java
        private void refreshUiStateForTest() {
            refreshUiStateForTestHook();
        }
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest#applyButtonsDependOnCachedLocalPathsAndBusyState" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: FAIL because apply-button state is not yet tied to cached local paths.

- [ ] **Step 3: Implement the minimal state wiring**

In `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`:

1. Add a package-private hook:

```java
    void refreshUiStateForTestHook() {
        refreshUiState();
    }
```

2. Update `refreshUiState()` to enforce:

```java
        selectLocalJdkButton.setDisable(busy);
        applyLocalJdkButton.setDisable(busy || !canApplyLocalJdkForTest());
        selectLocalMavenButton.setDisable(busy);
        applyLocalMavenButton.setDisable(busy || !canApplyLocalMavenForTest());
        selectLocalNodeButton.setDisable(busy);
        applyLocalNodeButton.setDisable(busy || !canApplyLocalNodeForTest());
```

3. Keep the existing install buttons and `Show Config` behavior unchanged.

- [ ] **Step 4: Run focused regression tests plus full verification**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn "-Dtest=EnvInstallerControllerTest,EnvInstallerLayoutTest,DownloadProgressDialogControllerTest,EnvInstallerServiceTest,JdkEnvServiceTest,MavenEnvServiceTest,NodeEnvServiceTest" test '-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository'
```

Expected: BUILD SUCCESS.

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
git add src/main/resources/top/oneyi/envLauncher/env-installer.fxml src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java src/test/java/top/oneyi/envLauncher/EnvInstallerLayoutTest.java src/test/java/top/oneyi/envLauncher/controller/EnvInstallerControllerTest.java
git commit -m "fix: AI生成-完善本地环境导入确认交互"
```

## Self-Review

- Spec coverage:
  - Two-step local path selection and explicit apply actions are covered by Tasks 2 and 3.
  - Read-only local path display fields are covered by Task 1.
  - Apply button gating and `busy` interaction are covered by Task 4.
  - Existing download/install flows remain unchanged and are protected by the Task 4 regression suite.
- Placeholder scan:
  - No `TODO`, `TBD`, or generic “handle appropriately” steps remain.
- Type consistency:
  - Helper and field names remain consistent across plan tasks: `selectLocalJdkButton`, `applyLocalJdkButton`, `localJdkPathField`, corresponding Maven/Node variants, and `refreshUiStateForTestHook`.
