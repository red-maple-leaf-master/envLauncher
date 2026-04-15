# env-launcher

Language: English | [简体中文](README.zh-CN.md)

## Overview
`env-launcher` is a Windows-oriented JavaFX desktop tool for fast setup of common dev runtimes: JDK, Maven, and Node.

It turns the full workflow into one visible flow:
- download
- unzip
- environment variable setup
- validation

## Core Features
- One-click flow: JDK -> Maven -> Node -> env setup
- Manual flow: run each setup step independently
- Download source management: view/edit/save sources in UI
- Observable progress: progress and logs for each task

## Tech Stack
- Java 17
- JavaFX 17
- Maven

## Project Structure
- `src/main/java`: application code
- `src/main/resources/top/oneyi/envLauncher`: FXML and styles
- `download-sources.properties`: local override config in project root (highest priority)

## Quick Start
### Requirements
- JDK 17
- Maven 3.8+

### Run
```bash
mvn clean javafx:run
```

### Package
```bash
mvn clean package
```

## Download Source Config
Two-level config is supported:
1. `download-sources.properties` in project root (preferred)
2. `src/main/resources/download-sources.properties` (default)

Key properties:
```properties
# Preferred JDK mode: dynamic template
jdk.url-template=https://api.adoptium.net/v3/binary/latest/{version}/ga/windows/x64/jdk/hotspot/normal/eclipse

# Fallback base URLs
jdk.base-url=https://mirrors.tuna.tsinghua.edu.cn/Adoptium/
maven.base-url=https://archive.apache.org/dist/maven/maven-3/
node.base-url=https://npmmirror.com/mirrors/node/
```

## Notes
- Run as administrator when setting system environment variables.
- Restart terminal/IDE after env variable updates.