# 更新日志

本项目的所有重大更改将记录在此处。

## [2.0] - 2026-03-08

### ✨ 新增 (Added)
- **Professional Layer System (专业级图层系统)**:
    - **Entity-based Model**: 重构图层为具备 UUID 唯一标识的实体对象，彻底解决撤销/重做及排序时的索引错位灾难。
    - **Hardware-Accelerated Compositing**: 实现基于**画家算法 (Painter's Algorithm)** 的渲染管线，支持 $O(1)$ 可见性裁剪。
    - **Advanced Blend Modes**: 引入正片叠底 (Multiply)、滤色 (Screen)、叠加 (Overlay) 等多种光学混合模式，由 GPU 片元着色器实时计算。
    - **Interactive Reordering**: 支持长按拖拽重排图层，配合 `zIndex` 浮空幻象与 `animateItemPlacement` 位移动画。
    - **Non-Destructive Editing**: 所有的透明度 (Opacity) 和混合模式调整均在渲染时动态应用，不破坏原始像素数据。
- **Commercial-Grade Persistence & Export (商业级存档与导出)**:
    - **.pxl Private Project Format**: 自研基于 ZIP 的混合打包格式。`manifest.json` 存储元数据，各图层以无损 PNG 资产隔离存储，兼顾解析速度与内存安全。
    - **Lossless Upscale Engine**: 导出时强制采用**最近邻插值 (Nearest Neighbor)**，支持 1x 至 30x 自由放大，确保像素边缘如刀切般锐利。
    - **Project Lifecycle Management**: 集成 Android 原生存取框架，支持工程文件的“打开”与“另存为”。
- **Elite Auxiliary Drawing Tools (精英级辅助工具)**:
    - **Symmetry Drawing (对称/镜像)**: 在绘图引擎层注入 X/Y 轴映射逻辑，支持单笔触多点同步写入及原子化 Undo 记录。
    - **Bilinear Reference Layer**: 引入物理隔离的参考图层，采用双线性过滤渲染以确保高分辨率背景清晰，与硬边缘像素网格和谐共存。
    - **High-Performance Navigator**: 实现基于**逆矩阵映射 (Inverse Matrix Mapping)** 的全局导航窗，支持在小窗中通过反向平移控制主视口。
- **Color Central Central (色彩数据中枢)**:
    - **HSV-First Picker**: 针对像素画师优化的 HSV 色彩空间控制，支持精确的 Hue-Shifting。
    - **Hex Two-way Binding**: 实现 HEX 代码与滑块位置的 120Hz 实时双向绑定。
    - **Lospec Ecosystem Connect**: 实现与 Lospec.com 官网 API 的异步直连，支持通过 Slug 一键导入全球顶尖像素画色板。
    - **Retrofit Presets**: 内置 GameBoy, Pico-8, NES 等复古硬件限色预设。

### 🐛 修复 (Fixed)
- **解决撤销/重做“双击”难题**: 优化 `saveState()` 调用时机，将其从“动作结束”前移至“落笔瞬时”，并清除所有冗余快照保存点。
- **修正 Bitmap 类型安全**: 修复了 `duplicateLayer` 时 `Bitmap.Config` 可选性导致的编译不匹配问题。
- **完善渲染同步**: 引入全局 `version` (Render Trigger) 机制，解决了直接修改像素引用不变导致 Compose 不重组的问题。

## [1.5] - 2026-03-08
... [rest of file]
