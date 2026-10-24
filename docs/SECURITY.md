# 隐私威胁模型与安全门禁

## 资产、边界和信任
资产：输入正文、密码、用户词/词频、上下文、组合缓存、剪贴板、测试截图。边界：宿主应用 ↔ Android IME Binder ↔ 本应用 Kotlin/JNI/Lua ↔ 私有资源/学习库；构建机和依赖源不等于运行时。输入法收到文本是实现功能所需，但最小化读取、保留和导出。对手包括恶意/被篡改依赖、导出组件调用方、误配置备份、误写日志与不可信资源包。OS/root/物理调试控制者和宿主自身的文本收集无法由输入法消除；“不开网权限”不是对所有泄露路径的证明。

## 威胁—控制—证据
| ID | 威胁 | 必须实现的控制 | 验证 |
| --- | --- | --- | --- |
| T01 | SDK/JNI/Lua 发送网络请求 | 无 INTERNET/共享UID；无 SDK、socket 客户端、上传中继、WebView/隐式浏览器提交；发布依赖审计 | SEC01 最终 APK+代码/依赖复核；SEC03 UID 网络观测 |
| T02 | 通过云备份/设备迁移带走学习库 | allowBackup=false；无 backupAgent；fullBackupContent 排除根域；dataExtractionRules 中 cloud/device-transfer 都排除 root/file/database/sharedpref/external 及 device-protected 域；数据放 noBackupFilesDir | SEC02 检查实际 XML 资源和目标 OS 行为，不能只检查属性存在 |
| T03 | debug/logcat/crash 文件泄露 | release 禁止正文日志、历史统计、远程崩溃 SDK；仅事件计数/错误码且不含候选或词库 | 合成 canary 输入后搜索 app 日志/私有文件，SEC04 |
| T04 | 密码/隐私字段仍触发底层学习 | 检测 text/password、visible/web password、number password；IME_FLAG_NO_PERSONALIZED_LEARNING；用户隐私开关优先；禁上下文读取和 Rime/Lua 每个持久写入点 | U05/I05；前后所有用户库字节/记录差异、重启后 canary 不可找回 |
| T05 | 导出组件、共享文件、剪贴板外泄 | IME service 虽 exported=true 但用 BIND_INPUT_METHOD 保护；其余非必要组件不导出；无学习库 FileProvider/SAF 导出、无输入 ACTION_SEND、无自动剪贴板读取 | final manifest+组件调用测试；禁止把 service 一概不导出导致 IME 不工作 |
| T06 | 任意主题/Lua/压缩包执行/穿越 | 首版只加载内置固定资源；禁自定义在线源、任意脚本与外部目录自动加载；安全解包与原子部署 | zip-slip/符号链接/超大包/中断/损坏哈希测试 |
| T07 | 切字段/异步回调泄露旧内容 | sessionId/revision、单一提交出口、清理候选/上下文/重复按键 | I03/I04，切密码字段无旧词 |
| T08 | 本地库残留与重装 | 不备份不迁移，明确“清除学习数据”原子清除所有 Rime/Lua/缓存；迁移失败不回写用户敏感数据 | 清除/重启/卸载重装合成测试，非恢复出厂 |
| T09 | 构建供应链/许可误判 | 固定commit/哈希、递归子模块和 Gradle 验证；不执行远程 install 脚本；核对脚本与资产授权 | M1 依赖SBOM/NOTICE、REL01 |

## 持久层与保留
内置词库/模型为只读应用资源，可部署到私有 noBackup 版本目录；用户词库/上下文 DB 必须独立，以便清除。默认允许普通字段本地学习，并在设置说明；个人上下文默认仅会话内、跨字段清空；不读通讯录/短信，不建输入历史或剪贴板历史。关闭学习后禁止持久写新词与调频，包括 Lua DB/WAL/临时文件。清除后重新打开引擎，确认无旧 handle 写回。崩溃与 ANR 本地系统诊断风险通过不记录正文减小，不能宣称操作系统也不可能保留内存。

## 最终 APK 检查要求
对 debug 和 release 分别从 APK 解码 manifest，扫描所有 uses-permission/uses-permission-sdk-*（包括 INTERNET 的 maxSdkVersion 变体）；检查 sharedUserId、allowBackup、backupAgent、debuggable、所有导出组件/权限与 provider grants。解析实际打包资源中的备份 XML，确认 device/cloud 域完整。要求数据迁移未包含敏感库，API/目标系统变化时复核 [Android 官方备份文档](https://developer.android.com/identity/data/autobackup)。noBackup 是额外防线，不替代规则和实测。

断网测试覆盖首次部署/词库编译/模型加载、冷启动、长句、学习与重启。联网网络观测仅用合成输入，绑定本应用 UID，普通 adb 不能总是完整抓包；工具权限不足写 BLOCKED，不能以“没看到请求”推断没有。禁止为了观测网络 root 用户手机。代理/浏览器/Intent 中继是没有 INTERNET 也必须排查的路径。最终出具已做检查及残余边界，不作“绝对不会截获”的承诺。

源码参考中 Trime 清单 `allowBackup=true` 是上游事实，必须在派生应用修改；参考文件保留原样，不能为让扫描过关而篡改证据。

用户隐私范围澄清（ADR037，2026-10-24）：敏感模式禁止读取文字正文；必要结构信息可以使用。生产结构删除只在当前作业内消费selection、插入行/editor边界和最多32个尾部单元坐标/matrix，不读取composingText或SegmentFinder、不序列化、不日志/持久化。不把“正文getter0”写成“零结构读取”。连接/session/selection检查及两秒预算保护异步删除，已发手势未知结果不自动重试；宿主自身违反API契约或已应用后抛异常不能由适配器凭空还原真实正文。当前合成组件记录不替代完整系统IME I05/SEC04及目标OS备份/迁移。
