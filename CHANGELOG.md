# 更新日志

本项目的所有重大更改将记录在此处。

## [1.3] - 2026-02-14

### ✨ 新增 (Added)
- **Pixel Art Editor (像素画编辑器)**:
    - **全功能绘图引擎**: 支持铅笔、橡皮擦、油漆桶、取色器。
    - **Pixel Perfect 算法**: 自动优化线条，去除L型转角，使线条更平滑自然。
    - **形状工具**: 支持绘制直线、矩形、圆形（可填充）。
    - **图层管理**: 支持多图层编辑，包括添加、删除、隐藏、可见性切换。
    - **选区系统**:
        - **矩形选区**: 支持框选区域。
        - **魔棒工具 (Magic Wand)**: 支持按颜色容差自动选择连通区域。
        - **选区变换**: 支持选区内容的移动、90度旋转、水平/垂直翻转。
        - **浮动选区**: 选区内容可作为临时浮动层进行独立编辑。
    - **手势支持**: 支持画布无限画布、双指缩放/平移、放大镜精准取色。
    - **UI 优化**: 新增底部工具栏 (支持滑动)、工具设置面板 (笔刷大小、抖动开关等)。
- **FlowRow 优化**: 修复了 CreationScreen 中的布局崩溃问题，实现了自定义 `SimpleFlowRow`。

### 🐛 修复 (Fixed)
- 修复了编辑器工具栏在某些设备上显示不全的问题 (添加了横向滚动)。
- 修复了画布背景透明导致看起来像白屏的问题 (添加了白色背景和灰色边框)。
- 修复了 `PixelArtViewModel.kt` 中的构建错误 (括号不匹配、when 表达式未穷尽)。
- 修复了 `PixelArtEditorScreen` 中的潜在空指针崩溃。

## [1.2] - 2026-02-08

### ✨ 新增 (Added)
- **Pixel Art Pipeline (三步处理流水线)**:
    - **Smart Smoothing**: 集成桑原滤波器 (Kuwahara Filter)，实现“油画/卡通”风格平滑，减少噪点。
    - **Grid-based Downsampling**: 严格使用最近邻插值 (Nearest Neighbor) 进行像素化，保证边缘清晰。
    - **Color Quantization**: 集成 K-Means 聚类算法，支持“自动”调色板生成。
- **UI 实时进度**: 编辑器界面现在分步显示处理过程 (平滑 -> 像素化 -> 量化)。

## [1.1] - 2026-02-08

### ✨ 新增 (Added)
- **UI 重构**: 全新的 ImageToolbox 风格界面，支持 Edge-to-Edge 沉浸式显示。
- **预览区增强**:
    - 添加透明棋盘格背景 (`CheckerboardBackground`)。
    - `ZoomableImage`: 支持双指缩放和平移操作。
    - **长按对比**: 长按预览图可查看原图。
- **底部控制面板**:
    - 圆角卡片式设计，支持上下滑动。
    - 分组管理参数 (基础, 像素化, 调色板, 高级, 输出)。
- **触感反馈 (Haptics)**: 滑块操作、保存成功时提供振动反馈。
- **动态取色**: UI 控件颜色根据系统壁纸自动适配 (Material You)。
- **本地化**: 全面支持简体中文界面。
- **多格式导出**: 支持 PNG, JPG, WEBP 导出。
- **Pixel Perfect Upscale**: 支持整数倍无损放大导出，防止像素模糊。

### 🐛 修复 (Fixed)
- 修复了 `libs.versions.toml` 中 `material-icons-extended` 的版本依赖错误。

## [1.0] - ITIA (Initial Test Implementation Alpha)

### 🎉 初始发布
- 基础架构搭建 (Clean Architecture, MVVM)。
- 核心图像处理算法 (KotlinImageProcessor): 下采样 (Downsampling), 量化 (Quantization), 抖动 (Dithering)。
- 基础编辑器界面和仪表盘。
- 支持从相册导入图片和保存到本地。
