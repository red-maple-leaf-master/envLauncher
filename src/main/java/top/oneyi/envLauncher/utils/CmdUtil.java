package top.oneyi.envLauncher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class CmdUtil {

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
        Process process = Runtime.getRuntime().exec(command);
        return readProcessOutput(process, charset);
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
