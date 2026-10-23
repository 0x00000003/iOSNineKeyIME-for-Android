# 当前状态

日期：2026-10-23（America/New_York）。阶段：M1 兼容性开发进行中（WIP，非应用完成），结果见 [初始化记录](testing/INIT-20261023.md)。

| 项目 | 状态 | 证据/下一步 |
| --- | --- | --- |
| 独立目录、约束与产品/设计/开发/测试文档 | 已创建 | 根目录及 docs |
| 上游版本、许可与关键源码快照 | 研究已核对 | UPSTREAM、upstream-lock.json |
| Android工程/JNI | WIP | 已导入并构建上游基线；独立探针debug/test/release构建通过，完整资源包未通过 |
| 九键编码和候选真实运行 | host/Pixel探针部分PASS | 64426首选、Lua、单次提交/退格通过；不是完整IME验收 |
| iPhone/Pixel 原截图 | BLOCKED | 原对话接口未提供附件，reference/README |
| 测试Pixel系统库存 | PASS（只读） | Android17/API37、arm64；不代表真机输入测试通过 |
| 词频学习、上下文排序、敏感场景 | NOT_RUN | M3/M4 |
| APK 静态权限/备份 | 探针debug/release PASS | 实际APK解码及两套XML；不是完整M4验收 |
| 首装离线运行 | 专用模拟器探针PASS | Pixel飞行模式/M6发布回归尚未执行 |
| 性能和完整回归 | NOT_RUN | M5—M6 |
| 文档、结构和工具检查 | 见真实执行记录 | testing/INIT-20261023.md |

下一项：M1 工具链与九键闭包兼容性探针。图片缺失不妨碍此项，但 M5 视觉验收不能绕过。可先实现明确标为临时的 UI。

不确定项：v18.1.7 各 release zip 的九键文件覆盖；wanxiang_lite 入口文件实际打包位置；Lua 上下文学习在隐私模式的彻底关停；398MB 级简体模型在 Pixel 的性能/授权/打包方式；Rime 和 Android 前端回调的竞态；OEM/系统版本的数据迁移行为。后续必须实测，不引用 README 宣传效果为本项目结论。

M1工作和失败重测见 [本轮记录](testing/M1-20261023.md)。模型哈希已核对但资产归属未完成，不打包。当前没有完成的M1验收结论。

Pixel修复重测通过，APK和证据绑定见[M1真实结果](testing/evidence/m1/pixel-probe-result.json)。第一次JNI接口崩溃及修复均保留；35项JVM测试、debug/release lint均通过；完整产品门禁不能宣称全部PASS。
