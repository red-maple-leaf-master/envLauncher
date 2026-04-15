# Portable Windows Runtime Bundle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `env-launcher` 产出一个带内置运行时的 Windows 绿色版目录，目标机器无须安装 JDK/JRE 也能直接双击 `.exe` 运行。

**Architecture:** 基于现有 `javafx-maven-plugin` 的模块化打包链路，优先使用 `jlink` 生成 app image 目录，而不是引入额外 exe 包装器或安装器。实现集中在 Maven 构建配置与文档说明，应用运行逻辑不改动。

**Tech Stack:** Java 17, JavaFX 17, Maven, javafx-maven-plugin, jlink

---

## File Structure

- Modify: `E:\wanyi\project\envLauncher\pom.xml`
  责任：补齐 JavaFX 打包配置，确保可以稳定生成带 runtime 的绿色目录和 Windows launcher。
- Modify: `E:\wanyi\project\envLauncher\README.md`
  责任：补充英文版开发构建、绿色版打包、最终用户运行说明。
- Modify: `E:\wanyi\project\envLauncher\README.zh-CN.md`
  责任：补充中文版开发构建、绿色版打包、最终用户运行说明。
- Verify output: `E:\wanyi\project\envLauncher\target\`
  责任：确认 `jlink` 生成的绿色目录中包含 `.exe` 与 runtime。

### Task 1: 固定 JavaFX 绿色版打包配置

**Files:**
- Modify: `E:\wanyi\project\envLauncher\pom.xml`

- [ ] **Step 1: 写一个失败验证，确认当前打包命令不能稳定产出目标绿色目录**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn clean javafx:jlink "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

Expected:

- 当前可能成功，但输出目录名、launcher 名称或 README 约定不稳定
- 这一步的目的不是制造编译失败，而是记录“当前配置尚未明确满足绿色版发布要求”

- [ ] **Step 2: 修改 Maven 打包配置，显式固定绿色版输出名称和 launcher 名称**

在 `pom.xml` 的 `javafx-maven-plugin` 中整理为类似下面的配置，保留现有 JDK 17/JavaFX 17 基线，不引入 `jpackage`：

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <executions>
        <execution>
            <id>default-cli</id>
            <configuration>
                <mainClass>top.oneyi.envLauncher.MainApp</mainClass>
                <launcher>env-launcher</launcher>
                <jlinkImageName>env-launcher</jlinkImageName>
                <jlinkZipName>env-launcher</jlinkZipName>
                <winLauncherType>gui</winLauncherType>
                <stripDebug>true</stripDebug>
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
            </configuration>
        </execution>
    </executions>
</plugin>
```

要求：

- 名称使用 `env-launcher`
- 不升级 Java 版本
- 不改动应用依赖行为

- [ ] **Step 3: 运行编译，确认基础构建未被破坏**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn clean compile "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 4: 运行 `jlink` 打包，确认绿色目录成功生成**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn clean javafx:jlink "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

Expected:

- `BUILD SUCCESS`
- `target\env-launcher\` 或插件实际输出的固定目录存在
- 目录中包含 `bin\env-launcher.exe`

- [ ] **Step 5: 提交构建配置改动**

```bash
git add pom.xml
git commit -m "build: AI生成-固定绿色版运行时打包配置"
```

### Task 2: 更新中英文文档，区分开发环境与最终用户运行环境

**Files:**
- Modify: `E:\wanyi\project\envLauncher\README.md`
- Modify: `E:\wanyi\project\envLauncher\README.zh-CN.md`

- [ ] **Step 1: 写一个失败检查，确认 README 仍然只描述通用打包**

Run:

```powershell
Select-String -Path README.md,README.zh-CN.md -Pattern "javafx:jlink|绿色版|portable|JRE|runtime"
```

Expected:

- 至少一部分关键说明不存在，说明文档尚未覆盖绿色版发布方式

- [ ] **Step 2: 在英文 README 中加入绿色版打包与运行说明**

将 `README.md` 的运行/打包部分调整为类似下面的内容：

```md
## Quick Start
### Requirements for Development
- JDK 17
- Maven 3.8+

### Run in Development
```bash
mvn clean javafx:run
```

### Build Portable Windows Bundle
```bash
mvn clean javafx:jlink
```

The generated portable bundle is under `target/env-launcher/` (or the configured jlink output directory).
End users can run `bin/env-launcher.exe` directly without installing JDK or JRE.
```
```

- [ ] **Step 3: 在中文 README 中加入绿色版打包与运行说明**

将 `README.zh-CN.md` 的对应部分调整为类似下面的内容：

```md
## 快速开始
### 开发环境要求
- JDK 17
- Maven 3.8+

### 开发运行
```bash
mvn clean javafx:run
```

### 打包绿色版目录
```bash
mvn clean javafx:jlink
```

生成后的绿色版目录位于 `target/env-launcher/`（或插件配置的输出目录）。
最终用户无需安装 JDK/JRE，直接双击 `bin/env-launcher.exe` 即可运行。
```
```

- [ ] **Step 4: 快速检查两份 README 都已覆盖绿色版说明**

Run:

```powershell
Select-String -Path README.md,README.zh-CN.md -Pattern "javafx:jlink|env-launcher.exe|JDK/JRE|绿色版|portable"
```

Expected:

- 两份文档都能匹配到打包命令和最终用户运行说明

- [ ] **Step 5: 提交文档改动**

```bash
git add README.md README.zh-CN.md
git commit -m "docs: AI生成-补充绿色版目录打包说明"
```

### Task 3: 验证绿色版目录可直接运行

**Files:**
- Verify: `E:\wanyi\project\envLauncher\target\env-launcher\`

- [ ] **Step 1: 运行全量测试，确认现有行为未回归**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn test "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 2: 重新生成绿色版目录**

Run:

```powershell
$env:JAVA_HOME='D:\environment\JDK\jdk-17.0.2'; $env:Path='D:\environment\JDK\jdk-17.0.2\bin;' + $env:Path; mvn clean javafx:jlink "-Dmaven.repo.local=C:\Users\yi.wan\.m2\repository"
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 3: 检查生成目录中是否存在 launcher 和 runtime**

Run:

```powershell
Get-ChildItem target\env-launcher
Get-ChildItem target\env-launcher\bin
Get-ChildItem target\env-launcher\lib
```

Expected:

- 根目录存在
- `bin` 目录存在
- `bin\env-launcher.exe` 存在
- runtime 相关目录或文件存在

- [ ] **Step 4: 从绿色版目录直接启动应用**

Run:

```powershell
Start-Process -FilePath ".\target\env-launcher\bin\env-launcher.exe"
```

Expected:

- 应用窗口成功打开
- 启动过程不依赖系统 `PATH` 中的 `java`

- [ ] **Step 5: 提交最终验证后的改动**

```bash
git add pom.xml README.md README.zh-CN.md
git commit -m "feat: AI生成-支持绿色版内置运行时目录打包"
```

## Self-Review

- Spec coverage:
  - 绿色版目录输出：Task 1, Task 3
  - 内置运行时与 launcher：Task 1, Task 3
  - 中英文说明：Task 2
  - 验证命令：Task 1, Task 3
- Placeholder scan:
  - 已给出明确文件、命令、期望结果和配置片段
- Type consistency:
  - 统一使用 `env-launcher` 作为 launcher 和 jlink image 名称
