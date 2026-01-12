# 📄 MNN 教程导出 PDF 指南

## 方案 1：VSCode 插件（最简单）⭐⭐⭐

### 步骤 1：安装插件

1. 打开 VSCode
2. 按 `Ctrl + Shift + X` 打开扩展商店
3. 搜索并安装：**Markdown PDF**（作者：yzane）

### 步骤 2：导出 PDF

1. 在 VSCode 中打开文件：
   ```
   C:\Users\64985\.gemini\antigravity\brain\c5c106cb-4281-4062-941c-742227211d80\MNN_完整教程.md
   ```

2. 按 `Ctrl + Shift + P` 打开命令面板

3. 输入并选择：`Markdown PDF: Export (pdf)`

4. 等待几秒，PDF 会生成在同一目录下：
   ```
   MNN_完整教程.pdf
   ```

### 优点
✅ 一键操作，无需命令行
✅ 支持 Mermaid 图表
✅ 自动处理中文字体

---

## 方案 2：使用 Chrome 浏览器打印

### 步骤详解

1. **右键点击** `MNN_完整教程.md` → 选择"打开方式" → "Chrome"
   
   或者直接在浏览器中打开：
   ```
   file:///C:/Users/64985/.gemini/antigravity/brain/c5c106cb-4281-4062-941c-742227211d80/MNN_完整教程.md
   ```

2. 安装 **Markdown Viewer** 插件：
   - 访问：chrome://extensions/
   - 搜索"Markdown Viewer"
   - 安装并启用

3. 刷新页面，应该能看到格式化的文档

4. 按 `Ctrl + P` 打开打印对话框

5. 选择：
   - 目标：**另存为 PDF**
   - 纸张大小：A4
   - 边距：默认
   - 背景图形：✅ 勾选

6. 点击"保存"

### 优点
✅ 无需安装额外软件
✅ 预览效果所见即所得

---

## 方案 3：使用 Pandoc（专业级，需安装）

### 安装 Pandoc

```powershell
# Windows 使用 winget 安装
winget install JohnMacFarlane.Pandoc

# 或者手动下载
# https://pandoc.org/installing.html
```

### 安装 LaTeX（用于 PDF 生成）

```powershell
# 安装 MiKTeX（较小，推荐）
winget install MiKTeX.MiKTeX
```

### 导出 PDF

```powershell
# 进入教程目录
cd "C:\Users\64985\.gemini\antigravity\brain\c5c106cb-4281-4062-941c-742227211d80"

# 基础转换
pandoc MNN_完整教程.md -o MNN_完整教程.pdf

# 高级转换（带目录、代码高亮）
pandoc MNN_完整教程.md -o MNN_完整教程.pdf `
  --toc `
  --toc-depth=3 `
  --highlight-style=tango `
  --pdf-engine=xelatex `
  -V CJKmainfont="Microsoft YaHei" `
  -V geometry:margin=2cm
```

### 参数说明
- `--toc`：生成目录
- `--toc-depth=3`：目录深度到三级标题
- `--highlight-style=tango`：代码高亮主题
- `--pdf-engine=xelatex`：使用 XeLaTeX（支持中文）
- `-V CJKmainfont`：指定中文字体
- `-V geometry:margin=2cm`：设置页边距

### 优点
✅ 最专业的排版效果
✅ 完全自定义样式
✅ 支持复杂的 LaTeX 公式

---

## 🎨 自定义 PDF 样式（高级）

如果使用 Pandoc，可以创建自定义模板：

### 创建 custom.yaml

```yaml
---
title: "MNN LLM Android 集成完整教程"
author: "技术文档"
date: "2026-01-12"
documentclass: article
geometry: margin=2.5cm
mainfont: Arial
CJKmainfont: Microsoft YaHei
fontsize: 11pt
colorlinks: true
linkcolor: blue
urlcolor: blue
header-includes: |
  \usepackage{fancyhdr}
  \pagestyle{fancy}
  \fancyhead[L]{MNN 集成教程}
  \fancyhead[R]{\thepage}
---
```

### 使用模板导出

```powershell
pandoc MNN_完整教程.md -o MNN_完整教程.pdf `
  --metadata-file=custom.yaml `
  --pdf-engine=xelatex `
  --toc `
  --highlight-style=tango
```

---

## 📊 方案对比

| 方案 | 难度 | 效果 | 图表支持 | 速度 |
|------|------|------|----------|------|
| VSCode 插件 | ⭐ | ⭐⭐⭐ | ✅ Mermaid | ⚡⚡⚡ |
| Chrome 打印 | ⭐⭐ | ⭐⭐ | ⚠️ 需插件 | ⚡⚡ |
| Pandoc | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⚠️ 需配置 | ⚡ |

---

## 🚀 快速开始（推荐）

**如果您想立即导出，建议使用方案 1：**

1. 在 VSCode 中按 `Ctrl + Shift + X`
2. 搜索"Markdown PDF"并安装
3. 打开 `MNN_完整教程.md`
4. 按 `Ctrl + Shift + P` → 选择"Markdown PDF: Export (pdf)"
5. 完成！

生成的 PDF 文件路径：
```
C:\Users\64985\.gemini\antigravity\brain\c5c106cb-4281-4062-941c-742227211d80\MNN_完整教程.pdf
```

---

## ⚠️ 常见问题

### 问题 1：Mermaid 图表不显示

**解决方案（VSCode 插件）：**
- 确保安装的是 **Markdown PDF**（yzane 版本）
- 在设置中启用：`markdown-pdf.mermaid: true`

### 问题 2：中文显示为乱码

**解决方案（Pandoc）：**
- 必须使用 `--pdf-engine=xelatex`
- 指定中文字体：`-V CJKmainfont="Microsoft YaHei"`

### 问题 3：PDF 太大

**解决方案：**
- 使用 PDF 压缩工具（如 Adobe Acrobat、SmallPDF）
- 或在 Pandoc 中添加：`-V geometry:papersize=a4`

---

## 📞 需要帮助？

如果遇到问题，请告诉我：
1. 您选择了哪个方案
2. 具体的错误信息
3. 我会提供针对性的解决方案
