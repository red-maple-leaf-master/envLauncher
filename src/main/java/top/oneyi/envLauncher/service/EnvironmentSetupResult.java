package top.oneyi.envLauncher.service;

/**
 * Captures whether environment setup fully completed and how it ended.
 */
public final class EnvironmentSetupResult {
    private final String status;
    private final boolean completed;
    private final boolean usedElevation;
    private final String message;

    private EnvironmentSetupResult(String status, boolean completed, boolean usedElevation, String message) {
        this.status = status;
        this.completed = completed;
        this.usedElevation = usedElevation;
        this.message = message;
    }

    public static EnvironmentSetupResult completed(String status, boolean usedElevation, String message) {
        return new EnvironmentSetupResult(status, true, usedElevation, message);
    }

    public static EnvironmentSetupResult incomplete(String status, boolean usedElevation, String message) {
        return new EnvironmentSetupResult(status, false, usedElevation, message);
    }

    public String status() {
        return status;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean usedElevation() {
        return usedElevation;
    }

    public String message() {
        return message;
    }
}
