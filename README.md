# 项目介绍 - env-launcher

## 概述
`env-launcher` 是一个基于 JavaFX 开发的桌面应用程序，旨在为没有安装 Java 运行环境的用户提供便捷的运行体验。通过使用 `jpackage` 工具，该项目可以将应用及其所需的 JRE 打包成一个独立的可执行文件（如 [.exe](file://E:\Desktop\W\TestJava\env-launcher\target\app\bin\java.exe) 文件），用户无需额外安装 Java 环境即可直接运行。

## 技术栈
- **JavaFX**：用于构建图形用户界面（GUI）。
- **Maven**：作为项目的构建工具，管理依赖和构建流程。
- **JDK 17**：使用 JDK 17 进行开发和打包，确保兼容性和稳定性。

## 核心功能
- **一键打包**：通过 Maven 插件（如 `javafx:jink`）进行打包，并结合 `jpackage` 将应用与 JRE 一起打包成 [.exe](file://E:\Desktop\W\TestJava\env-launcher\target\app\bin\java.exe) 文件。
- **免安装运行**：生成的可执行文件包含了完整的运行时环境，用户无需手动安装 Java 环境即可运行程序。
- **注册表支持**：打包后的 [.exe](file://E:\Desktop\W\TestJava\env-launcher\target\app\bin\java.exe) 文件需要以管理员身份运行，以便正确地设置系统 PATH 环境变量并通过注册表进行相关配置。

## 打包说明
### 使用命令行打包
在项目根目录下打开命令行窗口，执行以下命令：
```shell
jpackage --name envLauncher --type app-image -m top.oneyi.jdktool/top.oneyi.jdktool.MainApp --runtime-image .\target\app\
```


#### 参数说明
- `--name`：指定输出的 [.exe](file://E:\Desktop\W\TestJava\env-launcher\target\app\bin\java.exe) 文件名。
- `--type app-image`：指定输出类型为应用程序镜像。
- `--runtime-image`：指定包含 JRE 的运行时环境路径。

### 注意事项
- **JDK 版本**：请确保使用 JDK 17 进行打包，否则可能会出现错误。切换 JDK 版本后，请重启 IDE（如 IntelliJ IDEA）。
- **管理员权限**：打包生成的 [.exe](file://E:\Desktop\W\TestJava\env-launcher\target\app\bin\java.exe) 文件需要以管理员身份运行，因为某些操作（如修改注册表和设置 PATH）需要管理员权限。

## 适用场景
- 需要将 Java 应用程序分发给没有安装 Java 环境的用户。
- 希望提供一个简单、独立的可执行文件，简化用户的安装和使用流程。

这个项目非常适合那些希望快速将 Java 应用部署到目标机器上的开发者，特别是针对非技术用户或企业内部使用的场景。
