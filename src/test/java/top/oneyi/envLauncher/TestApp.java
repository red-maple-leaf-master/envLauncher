package top.oneyi.envLauncher;


import org.junit.Test;

import java.io.IOException;


/**
 * @author W
 * @date 2025/6/19
 * @description 测试类
 */
public class TestApp {


    @Test
    public void  test() throws IOException {
        Process process2 = Runtime.getRuntime().exec("cmd /c echo %PATH%");

    }

}
