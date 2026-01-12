# 任务列表：MNN 原生集成

- [ ] **设置项目结构/工件** <!-- id: 0 -->
    - [x] 分析当前状态
    - [ ] 创建实施计划
- [x] **MNN 库设置** <!-- id: 1 -->
    - [x] 配置 `CMakeLists.txt` 以链接 MNN 库
    - [x] 验证/添加 MNN 头文件 (`include`) (已从源码复制)
    - [x] 验证/添加 MNN `.so` 库 (arm64-v8a) (已从源码构建并复制)
- [x] **JNI 实现** <!-- id: 2 -->
    - [x] 在 `native-lib.cpp` 中实现 `init`
    - [x] 在 `native-lib.cpp` 中实现 `chat` (包含回调到 Kotlin)
    - [x] 在 `native-lib.cpp` 中实现 `stop`
    - [x] 处理 C++ 中的内存管理和线程
- [x] **Kotlin 集成** <!-- id: 3 -->
    - [x] 验证 `MNNLLMBridge` 签名是否与 JNI 匹配
    - [x] 测试 `NativeChatEngine` 流程
- [x] **验证** <!-- id: 4 -->
    - [x] 成功构建项目
    - [ ] 运行基本聊天测试 (需要模型文件)
