package top.oneyi.envLauncher.utils;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

public class CmdUtilTest {

    @Test(expected = IOException.class)
    public void throwsWhenProcessExitsWithFailure() throws IOException {
        CmdUtil.executeCommand(
                new String[]{"cmd", "/c", "echo denied 1>&2 & exit /b 1"},
                Charset.forName("GBK")
        );
    }
}
