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

    private static final class TestableEnvInstallerController extends EnvInstallerController {
        private String resolveLocalJdkHome(File selectedDir) {
            return resolveLocalJdkHomeForTest(selectedDir);
        }

        private String resolveLocalMavenHome(File selectedDir) {
            return resolveLocalMavenHomeForTest(selectedDir);
        }

        private String resolveLocalNodeHome(File selectedDir) {
            return resolveLocalNodeHomeForTest(selectedDir);
        }

        private void forceBusyForTest(boolean busy) {
            setBusyForTest(busy);
        }
    }
}
