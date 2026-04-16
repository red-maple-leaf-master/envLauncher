# 本地环境导入设计

日期：2026-04-16

## 背景

当前界面只支持下载并安装 JDK、Maven、Node，然后再写入环境变量。
用户已经存在本地安装目录时，仍然必须走下载流程，不符合 Windows 环境安装器“可安装、可运行、可回滚”的最小操作原则。

本次设计目标是在不改变现有下载链路的前提下，为 JDK、Maven、Node 增加“使用本地已有目录”的入口，并将“查看环境配置”按钮从安装操作区域中拆分出来，降低误操作和界面混杂。

## 目标

1. 用户可以为 JDK、Maven、Node 分别选择本地已有安装目录，而不触发下载。
2. 目录选择完成后，程序只做本地目录校验和环境变量设置，不改写本地工具内容。
3. 现有下载并安装流程保持可用，不与本地导入路径互相干扰。
4. “查看环境配置”按钮单独占一行，不与安装和环境设置操作混排。

## 非目标

1. 不增加自动扫描本机已有安装目录的能力。
2. 不支持选择父目录后自动深度搜索多个子目录。
3. 不改动下载源策略和现有下载逻辑。
4. 不在本地导入时修改 Maven `settings.xml`、Node npm 配置等工具内部文件。

## 交互设计

### 安装区调整

保留现有 JDK、Maven、Node 的版本选择和下载安装按钮。

每个组件增加一个本地导入按钮：

1. `Install JDK` 旁新增 `Use Local JDK`
2. `Install Maven` 旁新增 `Use Local Maven`
3. `Install Node` 旁新增 `Use Local Node`

每个本地导入按钮点击后，都弹出 `DirectoryChooser`，要求用户直接选择工具根目录。

### 查看配置按钮调整

`Show Config` 按钮从当前混合操作区移出，放到单独一行。

这样界面分为：

1. 安装目录
2. 组件安装与本地导入
3. 配置查看
4. 下载源
5. 日志

## 本地目录选择规则

### JDK

用户必须选择 JDK 根目录，例如：

- `D:\environment\JDK\jdk-17.0.2`

校验规则：

1. 所选目录存在
2. 所选目录下存在 `bin\java.exe`

环境变量写入规则：

1. `JAVA_HOME=<所选目录>`
2. PATH 插入 `%JAVA_HOME%\bin`

### Maven

用户必须选择 Maven 根目录，例如：

- `D:\environment\apache-maven-3.9.10`

校验规则：

1. 所选目录存在
2. 所选目录下存在 `bin\mvn.cmd`
3. 所选目录下存在 `conf` 目录

环境变量写入规则：

1. `MAVEN_HOME=<所选目录>`
2. PATH 插入 `<所选目录>\bin`

边界：

1. 本地导入时不调用 `configureMavenSettings(...)`
2. 不创建本地 Maven 仓库目录

### Node

用户必须选择 Node 根目录，例如：

- `D:\environment\node-v20.19.2-win-x64`

校验规则：

1. 所选目录存在
2. 所选目录下存在 `node.exe`

环境变量写入规则：

1. `NODE_HOME=<所选目录>`
2. PATH 插入 `%NODE_HOME%`

边界：

1. 本地导入时不执行 npm 配置初始化
2. 不覆盖用户现有 npm 源、缓存或前缀目录

## 控制器与服务设计

### 控制器

在 `EnvInstallerController` 中新增三个入口方法：

1. `onUseLocalJdk()`
2. `onUseLocalMaven()`
3. `onUseLocalNode()`

职责：

1. 打开 `DirectoryChooser`
2. 校验用户选择目录是否合法
3. 合法时进入对应环境变量设置流程
4. 非法时输出明确日志
5. 全程保持与现有 `busy` 控制一致

### 服务复用原则

尽量复用现有环境变量写入服务：

1. JDK 继续复用 `JdkEnvService.configureJdkEnvironment(...)`
2. Maven 继续复用 `MavenEnvService.configureMavenEnvironment(...)`
3. Node 继续复用 `NodeEnvService.configureNodeEnvironment(...)`

不新增新的提权策略：

1. 目录选择和目录校验不提权
2. 仅在写系统环境变量时走现有管理员权限流程

### 目录校验实现

目录校验优先放在控制器或轻量辅助方法中，保持最小改动。

建议在 `EnvInstallerController` 中新增若干私有校验方法，或提取到轻量级 helper，但不做大规模服务拆分。

## 日志与错误处理

所有新路径必须补齐日志，至少覆盖：

1. 用户开始选择本地目录
2. 用户取消目录选择
3. 目录校验通过
4. 目录校验失败
5. 环境变量设置成功
6. 环境变量设置失败
7. 管理员授权取消

失败提示要求明确：

1. JDK：`Selected directory is not a valid JDK root. Missing bin\\java.exe.`
2. Maven：`Selected directory is not a valid Maven root. Missing bin\\mvn.cmd or conf directory.`
3. Node：`Selected directory is not a valid Node root. Missing node.exe.`

## UI 状态约束

本地导入流程与下载流程共用 `busy` 状态。

要求：

1. 本地目录校验失败后必须立即恢复可操作状态
2. 用户取消选择目录后必须立即恢复可操作状态
3. 环境变量写入结束后必须恢复可操作状态
4. `Show Config` 独立一行，但仍受 `busy` 控制禁用

## 测试方案

### 控制器行为测试

新增或扩展测试，覆盖：

1. 本地 JDK 目录合法时触发环境变量设置
2. 本地 Maven 目录合法时触发环境变量设置
3. 本地 Node 目录合法时触发环境变量设置
4. 本地目录非法时不触发环境变量设置
5. 用户取消目录选择时不触发环境变量设置

### 现有服务回归测试

确保以下路径不被破坏：

1. JDK 下载安装后写环境变量
2. Maven 下载安装后写环境变量
3. Node 下载安装后写环境变量
4. 提权取消时返回不完整结果
5. 环境刷新广播逻辑保持有效

### 手工验证

至少验证一条本地导入链路：

1. 选择已有 JDK 根目录并写入环境变量
2. 新开终端验证 `where java`
3. 验证 `java -version`

可选扩展验证：

1. 选择已有 Maven 根目录并验证 `mvn -version`
2. 选择已有 Node 根目录并验证 `node -v`

## 风险与取舍

### 风险

1. 用户可能选错目录层级
2. 本地工具目录可能不完整或被手工改坏
3. Maven 和 Node 的本地导入不再改写工具内部配置，行为会与下载安装路径略有差异

### 取舍

本次选择“只接管环境变量，不接管本地工具内容”。

原因：

1. 风险更低
2. 更符合最小改动原则
3. 避免覆盖用户已有工具配置
4. 更容易解释失败原因和回滚路径

## 实现范围

预计会修改：

1. `src/main/resources/top/oneyi/envLauncher/env-installer.fxml`
2. `src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
3. 可能补充少量测试文件

原则上不修改：

1. 下载源配置逻辑
2. 环境变量提权服务主流程
3. 工具下载与解压逻辑
