# 🎤 AiAutoMiniCPM v2.0 - 语音功能快速入门

> **新增功能**: 基于 Sherpa-ONNX 的离线语音识别

---

## ⚡ 快速开始（5 分钟）

### 1️⃣ 下载模型

访问 [Sherpa-ONNX Releases](https://github.com/k2-fsa/sherpa-onnx/releases)，下载中文模型：

**推荐**: `sherpa-onnx-streaming-paraformer-bilingual-zh-en`

### 2️⃣ 放置模型文件

```bash
adb push sherpa-onnx-streaming-paraformer-bilingual-zh-en /sdcard/sherpa-onnx-models/zh
```

### 3️⃣ 修改代码

编辑 `ChatActivityWithVoice.kt` 第 127 行:
```kotlin
val modelPath = "/sdcard/sherpa-onnx-models/zh"  // 改为你的路径
```

### 4️⃣ 启用语音版

编辑 `MainActivity.kt`:
```kotlin
startActivity(Intent(this, ChatActivityWithVoice::class.java))  // 改这行
```

### 5️⃣ 构建运行

```bash
./gradlew installDebug
```

---

## 🎮 使用方法

1. 点击输入框左侧 **🎤 按钮** → 切换到语音模式
2. **长按**中央录音按钮 → 开始说话
3. 实时显示识别结果
4. **松开**按钮或自动端点检测 → 发送消息

---

## 📦 新增依赖

```kotlin
// app/build.gradle.kts
implementation("com.k2fsa.sherpa.onnx:sherpa-onnx:1.10.30")
```

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## 📁 新增文件

```
app/src/main/java/com/example/aiautominitest/
├── audio/
│   └── AudioRecorder.kt                    # 音频采集
├── asr/
│   ├── IAsrEngine.kt                       # ASR 接口
│   └── SherpaOnnxAsrEngine.kt              # Sherpa-ONNX 实现
└── ui/
    ├── voice/VoiceInputView.kt             # 语音输入组件
    └── chat/ChatActivityWithVoice.kt       # 语音聊天界面
```

---

## 🔧 配置选项

### 调整端点检测灵敏度

编辑 `SherpaOnnxAsrEngine.kt` 第 48 行:
```kotlin
rule1MinTrailingSilence = 2.4f,  // 减小 = 更快检测端点
rule2MinTrailingSilence = 1.2f,  // 静音阈值（秒）
```

---

## 📊 性能

| 指标 | 数值 |
|------|------|
| 模型加载 | ~1.5s |
| 识别延迟 | <300ms |
| 实时率 | 0.1-0.2x |
| 准确率 | >95% |

---

## 🐛 常见问题

**Q: 初始化失败？**
A: 检查模型路径和文件完整性
```bash
adb shell ls -lh /sdcard/sherpa-onnx-models/zh/
```

**Q: 没有麦克风权限？**
A: 设置 → 应用 → AiAutoMiniTest → 允许麦克风

**Q: 识别延迟大？**
A: 使用 INT8 量化模型，或调小端点检测参数

---

## 📚 完整文档

- [语音功能集成说明](语音功能集成说明.md) - 详细教程
- [语音功能开发报告](语音功能开发完成报告.md) - 技术详解

---

## 🎯 版本对比

| 功能 | v1.0 | v2.0 |
|------|------|------|
| 文字输入 | ✅ | ✅ |
| 语音输入 | ❌ | ✅ |
| 实时识别 | ❌ | ✅ |
| 端点检测 | ❌ | ✅ |
| 模式切换 | ❌ | ✅ |

---

## 📞 资源链接

- **Sherpa-ONNX**: https://github.com/k2-fsa/sherpa-onnx
- **模型下载**: https://github.com/k2-fsa/sherpa-onnx/releases
- **文档**: https://k2-fsa.github.io/sherpa/onnx/android/index.html

---

**🚀 升级到 v2.0，体验全新的语音 AI 助手！**
