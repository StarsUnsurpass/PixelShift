# 更新日志

本项目的所有重大更改将记录在此处。

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
