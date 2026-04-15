# 环境服务拆分实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 用 JDK、Maven、Node 三个组件服务加一个 Windows 命令服务，替换当前混合在一起的环境变量工具类。

**架构：** 保持现有安装流程不变，只迁移环境变量相关编排逻辑。组件服务分别负责 JDK、Maven、Node 的环境设置行为，Windows 命令服务负责 `setx`、注册表、PATH 与 PATH 读取等平台命令细节。

**技术栈：** Java 17、JavaFX、Maven、Windows 命令执行

---

### 任务 1：建立 Windows 命令边界

**文件：**
- 新建：`src/main/java/top/oneyi/envLauncher/service/WindowsEnvCommandService.java`
- 修改：`src/main/java/top/oneyi/envLauncher/utils/CmdUtil.java`

- [ ] 新增一个聚焦职责的服务，统一封装 PATH 读取、`setx`、注册表写入和 PATH 注册表更新。
- [ ] 把环境变量修改逻辑从组件层的直接命令细节中隔离出来。
- [ ] 如果仍需复用底层进程执行，可以保留 `CmdUtil` 作为低层辅助类。

### 任务 2：拆分 JDK 环境服务

**文件：**
- 新建：`src/main/java/top/oneyi/envLauncher/service/JdkEnvService.java`
- 修改：`src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`

- [ ] 将 JDK 环境读取和设置逻辑从 `EnvUtil` 迁移出去。
- [ ] 更新控制器，改为通过 `JdkEnvService` 完成 JDK 环境设置和读取。
- [ ] 保持现有日志和行为不变。

### 任务 3：拆分 Maven 环境服务

**文件：**
- 新建：`src/main/java/top/oneyi/envLauncher/service/MavenEnvService.java`
- 修改：`src/main/java/top/oneyi/envLauncher/controller/EnvInstallerController.java`
- 修改：`src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`

- [ ] 将 Maven 环境读取和设置逻辑从 `EnvUtil` 迁移出去。
- [ ] 更新安装服务和控制器调用方，改为使用 `MavenEnvService`。
- [ ] `settings.xml` 的 Maven 配置修改逻辑本次暂时保留在 `EnvInstallerService`。

### 任务 4：拆分 Node 环境服务

**文件：**
- 新建：`src/main/java/top/oneyi/envLauncher/service/NodeEnvService.java`
- 修改：`src/main/java/top/oneyi/envLauncher/service/EnvInstallerService.java`

- [ ] 将 Node 环境变量和 npm/cnpm 配置逻辑从 `EnvUtil` 迁移出去。
- [ ] 更新安装服务，改为通过 `NodeEnvService` 处理 Node 相关环境设置。
- [ ] 本次重构必须保持当前 Node 副作用行为不变。

### 任务 5：删除过时工具层

**文件：**
- 修改或删除：`src/main/java/top/oneyi/envLauncher/utils/EnvUtil.java`

- [ ] 从 `EnvUtil` 删除已迁移逻辑。
- [ ] 如果 `EnvUtil` 已无剩余职责，则直接删除。
- [ ] 确保项目中不再有调用方依赖旧工具类。

### 任务 6：验证与清理

**文件：**
- 如有必要，仅补充少量文档说明新的服务结构

- [ ] 执行 `mvn clean compile`
- [ ] 执行 `mvn test`
- [ ] 如果仍被当前 Java 8 / Java 17 构建环境问题阻塞，明确记录这个验证阻塞点
- [ ] 手工检查 JDK、Maven、Node 三条调用路径，确认都已分别走各自服务
