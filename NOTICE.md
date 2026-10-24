# 来源与许可声明

本仓库原创程序/工程准备内容按 GPL-3.0-or-later 管理，正文见 LICENSE。研究快照、词库、模型、图片和后续导入第三方代码保留各自授权，根 LICENSE 不重新授权它们。

- Trime：Rime community / osfans 及贡献者，v3.3.12；源码文件含 GPL-3.0-or-later SPDX，根许可证为 GPLv3文本。
- librime：Rime contributors，1.17.0；BSD-3-Clause。
- rime-wanxiang：amzxyz 及贡献者，v18.1.7；根 CC-BY-4.0，复制/派生时保留归属、链接、许可和修改声明，并检查逐文件例外。
- rime-wanxiang-data（旧址RIME-LMDG）：amzxyz及贡献者；原创数据CC-BY-4.0，但 LICENSE-SCOPE.md 明确原创程序代码未授予独立复制/修改/再分发/商业许可。当前仅研究快照，未集成其工具代码或模型制品。

原始许可证保存在 docs/reference/upstream 各项目目录，固定来源与哈希见 upstream-lock.json。本次对快照原文未作内容改写，仅按项目目录保存。后续资源改动必须逐项登记。此清单不是最终APK完整SBOM，正式分发前必须补齐传递依赖、Lua、模型、字体图标、词库来源和对应源码。

## M1 source import / local compatibility probe (2026-10-23)

Pinned Trime recursive sources are now imported under `android/trime/`, with each
original license/copyright retained. Exact repositories, commits and files are in
`third_party/trime-repositories.json` and `third_party/trime-sources.json`;
modifications to original files are in `third_party/trime-modifications.json`.

This is a WIP local probe, not a redistribution/compliance approval. The probe
uses a separately inventoried CC-BY-4.0 Wanxiang data/config subset and an original
GPL-3.0-or-later Lua test marker; it omits all other upstream Lua, statistics and
the model. Individual probe resources and attribution are listed in
`third_party/android-probe-resources.json`. No data-repository tool code is used.
The APK's Java/native transitive dependency license audit remains incomplete;
source inclusion or successful compilation does not close that gate.

## M4 本地派生增量（2026-10-23）

离线许可bundle由third_party/offline-notice-sources.json固定43个原文及SHA，来源文本保留原版权；实际运行时制品清单在third_party/runtime-inventory-release.json（POM/parent SHA与发布者许可声明）。BRAVH/Kodein/Kaverit精确版本MIT原文见docs/reference/upstream/maven-mit，Boost正文同目录boost。新设置/测试/生成器/严格公共资源验证沿用GPL-3.0-or-later。对Trime/Lua子模块修改：关闭旧同步worker，缩小R8旧前端roots，限制Lua标准库与固定内置模块，native编译去本地路径；完整逐文件变更SHA见third_party/trime-modifications.json。没有复制万象数据仓库未授权工具，没有打包Apple素材。完整对应源与分发审查仍WIP。


后续WIP：删除未用community-material-typeface及其字体，当前debug/unsigned/本地签名release实际APK字体数均0；历史字体原文来源仍保留而不作为运行时许可。实际运行时清单现122项，父POM链/制品SHA及明确版本变化见TOOLCHAIN记录；离线许可正文仍43份，包含保留的上游源码归属，不能将它等同全量分发义务已关闭。


738a365增量：固定Android NDK r28 (28.0.13004108)明确选择静态C++；加入其LLVM runtime/toolchain完整原文与其他版权/例外，离线正文现44项。源自SHA19b16241的官方归档，原文SHAf96f763b，位置/完整哈希见third_party/ndk-runtime-license.json；不是断言该广义notice中每个宿主工具都链接进APK。全部实际数据来源/对应源分发义务仍须逐项收尾。
