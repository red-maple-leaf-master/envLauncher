# AGENTS

## 目标
本项目是一个基于 JavaFX 的 Windows 环境安装器（JDK/Maven/Node）。所有改动都应优先保证“可安装、可运行、可回滚”。

## 硬性约束
1. Java 与构建版本固定为 JDK 17（见 `pom.xml`），不要在未明确要求时升级 Java 或核心依赖版本。
2. 构建工具固定为 Maven；默认以 `mvn clean compile` 和 `mvn test` 作为基础验证命令。
3. 模块系统必须保持可用：新增包/控制器后，若涉及 FXML 反射访问，必须同步更新 `src/main/java/module-info.java` 的 `opens`。
4. FXML、样式和图标路径必须保持资源可解析：`src/main/resources/top/oneyi/envLauncher/` 与 `src/main/resources/icons/` 下资源名不要随意改动。
5. 这是 Windows 优先项目：涉及环境变量与 PATH 的逻辑应兼容 `setx`/注册表行为，不要引入仅 Linux/macOS 可用命令。
6. 涉及下载、解压、环境变量写入的长任务必须放在后台线程/`Task` 中，UI 更新必须通过 `Platform.runLater(...)`。
7. 不要在代码中写死开发机绝对路径；路径拼接统一使用 `PathUtils` 或 `File`/`Path` API。
8. 未经明确需求，不要修改下载源策略（TUNA、npmmirror、Apache archive）和安装目录约定，避免影响现有用户环境。
9. 注释要求：新增或修改的关键逻辑必须有简洁注释，说明“为什么这样做”，避免无意义注释。
10. 日志要求：涉及下载、解压、环境变量、外部命令、异常分支的代码必须记录日志；成功、失败、取消三类状态都要可追踪。

## 变更边界
1. 优先做最小改动，避免“大重构+行为变化”同时发生。
2. 若修改 `EnvUtil`、`CmdUtil`、`EnvInstallerService` 这类系统相关代码，必须补充失败场景处理与日志。
3. 若新增 UI 交互，需保持窗口最小尺寸与主流程可用，不阻塞主线程。
4. 任何功能改动若缺少必要注释或关键日志，视为未完成，不可提交。

## 提交前检查
1. 编译通过：`mvn clean compile`
2. 测试通过：`mvn test`
3. 手工检查至少一条安装链路（JDK/Maven/Node 任一）不破坏现有流程
4. 提交记录要求是中文,符合提交规范格式要求,并且标明是AI生成
