package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import org.junit.Test;
import org.junit.BeforeClass;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class EnvInstallerServiceTest {

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
    public void writesLocalRepositoryWhenSettingsOnlyContainCommentedTemplate() throws Exception {
        Path mavenHome = Files.createTempDirectory("maven-home");
        Path confDir = Files.createDirectories(mavenHome.resolve("conf"));
        Path settingsFile = confDir.resolve("settings.xml");
        Files.writeString(settingsFile, defaultSettingsTemplate(), StandardCharsets.UTF_8);

        String updated = EnvInstallerService.updateMavenSettingsContent(
                Files.readString(settingsFile, StandardCharsets.UTF_8),
                mavenHome.toFile().getAbsolutePath()
        );
        String expectedRepo = new File(mavenHome.toFile(), "maven-repository").getAbsolutePath();

        assertTrue("settings.xml should contain installed Maven local repository path",
                updated.contains("<localRepository>" + expectedRepo + "</localRepository>"));
    }

    @Test
    public void replacesExistingLocalRepositoryWithInstalledMavenDirectory() throws Exception {
        Path mavenHome = Files.createTempDirectory("maven-home");
        Path confDir = Files.createDirectories(mavenHome.resolve("conf"));
        Path settingsFile = confDir.resolve("settings.xml");
        Files.writeString(settingsFile, settingsWithExistingRepository(), StandardCharsets.UTF_8);

        String updated = EnvInstallerService.updateMavenSettingsContent(
                Files.readString(settingsFile, StandardCharsets.UTF_8),
                mavenHome.toFile().getAbsolutePath()
        );
        String expectedRepo = new File(mavenHome.toFile(), "maven-repository").getAbsolutePath();

        assertTrue("settings.xml should replace existing local repository path",
                updated.contains("<localRepository>" + expectedRepo + "</localRepository>"));
        assertTrue("old local repository path should be removed",
                !updated.contains("<localRepository>C:\\custom\\repo</localRepository>"));
    }

    @Test
    public void removesDefaultHttpBlockerMirrorAndKeepsAliyunMirror() throws Exception {
        Path mavenHome = Files.createTempDirectory("maven-home");
        String updated = EnvInstallerService.updateMavenSettingsContent(
                settingsWithDefaultHttpBlocker(),
                mavenHome.toFile().getAbsolutePath()
        );

        assertFalse("default http blocker mirror should be removed",
                updated.contains("<id>maven-default-http-blocker</id>"));
        assertTrue("aliyun mirror should exist after cleanup",
                updated.contains("<id>aliyunmaven</id>"));
    }

    @Test
    public void reportsMavenInstallIncompleteWhenEnvironmentSetupIsCancelled() throws Exception {
        FakeMavenEnvService mavenEnvService = new FakeMavenEnvService();
        mavenEnvService.result = EnvironmentSetupResult.incomplete(
                "cancelled",
                true,
                "Administrator permission was cancelled."
        );
        EnvInstallerService service = new EnvInstallerService(mavenEnvService, new NodeEnvService());

        boolean success = service.applyMavenEnvironment("C:\\env\\apache-maven-3.9.10");

        assertFalse("installer should not report success when Maven environment setup is incomplete", success);
    }

    @Test
    public void reportsNodeInstallIncompleteWhenEnvironmentSetupIsCancelled() throws Exception {
        FakeNodeEnvService nodeEnvService = new FakeNodeEnvService();
        nodeEnvService.result = EnvironmentSetupResult.incomplete(
                "cancelled",
                true,
                "Administrator permission was cancelled."
        );
        EnvInstallerService service = new EnvInstallerService(new MavenEnvService(), nodeEnvService);

        boolean success = service.applyNodeEnvironment("C:\\env\\node-v20");

        assertFalse("installer should not report success when Node environment setup is incomplete", success);
    }

    private String defaultSettingsTemplate() {
        return "<settings>" + System.lineSeparator()
                + "  <!-- localRepository" + System.lineSeparator()
                + "  | The path to the local repository maven will use to store artifacts." + System.lineSeparator()
                + "  <localRepository>/path/to/local/repo</localRepository>" + System.lineSeparator()
                + "  -->" + System.lineSeparator()
                + "</settings>" + System.lineSeparator();
    }

    private String settingsWithExistingRepository() {
        return "<settings>" + System.lineSeparator()
                + "  <localRepository>C:\\custom\\repo</localRepository>" + System.lineSeparator()
                + "</settings>" + System.lineSeparator();
    }

    private String settingsWithDefaultHttpBlocker() {
        return "<settings>" + System.lineSeparator()
                + "  <mirrors>" + System.lineSeparator()
                + "    <mirror>" + System.lineSeparator()
                + "      <id>maven-default-http-blocker</id>" + System.lineSeparator()
                + "      <mirrorOf>external:http:*</mirrorOf>" + System.lineSeparator()
                + "      <name>Pseudo repository to mirror external repositories initially using HTTP.</name>" + System.lineSeparator()
                + "      <url>http://0.0.0.0/</url>" + System.lineSeparator()
                + "      <blocked>true</blocked>" + System.lineSeparator()
                + "    </mirror>" + System.lineSeparator()
                + "  </mirrors>" + System.lineSeparator()
                + "</settings>" + System.lineSeparator();
    }

    private static final class FakeMavenEnvService extends MavenEnvService {
        private EnvironmentSetupResult result = EnvironmentSetupResult.completed("completed", false, "done");

        @Override
        public EnvironmentSetupResult configureMavenEnvironment(String mavenHome, String mavenBinPath) {
            return result;
        }
    }

    private static final class FakeNodeEnvService extends NodeEnvService {
        private EnvironmentSetupResult result = EnvironmentSetupResult.completed("completed", false, "done");

        @Override
        public EnvironmentSetupResult configureNodeEnvironment(String nodeHome, String nodePathEntry) {
            return result;
        }
    }
}
