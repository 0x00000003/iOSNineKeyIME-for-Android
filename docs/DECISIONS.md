# Decision log

| ADR | 日期 | 状态 | 决定与理由 | 后果/复核条件 |
| --- | --- | --- | --- | --- |
| 001 | 2026-10-23 | Accepted | 派生固定Trime源码复用IME/JNI/Rime，自主UI控制器；主题不是完整逻辑 | M1验证扩展点，若不适用先给可运行探针和替代方案 |
| 002 | 2026-10-23 | Research pinned | Trime v3.3.12 + 其固定librime 1.17.0，万象v18.1.7九键候选 | 不代表兼容已证实；升级必须重跑集成/合规/安全 |
| 003 | 2026-10-23 | Accepted | 运行时无网无备份，无遥测广告；学习库私有noBackup，切敏感完全禁写 | 无INTERNET不能代替全部威胁审计 |
| 004 | 2026-10-23 | Accepted | 中文Idle标点优先，主动下一词联想默认关闭；上下文排序保留在输入中 | 满足产品目标；原图若有差异逐项确认，不自动牺牲R02 |
| 005 | 2026-10-23 | Provisional | 核心九键布局/尺寸先参数化临时实现，截图缺失 | 不宣称iOS事实；补图后替换参数并做视觉验收 |
| 006 | 2026-10-23 | Accepted | 根仓库文档+未来android/trime源码快照，所有里程碑提交可复查记录 | 固定递归子模块来源，禁止只留无法重建的缓存 |
| 007 | 2026-10-23 | Provisional | 本项目原创程序采用GPL-3.0-or-later，保留Trime同许可标识 | 数据/模型/第三方按独立许可，分发前做完整NOTICE |
| 008 | 2026-10-23 | Research only | LTS简体gram为候选，不带工具/Updater；LTS tag可能更新，不直接当不可变版本 | 实际下载后核对asset ID/digest/授权和兼容性能，再进入runtime_assets |
| 009 | 2026-10-23 | Accepted | 无截图/真机时推进独立工作，受影响门禁保持BLOCKED | 不以文档检查替代产品验证，外部高风险操作另处理 |

后续新增：记录问题背景、候选方案、决定/不确定项、证据、验证计划、受影响需求；改变现有决定追加 supersedes，不删历史。

| 010 | 2026-10-23 | M1 provisional | 独立M1 probe APK复用Trime JNI/DTO，Application使用系统默认，仅暴露合成探针Activity；暂不注册IME，避免初始化旧前端的共享路径/剪贴板/导出功能 | 这是兼容性制品，不能作为R01—R17交付。后续UI必须显式接入受测状态/隐私边界 |
| 011 | 2026-10-23 | Accepted for probe | 无模型探针将schema流水线缩减为原生speller/script_translator与自写无持久Lua marker；不打包统计及其他上游Lua | 原始lite闭包单独清单保留；完整功能/模型验收不能由缩减探针替代，修改及逐文件SHA见third_party |
| 012 | 2026-10-23 | WIP | Gradle验证严格开启；首次下载SHA256以明确TOFU方式固定，Wrapper使用发布方SHA256 | 不能把首次固定说成发布方签名审计；完整SBOM/许可及干净重建仍未通过 |
| 013 | 2026-10-23 | Accepted for offline fork | 去除平台后台通知副作用；旧通知接口仅保留源码兼容，当前probe无相应组件入口；触感改用遵从系统设置的View反馈 | 不新增VIBRATE/POST_NOTIFICATIONS权限，不压低lint；完整前端剥离与设置交互在M2/M4复核 |
| 014 | 2026-10-23 | Accepted | 主机/Pixel/专用离线API36模拟器已实际运行无模型JNI底座，允许并行准备不依赖模型/截图的M2纯状态组件 | M1未测门禁及模型/隐私验收仍保持WIP，不据此关闭M2集成或M3—M6 |

| 015 | 2026-10-23 | Provisional implementation | 对密码/NO_PERSONALIZED_LEARNING/用户隐私开关统一形成敏感策略；当前纯Reducer强制直接英/数，Cancel不得调用native；真实InputConnection/零持久写待接入验证 | 此为保守开发边界，不声称普通隐私中文候选已支持；R07全链路未验收 |
| 016 | 2026-10-23 | Accepted | 状态同时使用真实compositionLength与原始编码，候选menu为空不能推导Idle；触控与绘制共享半开矩形，一指拥有、字段更换终止重复 | 38纯单元测试；Android视图/TalkBack/实际部分选段与宿主集成仍要实测 |
