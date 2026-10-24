# 架构与集成方案

## 采用方案
派生固定 Trime 源码，保留 Android InputMethodService、JNI 与 librime 集成，新增项目自己的 UI 状态控制器和九宫格视图。主题配置负责颜色/尺寸等数据；动态候选栏、敏感模式、异步一致性和宿主提交由代码负责。M1 先证明主题扩展点和前端状态接口，如必须自绘视图则通过小改动接入现有服务，不重写输入法全栈。

根目录为管理仓库。后续 Android 构建根约定 `android/trime/`，以**纳入同一 Git 的源码快照**派生，不创建嵌套开发仓库、不把主仓库只变成 submodule 指针；保留来源、许可证与补丁账。下载用项目内 `.cache/upstream/` 临时 checkout，按固定 commit 取所有递归子模块；主仓库纳入所需源代码并记录每个来源/commit，不把 `.git` 和密钥复制进来。排除未知资源与在线功能时逐项记录。M1已导入android/trime递归源码快照，上游基线及独立probe构建通过；完整IME与资源包未验收。probe临时应用边界见ADR010—012。

## 逻辑边界（名称为拟定接口，不声称上游已有）
| 边界 | 责任 | 约束 |
| --- | --- | --- |
| ImeSessionController | 字段开始/结束、sessionId、输入类型、隐私策略 | 切字段/模式/进程重启时清除旧状态 |
| KeyboardReducer | 纯状态转移，决定栏位/布局/动作 | 不直接依赖 Android、JNI、文件或全局状态 |
| NineKeyView | 绘制、触控命中、长按/滑动取消/无障碍 | 所有尺寸由同一 layout spec，Android inset 单独算 |
| RimeEngineAdapter | 串行调用引擎；映射候选、preedit、selection、commit | native 非线程安全假设；同一专用队列，主线程不部署 |
| CandidatePresenter | 真实候选、分页/展开、稳定选择 ID | 输入版本+sessionId 双重过滤，不凭列表下标选择过期候选 |
| HostEditorAdapter | InputConnection 的 composing/commit/delete/action | 统一提交出口，事务边界、无连接降级、幂等保护 |
| PrivacyPolicy / LearningStore | 敏感检测、临时无学习会话、清除学习 | 私有 noBackup；禁止上下文/词条日志；Lua 与 C++ 都约束 |
| ResourceDeployer | 验证包哈希、首次离线部署、版本迁移、回滚 | zip-slip/大小限制、原子替换，不加载未知脚本 |

数据流：触控→语义事件→Reducer/串行引擎→带 sessionId 和 revision 的快照→Reducer→UI；引擎 commit 事件→唯一 HostEditorAdapter→宿主；学习只在普通字段提交完成时发生。不要同时从“选词点击”和“引擎提交回调”两处 commitText。部署/词库编译线程与会话访问需互斥并显式失效旧 revision。

## 九键与万象闭包
候选基线 `wanxiang_t9`：上游 schema 指向 `wanxiang_lite`、`wanxiang_abbrev_t9`、`wanxiang_phrase_t9`，并引用 Lua、symbols、反查和模型。不能只复制一个 schema。M1 列出 dependencies、dictionary/import_tables、__include、Lua require、OpenCC、grammar.language 全部依赖，生成实际打包清单及哈希。`custom/` 下入口需复制到有效资源根并部署，不能误以为放在 custom/ 即生效。检查 release zip 实际内容后选资源来源（尚未下载/解压验证）。

Trime 3.3.12 固定 librime 子模块正好是 1.17.0 对应 commit，同时带 lua、octagram、predict 子模块；存在子模块不等于最终二进制正确注册和启用。必须查询运行时组件/部署日志（无用户输入）并测试。模型候选 `wanxiang-lts-zh-hans.gram` 在构建时纳入离线资源，禁止安装后联网补齐。模型授权或性能未过时可开发无模型探针，但不能把完整上下文要求标为完成。

## 用户数据与上下文
词频/造词优先复用 Rime 用户词库，Lua context_reorder 另有持久数据，全部置私有 noBackup 根。禁用输入统计模块、同步目录/导出、剪贴板历史/自动读取、语音和外部更新入口。对每个 Lua 文件检查 `io`、`os`、FFI、进程执行和路径访问；禁用不必要模块后重新验证候选/排序。

区分：①固定词库质量；②引擎内局部上下文/grammar 模型排序；③个人词频和 context_reorder 学习；④提交后主动下一词联想。默认展示优先级：敏感屏蔽→正在组合候选→空闲标点，④默认关闭。默认只用本会话合成/已提交的最小上下文，不扫描宿主全文；切 App、字段、隐私模式、取消或超时清空；若引擎无法完全关闭学习，以隔离的临时非持久会话实现，不能仅设置 UI 开关。

## 构建和资源策略
先 arm64-v8a Pixel，x86_64 模拟器可用于测试；release 与 debug 同样无网无备份。改 applicationId 为独立 `dev.local.iosninekeyime`（debug 可带 .debug），代码 namespace 初期保留上游便于最小派生；验证所有 provider authority、intent、FileProvider 和设置跳转随包名更新。依赖只允许构建时解析，锁定版本和校验元数据；Gradle Wrapper 下载校验补齐。首次部署失败进入明确错误态，不输出乱码、不联网。

## 源码快照导入的构建适配
上游NativeAppConventionPlugin读取子库git describe；无嵌套.git时必须从provenance锁生成版本常量，避免误取本仓库版本。ProjectExtensions还读取builder/remote/时间，M1改为显式固定值，禁止为绕过构建而添加远端。具体已核对字段见UPSTREAM。

## M2薄服务实现偏离（ADR024）

实际TrimeInputMethodService.onCreate直接依赖全局AppPrefs/RimeDaemon/主题/剪贴板和旧外部路径链路；为保留受测离线边界，薄服务继承同源LifecycleInputMethodService，复用Rime JNI和librime而不注册旧复杂前端。SerialRimeEngine单worker+全进程租约，KeyboardController连接真实HostEditorAdapter与临时NineKeyView。敏感field不调用引擎且保留租约防探针进入；field/end/hide失效回调。完整系统生命周期与多宿主测试仍未跑，不等于已适用日用。界面暂只取前100候选，显示分页不等于完整native分页协议。

结构删除WIP补充（ADR038）：KeyboardHostPort的deleteAsync显式回调是controller顺序屏障；旧会话epoch先失效再结束port，避免同步取消回调排空旧队列。敏感HostEditorAdapter通过薄服务转发的CursorAnchorInfo获取必要坐标、一次TextBoundsInfo及有界宿主CHARACTER手势；不读文字正文，不读composing/segment finder，不持久化元数据，不发DEL后备。结果不明失败关闭并停止队列。系统回调/多宿主仍未验证；详细边界见testing/STRUCTURAL_DELETE-20261024.md。

## 参考五态前端增量（WIP）

参考深色五图现已部分到位。KeyboardReducer显式管理Chinese/Latin/Number/Symbols/Emoji及返回来源；ReferenceGeometry纯模型输出统一键框/事件/原创glyph语义，NineKeyView仅绑定实际视图、触控、候选稳定ID与短暂面板状态。Emoji查询/分类/分页只在视图内存，字段/布局结束清理，不接IC正文或持久层。FrameLayout组合框架TextView与Canvas图标，避免强行换AppCompat主题/EmojiCompat初始化；无需新依赖。

图示没有覆盖的长按/次页/消歧/语音不据静态图宣称等价；性能、系统状态往返、字体缩放/可访问性、最终release均待测。当前调试显示采用渲染器intrinsic高度到服务窗口的有界请求，纯布局dump辅助核对；是否保留须由重显故障A/B确定，不能把可访问性裁图大小直接等同实际View尺寸。

窗口策略澄清（2026-10-24）：显式固定高度/maximumWindowMetrics候选没有通过真实重显门禁，已撤掉Window固定高度设置；目前回到框架WRAP_CONTENT，纯几何dump用于诊断且不调用正文/默认EditorInfo dump。未认定系统平台缺陷，旧UI对照仍通过。
