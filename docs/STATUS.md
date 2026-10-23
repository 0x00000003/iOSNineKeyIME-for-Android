# 当前状态

日期：2026-10-23（America/New_York）。阶段：M0 初始化文档与检查完成（非应用完成），结果见 [初始化记录](testing/INIT-20261023.md)。

| 项目 | 状态 | 证据/下一步 |
| --- | --- | --- |
| 独立目录、约束与产品/设计/开发/测试文档 | 已创建 | 根目录及 docs |
| 上游版本、许可与关键源码快照 | 研究已核对 | UPSTREAM、upstream-lock.json |
| Android 可编译工程、JNI、资源完整包 | NOT_RUN | M1 导入并构建 |
| 九键编码和候选真实运行 | NOT_RUN | M1 探针，不能由 YAML 存在推定 |
| iPhone/Pixel 原截图 | BLOCKED | 原对话接口未提供附件，reference/README |
| 真机 Android 版本、密度、字体比例、导航模式 | NOT_RUN | M1 经授权读取测试 Pixel |
| 词频学习、上下文排序、敏感场景 | NOT_RUN | M3/M4 |
| APK 权限/备份、离线、性能和回归 | NOT_RUN | M4—M6 |
| 文档、结构和工具检查 | 见真实执行记录 | testing/INIT-20261023.md |

下一项：M1 工具链与九键闭包兼容性探针。图片缺失不妨碍此项，但 M5 视觉验收不能绕过。可先实现明确标为临时的 UI。

不确定项：v18.1.7 各 release zip 的九键文件覆盖；wanxiang_lite 入口文件实际打包位置；Lua 上下文学习在隐私模式的彻底关停；398MB 级简体模型在 Pixel 的性能/授权/打包方式；Rime 和 Android 前端回调的竞态；OEM/系统版本的数据迁移行为。后续必须实测，不引用 README 宣传效果为本项目结论。
