# Portable Package Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增一个固定切换到 JDK 17 的 Windows 打包脚本，让用户可直接执行绿色版打包，不再受本机默认 JDK 8 干扰。

**Architecture:** 通过一个独立的 `package-portable.bat` 包装现有 Maven profile 打包命令，在脚本中显式设置 `JAVA_HOME` 和 `PATH`，保持打包入口职责清晰。

**Tech Stack:** Windows Batch, Maven, JDK 17, jpackage profile

---

## File Structure

- Create: `E:\wanyi\project\envLauncher\package-portable.bat`
  责任：固定切换 JDK 17、打印版本信息、执行绿色版打包、输出产物路径和失败提示。
- Modify: `E:\wanyi\project\envLauncher\README.md`
  责任：补充脚本化打包入口说明。
- Modify: `E:\wanyi\project\envLauncher\README.zh-CN.md`
  责任：补充中文脚本化打包入口说明。

### Task 1: 新增绿色版打包脚本

**Files:**
- Create: `E:\wanyi\project\envLauncher\package-portable.bat`

- [ ] **Step 1: 写脚本文件**

脚本内容：

```bat
@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME=D:\environment\JDK\jdk-17.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_REPO=C:\Users\yi.wan\.m2\repository"
set "OUTPUT_DIR=target\portable\env-launcher"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 17 not found: %JAVA_HOME%
    exit /b 1
)

echo [INFO] Using JAVA_HOME=%JAVA_HOME%
java -version
if errorlevel 1 exit /b 1

javac -version
if errorlevel 1 exit /b 1

mvn -version
if errorlevel 1 exit /b 1

echo [INFO] Building portable bundle...
mvn clean package -Pportable-app-image "-Dmaven.repo.local=%MAVEN_REPO%"
if errorlevel 1 (
    echo [ERROR] Portable package build failed.
    exit /b 1
)

if not exist "%OUTPUT_DIR%\env-launcher.exe" (
    echo [ERROR] Portable package build finished, but exe was not found: %OUTPUT_DIR%\env-launcher.exe
    exit /b 1
)

echo [INFO] Portable bundle created successfully.
echo [INFO] Output: %OUTPUT_DIR%
endlocal
```

- [ ] **Step 2: 运行脚本验证它会切到 JDK 17 并完成打包**

Run:

```powershell
.\package-portable.bat
```

Expected:

- 输出的 `java -version` 为 17
- 输出的 `mvn -version` 使用 JDK 17
- 绿色版打包成功

- [ ] **Step 3: 检查 exe 产物是否存在**

Run:

```powershell
Get-ChildItem target\portable\env-launcher\env-launcher.exe
```

Expected:

- `env-launcher.exe` 存在

- [ ] **Step 4: 提交脚本改动**

```bash
git add package-portable.bat
git commit -m "build: AI生成-新增绿色版打包脚本"
```

### Task 2: 更新文档入口说明

**Files:**
- Modify: `E:\wanyi\project\envLauncher\README.md`
- Modify: `E:\wanyi\project\envLauncher\README.zh-CN.md`

- [ ] **Step 1: 在英文 README 中补充脚本入口**

加入类似说明：

```md
You can also run the helper script on Windows:

```bat
package-portable.bat
```
```

- [ ] **Step 2: 在中文 README 中补充脚本入口**

加入类似说明：

```md
在 Windows 上也可以直接执行辅助脚本：

```bat
package-portable.bat
```
```

- [ ] **Step 3: 快速检查文档中已出现脚本名**

Run:

```powershell
Select-String -Path README.md,README.zh-CN.md -Pattern "package-portable.bat"
```

Expected:

- 两份 README 都能检索到 `package-portable.bat`

- [ ] **Step 4: 提交文档改动**

```bash
git add README.md README.zh-CN.md
git commit -m "docs: AI生成-补充绿色版打包脚本说明"
```

## Self-Review

- Spec coverage:
  - 独立脚本：Task 1
  - 固定 JDK 17：Task 1
  - 失败提示与产物检查：Task 1
  - 文档入口说明：Task 2
- Placeholder scan:
  - 已给出完整脚本内容、命令和验证方式
- Type consistency:
  - 统一使用 `package-portable.bat`、`target\portable\env-launcher`、`env-launcher.exe`
