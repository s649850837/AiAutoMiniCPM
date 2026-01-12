# AiAutoMiniCPM - Android AI 聊天应用

> 🤖 基于 MNN 引擎的本地大语言模型 Android 应用  
> 📱 使用 Kotlin + MVVM 架构 + JNI 原生集成

---

## 📋 项目简介

AiAutoMiniCPM 是一个 Android AI 聊天应用，集成了阿里巴巴开源的 MNN 深度学习推理引擎，可在 Android 设备上**本地运行**大语言模型（LLM），实现离线 AI 对话功能。

### 核心特性

- ✅ **本地推理**：无需联网，保护用户隐私
- ✅ **流式输出**：支持逐字显示 AI 回复（类似 ChatGPT）
- ✅ **MVVM 架构**：清晰的代码结构，易于维护和扩展
- ✅ **JNI 集成**：高性能的 C++ 原生层实现
- ✅ **模型管理**：支持动态下载和离线加载
- ✅ **协程支持**：使用 Kotlin Flow 处理异步数据流

---

## 🚀 快速开始

### 环境要求

- Android Studio 2022.1+
- NDK 25.1+
- Gradle 8.2+
- JDK 17
- 最低 Android 版本：API 26 (Android 8.0)

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd AiAutoMiniCPM

# 构建 APK
./gradlew assembleDebug

# 或在 Android Studio 中打开项目后点击 Run
```

### 准备模型文件

将 MiniCPM 模型文件放置在设备存储中：

```
/sdcard/MiniCPM/
├── config.json
├── lm.mnn
├── embeddings_bf16.mnn
└── tokenizer.json
```

---

## 📂 项目结构

```
AiAutoMiniCPM/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/                    # C++ JNI 代码
│   │   │   │   ├── include/            # MNN 头文件
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── native-lib.cpp      # JNI 实现
│   │   │   ├── jniLibs/                # 原生库
│   │   │   │   └── arm64-v8a/
│   │   │   │       └── libMNN.so       # MNN 引擎 (128 MB)
│   │   │   └── java/
│   │   │       └── com/example/aiautominitest/
│   │   │           ├── core/           # 核心引擎
│   │   │           │   ├── MNNLLMBridge.kt
│   │   │           │   └── NativeChatEngine.kt
│   │   │           ├── data/           # 数据层
│   │   │           ├── ui/             # UI 层
│   │   │           └── di/             # 依赖注入
│   └── build.gradle.kts
├── docs/                                # 📚 项目文档
│   ├── MNN_完整教程.md                  # MNN 集成详细教程
│   ├── 集成验证报告.md                   # 验证报告与总结
│   ├── implementation_plan.md          # 实施计划
│   └── PDF导出指南.md                   # 文档导出指南
├── MNN_Repo/                            # MNN 源码（构建后可选删除）
└── build_mnn_core.ps1                   # MNN 构建脚本
```

---

## 🔧 技术栈

### Android 层
- **语言**：Kotlin
- **架构**：MVVM
- **异步处理**：Kotlin Coroutines + Flow
- **依赖注入**：手动 DI (AppContainer)
- **数据库**：Room (用于聊天历史持久化)
- **下载管理**：FileDownloader

### 原生层
- **推理引擎**：MNN 3.3.1
- **JNI 语言**：C++17
- **构建工具**：CMake 3.22.1
- **目标架构**：ARM64-v8a

---

## 📚 文档索引

### 入门教程
- 📖 **[MNN 完整教程](docs/MNN_完整教程.md)** - 从零开始的完整集成指南（面向初学者）
  - MNN 基础知识
  - 构建 MNN 库
  - JNI 层实现
  - Kotlin 层封装
  - 故障排除

### 项目文档
- 📋 **[实施计划](docs/implementation_plan.md)** - 技术方案与架构设计
- 📊 **[集成验证报告](docs/集成验证报告.md)** - 构建结果与测试报告
- ✅ **[任务列表](docs/task.md)** - 开发进度追踪

### 工具指南
- 🔧 **[PDF 导出指南](docs/PDF导出指南.md)** - 将文档导出为 PDF

---

## 🎯 开发状态

### ✅ 已完成
- [x] MNN 引擎从源码构建（ARM64）
- [x] JNI 层实现（init、chat、stop）
- [x] Kotlin 协程封装
- [x] 流式输出回调机制
- [x] 项目构建配置

### 🚧 进行中
- [ ] UI 界面开发
- [ ] 模型下载功能
- [ ] 聊天历史持久化

### 📋 待开发
- [ ] 多轮对话优化
- [ ] 模型热切换
- [ ] 性能调优（量化、GPU 加速）

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- [MNN](https://github.com/alibaba/MNN) - 阿里巴巴开源的深度学习推理引擎
- [MiniCPM](https://github.com/OpenBMB/MiniCPM) - 面向移动设备的大语言模型
- [FileDownloader](https://github.com/lingochamp/FileDownloader) - 高性能文件下载库

---

## 📞 联系方式

有任何问题或建议，欢迎通过以下方式联系：

- 📧 Email: [your-email@example.com]
- 💬 Issues: [GitHub Issues](issues-url)

---

**⚡ Happy Coding!**
