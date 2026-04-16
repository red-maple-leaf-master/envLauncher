package top.oneyi.envLauncher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class CmdUtil {

    public record ProcessResult(int exitCode, String output) {
    }

    /**
     * Execute a Windows cmd command and return merged standard/error output.
     */
    public static String executeCmdCommand(String command) throws IOException {
        return executeCommand(new String[]{"cmd", "/c", command}, Charset.forName("GBK"));
    }

    /**
     * Execute a raw process command and return merged standard/error output.
     */
    public static String executeCommand(String[] command, Charset charset) throws IOException {
        ProcessResult result = executeCommandForResult(command, charset);
        if (result.exitCode() != 0) {
            throw new IOException("Command exited with code " + result.exitCode() + ": " + result.output().trim());
        }
        return result.output();
    }

    /**
     * Execute a raw process command and capture both the merged output and exit code.
     */
    public static ProcessResult executeCommandForResult(String[] command, Charset charset) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        String output = readProcessOutput(process, charset);
        try {
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted.", e);
        }
    }

    private static String readProcessOutput(Process process, Charset charset) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), charset));
        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }

        while ((line = errorReader.readLine()) != null) {
            output.append("ERROR: ").append(line).append(System.lineSeparator());
        }

        reader.close();
        errorReader.close();

        return output.toString();
    }
}
