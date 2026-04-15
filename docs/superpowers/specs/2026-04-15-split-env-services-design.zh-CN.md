# 环境服务拆分设计

**目标**

将当前混合在一起的环境变量设置逻辑拆分为 JDK、Maven、Node 三个组件服务，同时把 Windows 命令执行细节隔离到专门的命令服务中。

**问题**

当前 `EnvUtil` 同时承担了三类职责：

- JDK 环境变量设置
- Maven 环境变量设置和 Maven 配置相关处理
- Node 环境变量设置以及 npm/cnpm 配置

这会让代码在修改时风险变高，因为组件逻辑和 Windows 平台命令细节耦合在一起。

**设计**

在 `src/main/java/top/oneyi/envLauncher/service/` 下新增四个聚焦职责的服务：

- `JdkEnvService`
  - 读取当前 JDK 环境信息
  - 应用 JDK 环境变量
- `MavenEnvService`
  - 读取当前 Maven 环境信息
  - 应用 Maven 环境变量
- `NodeEnvService`
  - 应用 Node 环境变量
  - 应用 npm 相关配置
- `WindowsEnvCommandService`
  - 执行 `setx`、注册表写入、PATH 更新和 PATH 读取
  - 将 Windows 平台命令细节从组件服务中隔离出去

**边界**

- `EnvInstallerController` 和 `EnvInstallerService` 改为调用新服务，而不是直接依赖 `EnvUtil`。
- `CmdUtil` 本次可以先保留，但所有与环境变量修改直接相关的 Windows 命令逻辑应迁移到 `WindowsEnvCommandService` 后面。
- `PathUtils` 不承担环境变量职责。
- `settings.xml` 的 Maven 配置修改逻辑本次仍可保留在 `EnvInstallerService`，因为它属于安装阶段配置，不属于通用环境变量编排。

**迁移步骤**

1. 新增 `WindowsEnvCommandService`，承接当前底层环境变量命令行为。
2. 将 JDK 相关逻辑从 `EnvUtil` 迁移到 `JdkEnvService`。
3. 将 Maven 相关逻辑从 `EnvUtil` 迁移到 `MavenEnvService`。
4. 将 Node 相关逻辑和 npm 配置迁移到 `NodeEnvService`。
5. 更新调用方，改为使用新服务。
6. 删除 `EnvUtil` 中已废弃逻辑；如果没有剩余职责，则删除 `EnvUtil`。

**非目标**

- 本次不改变现有环境变量行为本身。
- 本次不修改下载源、安装目录或 UI 流程。
- 本次不扩大为完整的命令执行框架重构。

**风险**

- 现有代码同时涉及系统级和用户级写入，本次重构必须先保持现有行为，再考虑后续修复。
- Node 逻辑耦合度最高，因为它还包含 npm 配置和 `cnpm` 安装。

**验证**

- 执行 `mvn clean compile`
- 执行 `mvn test`
- 手工验证至少一条安装链路仍能进入环境变量设置分支，且控制器/运行时没有报错
