package top.oneyi.envLauncher.controller;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnvInstallerControllerTest {

    @BeforeClass
    public static void initToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit may already be initialized by another test.
        }
    }

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

    @Test
    public void selectingLocalMavenOnlyStoresThePath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localMavenPathField = new TextField();

        controller.storeSelectedLocalMavenPath("D:\\tools\\apache-maven-3.9.10");

        assertEquals("D:\\tools\\apache-maven-3.9.10", controller.localMavenPathField.getText());
        assertFalse(controller.mavenApplyTriggered);
    }

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

    @Test
    public void selectingLocalNodeOnlyStoresThePath() {
        TestableEnvInstallerController controller = new TestableEnvInstallerController();
        controller.localNodePathField = new TextField();

        controller.storeSelectedLocalNodePath("D:\\tools\\node-v20");

        assertEquals("D:\\tools\\node-v20", controller.localNodePathField.getText());
        assertFalse(controller.nodeApplyTriggered);
    }

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

        controller.storeSelectedLocalJdkPath("D:\\tools\\jdk-17");
        controller.storeSelectedLocalMavenPath("D:\\tools\\apache-maven-3.9.10");
        controller.storeSelectedLocalNodePath("D:\\tools\\node-v20");
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
        assertTrue(controller.showConfigButton.isDisable());
    }

    private static final class TestableEnvInstallerController extends EnvInstallerController {
        private boolean jdkApplyTriggered;
        private boolean mavenApplyTriggered;
        private boolean nodeApplyTriggered;

        private String resolveLocalJdkHome(File selectedDir) {
            return resolveLocalJdkHomeForTest(selectedDir);
        }

        private void storeSelectedLocalJdkPath(String path) {
            setSelectedLocalJdkPathForTest(path);
        }

        private boolean canApplyLocalJdk() {
            return canApplyLocalJdkForTest();
        }

        private String resolveLocalMavenHome(File selectedDir) {
            return resolveLocalMavenHomeForTest(selectedDir);
        }

        private void storeSelectedLocalMavenPath(String path) {
            setSelectedLocalMavenPathForTest(path);
        }

        private String resolveLocalNodeHome(File selectedDir) {
            return resolveLocalNodeHomeForTest(selectedDir);
        }

        private void storeSelectedLocalNodePath(String path) {
            setSelectedLocalNodePathForTest(path);
        }

        private void refreshUiStateForTest() {
            refreshUiStateForTestHook();
        }

        private void forceBusyForTest(boolean busy) {
            setBusyForTest(busy);
        }
    }
}
