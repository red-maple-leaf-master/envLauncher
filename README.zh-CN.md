# env-launcher

语言: [English](README.md) | 简体中文

## 项目介绍
`env-launcher` 是一个面向 Windows 的 JavaFX 桌面工具，用于快速安装和配置常用开发环境：JDK、Maven、Node。

它把完整流程串成可视化步骤：
- 下载
- 解压
- 环境变量配置
- 结果验证

## 核心能力
- 一键流程：JDK -> Maven -> Node -> 环境变量配置
- 手动流程：支持单独执行任意步骤
- 下载源管理：支持在 UI 中查看、编辑、保存下载源
- 过程可观察：任务进度与日志可追踪

## 技术栈
- Java 17
- JavaFX 17
- Maven

## 目录说明
- `src/main/java`：应用核心代码
- `src/main/resources/top/oneyi/envLauncher`：FXML 与样式资源
- `download-sources.properties`：项目根目录下载源覆盖配置，优先级最高

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
mvn clean package -Pportable-app-image
```

生成后的绿色版目录位于 `target/portable/env-launcher/`。
最终用户无需安装 JDK/JRE，直接运行 `target/portable/env-launcher/env-launcher.exe` 即可。

## 下载源配置
支持两层配置：
1. 项目根目录 `download-sources.properties`（推荐）
2. `src/main/resources/download-sources.properties`（默认）

关键配置项：
```properties
# 推荐：JDK 动态模板，按大版本获取最新可用包
jdk.url-template=https://api.adoptium.net/v3/binary/latest/{version}/ga/windows/x64/jdk/hotspot/normal/eclipse

# 兜底基础源
jdk.base-url=https://mirrors.tuna.tsinghua.edu.cn/Adoptium/
maven.base-url=https://archive.apache.org/dist/maven/maven-3/
node.base-url=https://npmmirror.com/mirrors/node/
```

## 注意事项
- 涉及系统环境变量写入时，建议使用管理员权限运行。
- 更新环境变量后，请重启终端或 IDE。
