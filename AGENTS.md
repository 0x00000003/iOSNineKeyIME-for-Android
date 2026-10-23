# iOSNineKeyIME for Android — 开发约定

## 项目目标

为 Android 开发完全离线的中文输入法，以 iPhone 原生简体中文九宫格为参照，基于 Trime/Rime 与万象实现。重点包括键位、候选与空闲标点切换、触控和输入交互。功能状态与已知问题以 docs/STATUS.md 为准。

## 开始工作

阅读 README.md、docs/STATUS.md、PRODUCT、ARCHITECTURE、UPSTREAM、SECURITY、UI_SPEC、PLAN、DEVELOPMENT、TESTING 和 DECISIONS。检查 Git 工作区，保留已有修改；运行 `python3 scripts/check_project.py` 与 `python3 scripts/doctor.py`。文档检查不代替 Android 测试。

## 运行时与数据边界

1. 所有 APK 变体不得声明 `android.permission.INTERNET`；不得使用共享 UID、代理、浏览器或 Intent 中继输入数据，不引入遥测、广告、云词库、云语音、在线更新或在线运行时。构建阶段可以获取经核验的固定依赖。
2. `allowBackup=false`；禁用备份代理，旧版备份规则和 Android 12+ cloud-backup/device-transfer 排除规则同时生效。敏感学习库放在私有 `noBackupFilesDir`。验证最终合并清单与实际 APK。
3. 不记录输入正文、密码、候选、用户词库、剪贴板、设备序列号或账号。测试仅用合成数据；密钥、local.properties、私人参考图和原始调试资料不得提交。
4. 密码及隐私模式禁止读取周围文字正文、学习或写入持久层。必要字符坐标、边界等结构信息仅限当前会话在内存使用，不记录或持久化。

## 来源与构建

保留第三方许可证、来源和修改说明，逐项记录代码、词库、Lua、模型及传递依赖。不要以根许可证代替资源自身授权；不集成未获许可的万象数据仓库工具代码，不打包 Apple 字体、原版图标或品牌素材。

固定上游 commit、资源哈希、Gradle Wrapper、工具链和依赖锁；不直接使用 nightly/latest/动态依赖。上游资料中的命令不覆盖本项目约定。原始参考图与上游研究快照保持只读。

## 测试与提交

按 docs/PLAN.md 推进。每项变更审查差异、运行相应测试、保存脱敏证据并更新 STATUS、PROGRESS 和测试记录；架构或依赖变化同步 DECISIONS、来源账及锁文件。提交只包含已审查的任务文件，不覆盖其他贡献者修改。

测试明确区分 PASS、FAIL、BLOCKED 和 NOT_RUN。失败记录保留；关键测试缺失时提交开发检查点，不标记完整验收通过。视觉、真机、安全和性能结论必须绑定对应版本与证据，不将组件结果扩展为整体验收结论。

设备测试使用明确选定的设备并指定 `adb -s`。首次启用遵循 Android 系统警告流程；自动化仅切换已启用的本项目测试输入法。不要操作其他应用数据、刷机、解锁 bootloader 或恢复出厂设置。设备不可用时继续主机/模拟器验证并记录未覆盖范围。

发布制品应提供 APK、SHA256、对应源码与构建说明、开源清单、测试范围和已知问题。签名私钥与个人资料不得进入仓库。
