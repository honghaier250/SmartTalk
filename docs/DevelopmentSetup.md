# 开发环境配置指南

本文档详细说明了SmartTalk（智言）输入法项目的开发环境配置步骤。

## 基本要求

- Android Studio Iguana | 2023.2.1 或更高版本
- JDK 17
- Gradle 8.4
- Android SDK 35

## Gradle配置步骤

由于本项目需要手动配置Gradle环境，请按照以下步骤进行设置：

### 1. 下载Gradle

1. 访问Gradle官网：https://gradle.org/releases/
2. 下载Gradle 8.4 版本的二进制发行版（Binary-only）
3. 将下载的zip文件解压到本地目录（建议放在一个固定的位置，如 `D:\Development\gradle-8.4`）

### 2. 配置Android Studio

1. 打开Android Studio
2. 进入设置页面：
   - Windows/Linux：File → Settings
   - macOS：Android Studio → Preferences
3. 导航到：Build, Execution, Deployment → Gradle
4. 在"Gradle Distribution"部分：
   - 选择"Local installation"
   - 点击"Browse"按钮
   - 选择之前解压的Gradle目录（如 `D:\Development\gradle-8.4`）
5. 点击"Apply"保存设置
6. 点击"OK"关闭设置窗口

### 3. 项目初始化

首次打开项目时，需要进行以下步骤：

1. 关闭Android Studio的自动Gradle同步（Auto Import）
2. 打开项目后，等待Android Studio初始化完成
3. 如果提示"Gradle project sync failed"，这是正常的，因为我们需要手动配置Gradle
4. 确保已经按照上述步骤正确配置了Gradle本地安装路径
5. 点击Android Studio工具栏的"Sync Project with Gradle Files"按钮进行同步
6. 等待项目同步完成，如果出现错误，请参考下方的常见问题部分

## 常见问题

### Gradle同步失败
- 确保网络连接正常
- 检查Gradle目录路径是否正确
- 确认JDK版本是否符合要求（JDK 17）
- 确保Android Studio使用的是本地安装的Gradle 8.4

### 构建错误
- 清理项目：Build → Clean Project
- 清理Gradle缓存：File → Invalidate Caches / Restart
- 检查build.gradle文件中的依赖是否正确

## 注意事项

- 请不要使用Android Studio的自动Gradle配置
- 确保使用正确的Gradle版本（8.4）
- 首次配置时可能需要多次尝试Gradle同步
- 如果同步失败，请检查Android Studio的Event Log窗口查看具体错误信息

## 相关文件

- `build.gradle`：项目根目录和app模块的构建配置文件
- `settings.gradle`：项目设置文件
- `gradle.properties`：Gradle属性配置文件



---

> 提示：
> 1. 请确保在开始开发之前完成所有配置步骤
> 2. 如果遇到构建问题，优先检查Gradle配置是否正确
> 3. 本项目使用手动Gradle配置，这与一般Android项目的自动配置方式不同
> 4. 如有任何问题，请及时反馈 