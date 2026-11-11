# ADR 0009 — 借 Spektrafilm-android 的应用层行为，不重写架构

- **Status:** Accepted
- **Date:** 2026-11-10

## Context

`thetechgeekko/Spektrafilm-android`（GPLv3）与本项目同源到 `spektrafilm` 引擎，
但产品路线不同：对方是 Kotlin/Compose 全功能编辑器，本项目是 Java/View + M3 控制台，
引擎在仓库外。对比后确认四项可直接借鉴、且不需要改引擎：

1. **内置外观**：六个常用胶片组合，以稀疏覆盖应用，不重置构图/视图。
2. **预设强度**：从当前 Look 线性过渡到目标 Look，类别值在 50% 切换。
3. **最小会话存档**：只保存当前照片 URI、Look 和当前页，原子替换；启动时校验 URI。
4. **事务化相册导出**：失败删除 `IS_PENDING` 行，发布前回读长度和摘要。

同时确认**不**借：Compose/Hilt 重写、CPU 精确引擎、三 ABI、遮罩/局部调整、
曲线、ICC/PNG16/TIFF32F/场景线性/LUT。它们要么需要另一个引擎，要么需要新的原生
writer，超出当前一刀的范围。

## Decision

1. 保持 Java/View 组合根与 M3 控制台；只移植行为与契约，不引入 Compose。
2. **Built-in look 是稀疏覆盖**，不是 `LookPreset` 的完整快照。它必须保留用户未
   authored 的构图、裁切、日期和视图字段。
3. **预设强度**是显示层的 0–1 混合：数字与等长数字数组线性插值，布尔、字符串以及
   seed/turn 这类类别数字在 0.5 切换。应用仍只记录一次 undo、渲染一次。
4. **会话存档只覆盖当前帧**：当前 source URI、Look、当前 tab、Scan 位置。多帧列表、
   完整 undo/redo、导出选项和遮罩索引不在本切片。存档写在 `noBackupFilesDir`，
   先写临时文件再替换；启动恢复前必须确认 URI 仍可读。
5. **导出事务的最小闭环**：`IS_PENDING` 行在复制失败、长度或 SHA-256 回读不一致时
   删除；只有回读一致才把行发布。不引入 pending journal 或前台服务。
6. 直接借鉴设计并重写实现；不复制对方的 Compose 代码。两项目均为 GPLv3，若复制
   片段必须保留来源与修改标记。

## Consequences

- 内置外观与预设强度完全可离线、可在纯 JVM 检查；不增加引擎风险。
- 会话存档只恢复一张照片；进程死亡后多帧工作区仍会丢失。完整会话需要另立 ADR。
- 导出在慢存储上多一次回读 I/O；换来的是不再把半写文件发布到相册。
- 以后要借曲线、遮罩或 ICC 输出，必须先决定是否需要新引擎参数或原生 writer，
  不能把本 ADR 当作“全功能对齐”的承诺。
