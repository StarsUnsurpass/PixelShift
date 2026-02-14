# PixelShift 像素转换

PixelShift 是一个现代化的 Android 应用程序，用于将高分辨率图像转换为复古的 8-bit / 像素风格艺术作品。

## 🎯 功能特性

    *   **⚡ 实时预览**: 通过高效的算法实时查看像素化效果。
    *   **🎨 丰富的调色板**: 内置 GameBoy, NES (红白机), CGA, 黑白等经典复古调色板。
    *   **🎛️ 高级控制**:
        *   **像素大小**: 自由调节马赛克颗粒大小 (1px - 100px)。
        *   **抖动算法**: 支持 Floyd-Steinberg, Bayer, Atkinson 等多种抖动算法，还原复古质感。
        *   **对比度调节**: 增强画面层次感。
    *   **🖌️ 像素画编辑器 (Pixel Art Editor)**:
        *   **多图层系统**: 支持添加、删除、隐藏、调整透明度及图层混合。
        *   **全功能工具箱**: 铅笔 (支持Pixel Perfect), 橡皮擦, 油漆桶 (连续/全局), 取色器 (跨图层采样)。
        *   **形状工具**: 直线, 矩形, 圆形 (支持填充)。
        *   **选区工具**: 矩形选区, 魔棒 (Magic Wand), 支持选区移动、旋转、翻转。
        *   **手势操作**: 双指缩放画布，单指绘画/拖动。
    *   **🖼️ 沉浸式体验**: 采用 Edge-to-Edge 全屏设计，纯净的预览区域。
    *   **👆 便捷交互**:
        *   双指缩放/平移查看细节。
        *   **长按对比**: 长按图片快速查看原图，松开恢复预览。
        *   触感反馈 (Haptics) 提供细腻的操作手感。
    *   **💾 高清导出**:
        *   支持 PNG, JPG, WEBP 格式。
        *   **Pixel Perfect Upscale**: 支持按整数倍无损放大导出，确保像素边缘清晰锐利。

## 🛠️ 技术栈 (Modern Android Development)

*   **语言**: Kotlin (100%)
*   **UI 框架**: Jetpack Compose (Material Design 3)
*   **架构**: MVVM + Clean Architecture (Domain, Data, UI Layers)
*   **依赖注入**: Manual DI (AppModule)
*   **图片加载**: Coil
*   **异步处理**: Kotlin Coroutines & Flow

## 🚀 快速开始

1.  克隆仓库:
    ```bash
    git clone https://github.com/yourusername/pixelshift.git
    ```
2.  使用 Android Studio 打开项目。
3.  连接 Android 设备或启动模拟器。
4.  点击 **Run** (运行) 按钮。

## 📝 开发计划

*   [ ] 集成 Rust 核心引擎 (通过 JNI) 以提升图像处理性能。
*   [ ] 添加自定义调色板导入功能。
*   [ ] 批量处理模式。

## 📄 许可证

MIT License
