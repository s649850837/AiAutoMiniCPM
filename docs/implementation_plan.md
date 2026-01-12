# MNN 原生集成计划

## 目标描述
实现 JNI 层以连接 Kotlin 的 `NativeChatEngine` 和 MNN LLM C++ 库。这将支持直接在设备上运行 MiniCPM 模型。

## 需要用户审查
 > [!IMPORTANT]
 > **MNN 库构建策略**：由于官方示例项目构建环境陈旧，我们采用直接 CMake 构建方式。
 > 
 > ### 核心构建步骤
 > 1. **清理环境**：确保没有旧的 Gradle构建残留。
 > 2. **直接 CMake 编译**：使用 Android NDK 提供的工具链直接编译 MNN 核心库。
 >    - 开启 LLM 支持 (`-DMNN_BUILD_LLM=ON`)
 >    - 开启 Transformer 融合 (`-DMNN_SUPPORT_TRANSFORMER_FUSE=ON`)
 >    - 开启 ARM8.2 FP16 支持 (`-DMNN_ARM82=ON`)
 >    - 目标架构：`arm64-v8a`
 > 3. **集成**：将生成的 `libMNN.so` 和头文件复制到 Android 项目中。
 > 
 > ### 预期产物
 > - `libMNN.so` (包含 LLM 功能的单体库，因 `MNN_SEP_BUILD=OFF`)
 > - `include/MNN/*.h` + `include/llm/*.hpp`

## 建议的更改

### 构建配置

#### [MODIFY] [CMakeLists.txt](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/src/main/cpp/CMakeLists.txt)
- 添加 MNN 头文件的 `include_directories`。
- 添加 `MNN` 和 `MNN_LLM` 的 `add_library` (IMPORTED SHARED)。
- 将 `aiautominitest` 链接到 `MNN` 和 `MNN_LLM`。

#### [MODIFY] [build.gradle.kts](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/build.gradle.kts)
- 确保如果 `jniLibs` 位于自定义位置，能被正确拾取；如果是标准位置 `src/main/jniLibs` 则通常无需更改。

### 原生实现 (Native)

#### [MODIFY] [native-lib.cpp](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/src/main/cpp/native-lib.cpp)
- **包含 (Includes)**：包含 `llm/LLM.hpp` 和 JNI 头文件。
- **全局/状态 (Global/State)**：维护一个指向 `MNN::LLM` 实例的全局或持久指针。
- **函数 (Functions)**：
    - `Java_com_example_aiautominitest_core_MNNLLMBridge_init`: 从模型路径初始化 LLM。
    - `Java_com_example_aiautominitest_core_MNNLLMBridge_chat`: 调用 `llm->response` 传入用户输入和历史记录。
        - 设置一个回调 lambda 以调用 Kotlin 中的 `onTokenGenerated`。
    - `Java_com_example_aiautominitest_core_MNNLLMBridge_stop`: 停止生成。

### Kotlin 辅助类

#### [MODIFY] [MNNLLMBridge.kt](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/src/main/java/com/example/aiautominitest/core/MNNLLMBridge.kt)
- 验证签名是否与 JNI 实现匹配。

## 验证计划

### 手动验证
1.  **构建**：运行 `./gradlew assembleDebug` 以确保 CMake 链接正确。
2.  **运行时**：启动应用，下载/选择一个模型，并发送消息。验证：
    - JNI `init` 返回 true。
    - Token 通过回调流式传回。
    - 旋转屏幕或快速输入时无崩溃。
