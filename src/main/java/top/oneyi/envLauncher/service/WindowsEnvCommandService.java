package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.CmdUtil;

import java.io.IOException;

/**
 * Centralizes Windows environment mutation commands so component services do not
 * need to know about setx or registry details.
 */
public class WindowsEnvCommandService {

    public String readPathContaining(String... keywords) throws IOException {
        return CmdUtil.getPathFromEnvironment(keywords);
    }

    public String setMachineEnvironmentVariable(String variableName, String variableValue) throws IOException {
        return CmdUtil.executeCmdCommand("setx " + variableName + " \"" + variableValue + "\" /M");
    }

    public String executeCommand(String command) throws IOException {
        return CmdUtil.executeCmdCommand(command);
    }

    public void setUserRegistryEnvironmentVariable(String variableName, String variableValue) throws Exception {
        CmdUtil.setHomeInWindows(variableValue, variableName);
    }

    public void updateMachinePath(String pathValue) throws IOException {
        CmdUtil.regAddPath(pathValue);
    }
}
