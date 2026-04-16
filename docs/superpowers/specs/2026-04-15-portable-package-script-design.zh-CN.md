# 绿色版打包脚本设计

## 背景

当前项目已经支持通过 Maven profile 生成带内置运行时的 Windows 绿色版目录，但实际使用时仍需要手动切换到 JDK 17，再执行较长的 Maven 命令。

用户侧已经出现过“本机默认是 JDK 8，导致构建失败”的情况，因此需要一个固定入口脚本，避免再次因为环境变量指向错误的 JDK 而打包失败。

## 目标

新增一个独立的 Windows 脚本 `package-portable.bat`，用于在本机固定切换到 JDK 17 后执行绿色版打包。

脚本目标：

- 固定使用 `D:\environment\JDK\jdk-17.0.2`
- 调用当前已经验证通过的 Maven 打包命令
- 在执行前输出当前 `java` 与 `mvn` 版本，便于排查环境问题
- 成功后提示绿色版产物目录

## 推荐方案

采用独立脚本 `package-portable.bat`。

推荐原因：

- 职责单一，只负责绿色版打包
- 对使用者最直接，双击或命令行运行都可
- 出问题时更容易定位是“脚本入口”还是“项目构建”

不采用的方案：

- 直接复用旧启动脚本：职责会混乱，不利于后续维护
- 同时重构旧启动脚本与新增脚本：超出当前最小改动范围

## 脚本行为设计

脚本执行流程：

1. 关闭回显并切换到脚本所在项目目录。
2. 设置 `JAVA_HOME=D:\environment\JDK\jdk-17.0.2`。
3. 将 `%JAVA_HOME%\bin` 插到当前进程 `PATH` 前面。
4. 输出 `java -version`、`javac -version`、`mvn -version`。
5. 执行：

```bat
mvn clean package -Pportable-app-image "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

6. 若命令失败，打印失败提示并返回非零退出码。
7. 若命令成功，打印产物目录：

```text
target\portable\env-launcher
```

## 错误处理

脚本应处理以下场景：

- JDK 17 路径不存在：直接报错并退出
- Maven 命令执行失败：保留 Maven 原始输出，并返回非零退出码
- 打包成功但产物目录不存在：提示检查 Maven 输出，避免误报完成

## 验证要求

实现完成后至少验证：

1. 双击或命令行运行 `package-portable.bat` 时，输出的 Java 版本为 17
2. 脚本能成功执行绿色版打包命令
3. 产物目录下存在 `target\portable\env-launcher\env-launcher.exe`
4. 产物目录与打包入口说明保持一致
