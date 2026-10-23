# 上游核验、版本选择和Android九键集成

核验日期：2026-10-23。通过GitHub API读取release/tag commit/tree，再读取对应SHA的原始文件；每个快照的来源、字节数和SHA256见 [upstream-lock.json](../upstream-lock.json)。研究锁固定可复查资料；`runtime_assets=[]`表示没有词库/模型制品已被认定可用于应用。

## 固定研究基线
| 项目 | 本次选择 | 完整commit | 授权证据 | 状态 |
| --- | --- | --- | --- | --- |
| osfans/trime | v3.3.12，2026-09-01发布；API本次latest稳定版 | e09ac7114fa067904ed358e82d499516cb423ff4 | [GPLv3正文](reference/upstream/trime/LICENSE)；manifest/build SPDX为GPL-3.0-or-later | 可作为派生候选，未构建 |
| rime/librime | 1.17.0，2026-06-06发布；与所选Trime子模块一致 | 33e78140250125871856cdc5b42ddc6a5fcd3cd4 | [BSD-3-Clause](reference/upstream/librime/LICENSE) | 已核对gitlink，未运行 |
| amzxyz/rime-wanxiang | v18.1.7，2026-10-20发布 | 89075fe7b870dcf80249dfa0ece7474f1a960d91 | [CC-BY-4.0](reference/upstream/wanxiang/LICENSE) | schema已读；资源包兼容未测 |
| amzxyz/rime-wanxiang-data | 原amzxyz/RIME-LMDG地址重定向；研究固定当日HEAD | 52328d36137a68ea9f9b6855a895ac0417b6b6a1 | [根许可](reference/upstream/wanxiang-data/LICENSE)及[LICENSE-SCOPE](reference/upstream/wanxiang-data/LICENSE-SCOPE.md) | 仅数据/模型来源研究，不集成工具代码 |

不要把data仓库latest release `tool`当作语法模型版本；同一仓库有工具、nightly词库与LTS模型不同release。网页README与默认分支随时间变化，以这里固定SHA和独立制品哈希为准。

## 已从源码核对的事实
1. Trime是Android IME前端，不是词库；Java/Kotlin/JNI复用Rime。万象是schema、词库、Lua和相关模型生态，不能独自替代Android键盘UI。
2. 所选万象确实有 `wanxiang_t9.schema.yaml` 和 `wanxiang_t9i.schema.yaml`。前者schema_id=wanxiang_t9，name=万象九键；依赖wanxiang_abbrev_t9、wanxiang_phrase_t9，translator.dictionary=wanxiang_lite；上游xlit把ABCDEFGHIJKLMNOPQRSTUVWXYZ映射到22233344455566677778889999。它是编码支持证据，不证明iOS界面/Android完整体验。
3. 九键schema包含Lua context_reorder、super_processor等，enable_user_dict=true、grammar.language=wanxiang-lts-zh-hans，contextual_suggestions=false。输入统计也在translator列表中，本项目要移除或彻底禁用，不能因“本地统计”就保留非必要输入收集。
4. [Trime .gitmodules](reference/upstream/trime/.gitmodules)及tree固定lua/octagram/predict等依赖；[Rime.cmake](reference/upstream/trime/app/src/main/jni/cmake/Rime.cmake)指定这些插件构建。实际二进制注册与schema加载仍待M1探针。
5. [Trime manifest](reference/upstream/trime/app/src/main/AndroidManifest.xml)没有直接声明INTERNET，但**allowBackup=true**，且有文档provider/文件provider。不能从单个上游清单推断合并APK无网或无泄露；本项目必须改备份并审计导出路径。
6. `custom/wanxiang_lite.dict.yaml`、schema/custom入口存在于上游tree；九键依赖入口/导入词库/Lua/符号/OpenCC/grammar需要一起部署，不可以只放一个t9.yaml。release zip内是否包含完整闭包尚未下载检查。

关键证据：[九键schema](reference/upstream/wanxiang/wanxiang_t9.schema.yaml)、[t9i备选](reference/upstream/wanxiang/wanxiang_t9i.schema.yaml)、[lite词库入口](reference/upstream/wanxiang/custom/wanxiang_lite.dict.yaml)、[简码词库入口](reference/upstream/wanxiang/wanxiang_abbrev_t9.dict.yaml)、[个人上下文脚本](reference/upstream/wanxiang/lua/wanxiang/context_reorder.lua)。t9和t9i的差异与Android按键适配须用相同语料比较，当前只选t9做先行探针，不虚构t9i已验证更好。

## 固定构建配置（不等于本机已满足）
| 项目 | 固定源码值 | 证据 |
| --- | --- | --- |
| Gradle Wrapper | 9.5.1-all；原properties未见distributionSha256Sum | gradle/wrapper/gradle-wrapper.properties |
| AGP / Kotlin / KSP | 9.3.0 / 2.3.21 / 2.3.6 | gradle/libs.versions.toml |
| compileSdk / targetSdk / minSdk | 36 / 36 / 21（上游值） | app/build.gradle.kts |
| SDK build tools | 36.0.0 | app/build.gradle.kts |
| NDK / CMake默认 | 28.0.13004108 / 3.31.6 | build-logic/.../Versions.kt |
| Java/Kotlin字节码目标 | Java11 / JVM11 | app/build.gradle.kts + AndroidBaseConventionPlugin |
| ABI | 上游支持armeabi-v7a/arm64-v8a/x86/x86_64；本项目先arm64-v8a | Versions.kt + NativeBaseConventionPlugin |

当前主机doctor发现JDK21.0.7；是否可用须实际Wrapper/configure/build/lint验证，不能把源码Java11目标当作Gradle运行JDK。官方 [AGP9.3兼容表](https://developer.android.com/build/releases/agp-9-3-0-release-notes)要求Gradle≥9.5.0、JDK≥17，且9.3.2记录过JDK17 lint修复；本项目仍须测所选9.3.0与本机JDK21，升级要独立记录，不能初始化时编造验证。

快照导入还需处理上游构建元数据：`NativeAppConventionPlugin`会在librime/OpenCC路径运行git describe；`ProjectExtensions`读取Git版本、builder、remote和当前时间。移除嵌套.git后这些值可能错误或失败。M1改为从本仓库provenance锁读取库版本，并固定/显式传递buildTimestamp等字段；禁止偷偷给根库添加远端或让子库git命令误读主仓库。`BUILD_ABI`/`buildABI`、`NDK_VERSION`/`ndkVersion`、`CMAKE_VERSION`/`cmakeVersion`是已在ProjectExtensions核对的扩展点；是否在实际构建正确生效待验证。

## 模型候选（未下载）
LTS release中的 `wanxiang-lts-zh-hans.gram`：398,497,836 bytes；release id 268075342，asset id 655157355；API提供publisher digest `sha256:9eedac1d836f3fdafb61b162035c8937253aedc106be40b2b957d9ab64e9ed89`。这是**发布方元数据**，尚未下载计算本地哈希或验证授权/兼容性。LTS标签/资产可能重发，下载时再次核对asset id、时间、大小、摘要，差异先调查，不直接更新锁。

只在模型授权范围清楚后，以构建时完整离线资产纳入；首次安装不得依赖网络。评估APK体积、首次解包、mmap/PSS、完整性和升级空间；不能把近400MB文件体积当成实际内存占用。若不合适先形成测量与替代方案，完整上下文需求仍保持未完成。

## M1必须跑的兼容性探针
- 列全资源闭包与哈希，下载zip检查实际内容和许可证，不假定base/lite包名就代表九键完整支持。
- 记录运行时Rime版本、注册组件、Lua API可用性、grammar启用证据；在断网首次部署下用合成语料输入数字编码。
- 64426候选能含“你好”、多音节/歧义/消歧/部分提交/退格正确；候选排序是实测而不是固定假数据。
- normal学习可复现；敏感模式在Rime与Lua两层无持久写；input_statistics不运行；用户库路径不落共享存储。
- 对比无模型/有模型的性能与排序；未过关不宣布“和Gboard一样”或“支持所有上下文”。
