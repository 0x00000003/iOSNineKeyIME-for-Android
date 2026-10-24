# 当前状态

正在补齐选拼音：预冻结5ae5845的真实host Rime探针已通过混合/明确音节、已选前缀保持和提交一次性，证据[pinyin-host-compat-v1](testing/evidence/m1/pinyin-host-compat-v1.json)。纯音节解析/范围重写组件已完成真实红绿测试（10项：6失败→10通过，全state 93/93）；Android JNI/事件状态/UI尚未接入，仍未完成，不能把纯组件算作按钮已可用。见[记录](testing/PINYIN_STATE-20261024.md)；现有可检查APK继续为下述5c46/c37。

同优化c37的双设备engine/resources/quality/learning/context十条记录已全部通过（词Top5 99/100、句19/20、十词学习/重启/清合成库10/10、上下文9/10）。[当前引擎记录](testing/CORNERS_RELEASE_ENGINE-20261024.md)同时说明旧测试元数据文案与新f7b2测试包两设备复测；不是完整系统质量/隐私或性能验收。两个窗口诊断候选已失败撤销，生产服务恢复已验证检查点。

2026-10-24 当前五态检查点（WIP）：debug `5c46ae95` / 优化本地测试证书 `c37aa834` 已补底角和页脚原创图形尺寸。**两版本分别**在Pixel五态9/9、同字段重显5/5、敏感字素36/36、异步队列20/20 PASS，敏感正文getter0；debug两设备full28故障/敏感5/metadata6、controller和view20通过。83 state、50 debug/50 release app、45工具与构建/lint、三个实际APK静态PASS。原a11y窗口坐标/lifecycle、完整安全/性能/多宿主/正式签名仍未签收，字形/Emoji/导航/未知状态不同，非1:1。见[当前记录](testing/CORNERS-20261024.md)及[三SHA检查点](testing/evidence/m5/reference-ui-corners-checkpoint.json)。下列为历史增量，不替代当前制品证据。

系统回归增量（2026-10-24，WIP）：同debug `179a24e7` 在独立稳定真实像素/触屏协议下五态9/9、敏感字素36/36、异步队列20/20 PASS，敏感正文getter0，第二续输键前确实pending；API36模拟器full28故障/敏感5/metadata6与controller PASS。优化非debuggable本地测试证书版 `b127fe87` 的联合构建/lint/50 JVM、实际APK静态及Pixel同字段5/5已通过，其余测试按独立记录；原a11y坐标仍FAIL，不能关闭整体M2/M5。

2026-10-24 最新增量（WIP）：debug `179a24e7` 修复同一字段仅隐藏/重显后退格使用过期初始caret的问题。最终同host/test旧c51 1/5→新179a 5/5，另一次新179a 5/5；四敏感场景正文getter全0，隐藏期间外移光标也按真实位置删除。保留旧红测及早期no-learn定位中断；当前五态触控回归在number阶段FAIL，不能继承c51/a871的9/9。50 app/83 state/45工具、debug/testhost lint/build、实际APK静态通过。原无障碍与完整安全/性能/正式签收未关闭，见[同字段记录](testing/SAME_FIELD-20261024.md)。

**最新生产检查点（WIP，2026-10-23）：** debug `831233e4` / 默认unsigned `f857df0c` 已补删除后真实caret确认，前次系统17/20红测保留。相同独立host/test在真实Pixel core10/10、敏感字素36/36、正文拒绝/异步续输/长按/切字段20/20均PASS，敏感正文getter0；同debug两设备full组件28结构故障PASS、敏感5/5和metadata6/6、bounds11/cursor22，controller两设备PASS。50app/77state/42工具、两套lint/构建/实际APK静态审计PASS；release尚未绑定本最终哈希运行，不称完整M2/R04/I05安全验收。系统测试的两个本项目IME均已启用；名称现已区分调试/优化测试版，已安装旧优化版尚未随本轮更新。见[系统宿主记录](testing/SYSTEM_HOST-20261023.md)。下列未启用/未授权等均为此前历史。

**M5参考实施中（WIP，2026-10-24）：** 当前debug `c51dad6b` 已按iPhone13/iOS27.2五张深色参考校准五种面板、字号/字距、可见键面与48dp触控框、原创图形及内存Emoji。独立实际像素定位系统触控9/9 PASS，采齐五态+按下6图，真实123/符号/64426选词/abc/Emoji提交删除与宿主上部按钮核对通过；不是内部事件桥，也不替代原门禁。五态高度归一差+0.306..0.525来源px、四种主键矩形最大边差≤1.297px，三类灰色块一致；字体轮廓/Emoji默认顺序/原生导航/其他状态仍有差异，不称1:1。两设备同SHA view-insets20/20+搜索会话清除、full组件28结构故障/敏感5/5/metadata6/6和controller PASS；83 state/50 app/45工具、两套debug lint、实际APK静态PASS。**原无障碍窗口坐标/lifecycle仍FAIL**，透明整窗/事件通知未证明修复，不能沿用像素scope签收TalkBack或M5。敏感数字→符号返回错误已有红测并修为Latin；逐音节选拼音/语音未提供。显示缩放/文字大小未知，release全门禁未跑。见[完整记录](testing/REFERENCE_UI-20261023.md)及[当前检查点](testing/evidence/m5/reference-ui-calibration2-checkpoint.json)。

磁盘管理修正：2026-10-24回收历史隔离构建、未用模拟器及安装包缓存9处，实际释放24.10GiB；两交付包60文件SHA保持一致。只保留一套日常工具链与最多一个必要隔离目录，旧重建路径已清理，原结果保留。见[清理记录](testing/evidence/m1/storage-reclamation-20261024.json)。

**最新生产组件检查点（WIP，本地提交07cab95）：** debug `77636c33` / test `712e60e9` 已接入结构信息异步删除。Pixel与API36模拟器同哈希 full探针及controller组件PASS：敏感原5字素5/5、6种敏感metadata6/6、正文getter0/结构请求11；21个结构故障案例均PASS，无key fallback。77 state/49 app JVM、40工具与debug lint/构建PASS，实际debug权限/备份/资产/零字体审计PASS。系统cursor-anchor由合成宿主桥接，真实系统IME与多宿主未运行，因此R04整体仍WIP；旧4/5 FAIL记录不改。见[本轮记录](testing/STRUCTURAL_DELETE-20261024.md)。下列738a365及release582016c3是此前已冻结制品，不包含此次生产修复，不能继承本轮结果。

**M1—M4 WIP，未达到发布门禁。** 此前完整制品源码提交738a365，已补齐固定NDK LLVM runtime/toolchain原文：44份离线正文，实际运行时122项。当前debug `b0ca3dd5`，优化且非debuggable本地调试证书release `582016c3`，默认unsigned `22f2f226`；完整SHA与制品/26条分项记录见[当前检查点](testing/CHECKPOINT-20261023.md)。开发记录。

49 app/73 state JVM、38工具、两套lint/构建和三APK实际零字体/权限/备份/资产审计PASS。两设备44项许可hash/NDK完整正文显示PASS；同debug原生跨120分页/11宿主故障、controller/settings/resources/R08/R09/R10/预准备无学习重启分项PASS，**full仍因R04敏感预存组合音标4/5而FAIL**。同release两设备五引擎scope PASS，专用离线模拟器新装引擎组件PASS，不是完整系统IME D05。

738a365隔离新源码/build/.cxx、warm固定SDK/JDK/Gradle输入重建整包22f2f226一致；96d源码另有新SDK/JDK/初始空Gradle cache整包复现证据。旧94b9d40f的各30分钟JNI观察独立记录，不继承为当前完整性能成绩。系统IME未由用户启用、iPhone原图缺失、R04未修复、完整安全/性能/正式签收未通过。[逐需求验收账](ACCEPTANCE.md)与[阻碍](OPEN_ISSUES.md)保留具体未交付项。

R04调查追加：[宿主委托](testing/HOST_GESTURE_RESEARCH_PROTOCOL.md)两设备各36/36、[结构坐标](testing/HOST_BOUNDS_RESEARCH_PROTOCOL.md)各18/18合成案例通过，IC正文getter0；第二类确实新增结构请求，未接生产私密路径。原editor保持4/5 FAIL；这不是R04修复或完整隐私通过。隐私策略规定“仅禁止文字正文，必要结构信息可以使用”；已记录该定义，生产异步删除与会话/超时防护已通过上述组件测试，尚未关闭系统R04。

以下是历史检查点及追加结果，最新制品不可继承旧哈希成绩。

日期：2026-10-23（America/New_York）。M1兼容性与M2独立纯组件开发检查点，**不是应用完成或可日用输入法**。本地前一提交`d9a8466`，本轮补丁见Git与下列记录；开发记录。

| 项目 | 真实状态 | 证据/范围 |
| --- | --- | --- |
| Trime固定递归源码与构建 | 探针PASS | 22来源/4928文件；基线、debug、test、unsigned release构建 |
| 真实引擎九键/多音节/部分选词 | host/Pixel/模拟器探针PASS | 3组合成词、部分选择保留组合、最终完整且仅一次提交 |
| 普通学习正对照 | 单词探针PASS | 非首词20次后2→1，重启仍1；不是R09十词验收 |
| APK权限与备份XML | debug/release静态PASS | extended-debug-apk/extended-release-apk；不是目标OS备份/迁移验收 |
| 首装离线 | API36专用模拟器探针PASS | 原d04f618e首次安装；新扩展版本为回归，不能混用条件 |
| 纯状态/触控组件 | 49项JVM PASS | U01—U04/U06部分覆盖，无真实IME/宿主/Android视图验收 |
| Android JVM与lint | 41项JVM、debug/release lint PASS | 含6项敏感元数据策略；保留lint警告 |
| 无学习原生配置 | host/Pixel/模拟器PASS | 独立配置20次选词、无userdb、输入到退出的完整profile内容/时间戳不变 |
| 真实EditText宿主组件 | 部分PASS / R04 FAIL | 去重、预编辑替换、断线恢复、正常字段6组删除、6种敏感元数据零上下文读取通过；隐私预存组合音标仅删音标，保留失败 |
| 完整密码/隐私零写 | NOT_RUN | 当前仅组件/无学习配置证据，不是完整IME的I05/SEC04 |
| 398MB模型 | host格式/查询PASS，未打包 | 4固定查询；授权边界与Android加载/性能仍待完成 |
| iPhone/Pixel参考原图 | BLOCKED | 无附件；M5视觉不验收，不声称1:1 |
| 完整IME/质量/上下文/性能/发布 | NOT_RUN | M1完整资源与隐私门禁未关闭，M2集成至M6未完成 |

最新记录：[M1真实执行](testing/M1-20261023.md)、[M2独立组件](testing/M2-20261023.md)。最终扩展debug SHA256 `7ddd343a07b363e33e74ef2383f7afd273e05b64a6e9ec8eb8e7e06ab4ddba23`；unsigned release `ad4778853a2c1c340c317ba35c8ea168f32b36bccd119e4626c15ee65e8a0cfd`。它们是开发探针；release未签名/运行，不能当M6交付。

接续：先补M1学习关闭/敏感零写、模型授权及Android加载、资源与传递许可闭包；同时完成允许独立验证的纯状态/提交协议。测试设备应连接且保持解锁；不得启用/切换日用默认键盘。参考图缺失保留M5阻碍，继续独立可做任务。

第三检查点：原生probe-2测试APK SHA256 `632bdcc7b0035232bcf6863bd9d8e72bcf13963509251dfb5d51542aba8aadca`，native-only在Pixel/模拟器PASS，editor-only明确FAIL，不混为全项通过。后续提交边界重入修复已通过49项state JVM；检查点当前构建的debug `7e780cb45f2d3d131a8a34a918e52734a3bbeae4f6366e1ebb35c50468f4970d` 与unsigned release `2c347994661891a8ccb9cc5a8a9295164a75854605d5ea03d2fd3916ea104614`仅静态APK及构建/lint通过，尚未绑定该最终哈希重跑设备测试。见[未解决项](OPEN_ISSUES.md)。下一步优先评估构建时预编译词典，减少每个新profile重复编译的耗时；不放宽R04私密字段断言。

第四检查点（取代上段“当前最终哈希”）：probe-3 debug SHA256 `13e97020102c69b09a44f0353e4510b39f9077c58c5dc12326ec263d1ab41b32`，已在Pixel及模拟器运行native-only PASS、editor-only仍FAIL；同一debug哈希实际APK静态权限/备份PASS。unsigned release `f95dc8ab7991e72c637dbbde7ebdfb6bc4a105384792cfd3d565e6edd6d66ddb`构建/lint/静态PASS，未签名或运行。无用户库的六个固定公共table/reverse改为构建时产物，原始词典不随此探针重复打包；ZIP24390180 bytes、16文件、SHA daec81fe，两个fresh staging重建字节一致。Pixel一次debug双profile探针13005ms（资源部署1190ms、首profile引擎ready4293ms）；模拟器干净卸装后离线首装重测3906ms（497/1291ms），不是P02二十次release成绩。

继续调查模型：固定lotem/rime-octagram-data 20260712 / 8ceef1b、约41MB compact词模型，README对命名gram数据给出LGPL许可指向。下载完整性与格式加载通过，原万象专用4查询只匹配2项；真实host grammar引擎的学习探针FAIL（即时排名2、重启1），需控制前文/重置会话验证。此候选未打包、未被认定达到R08/R10；原万象模型的许可疑点未自动解除。下一步先查明此测试混杂与原生session重置边界，再进入完整IME；R04私密组合音标FAIL不变。

第五检查点（当前制品，以本段为准）：probe-4 debug SHA256 `05da473956b8db4f5708d3a699c90e2a9a3b5ccfa13c3faa1d224edfc6867a73`；unsigned release `0e29f12e7f5a71a86d623ef5962b88c40a06afe5aae6ee44d67279ba28357ee0`。最终两APK静态权限/备份XML PASS，45 app/49 state JVM及两套lint PASS，release未签名/运行。原万象398MB模型仍不打包，另用有命名数据许可证据的lotem compact 40925228字节LGPL数据模型，来源、哈希、NOTICE和模型单独部署预算均固定。完整分发对应源/许可闭包仍是M4/M6工作，不据此宣称完整合规。

同一05da4739 debug在Pixel和专用离线首装API36模拟器native-only PASS：真实grammar加载、Lua、三组多音节/部分提交、正常学习和无学习对照均通过。已证明clearComposition保留20条提交历史；新增native session reset真正清掉历史/property，学习排名在无前文的新会话控制下2→1、重启1。原未控制前文测试FAIL保留；并发新会话对照排除“关闭DB才刷新”的解释，未降低原排名断言。见model-final-*-native及compact-*-control.json。

资源损坏恢复同哈希两设备PASS：独立篡改本项目公共资源/模型即拒绝，隔离重建固定数据；已有profile全部字节/mtime不变（Pixel15个、模拟器2个），同进程重新打开引擎成功。不是清除学习功能或完整备份验收。Pixel一次debug双profile14398ms（资源1546ms、首profile引擎ready4224ms）；模拟器离线首装4315ms（652/1273ms），不算P02 release20次成绩。editor-only两设备仍FAIL，敏感预存组合音标4/5，不能掩盖。当前仍是探针，无IME服务；继续M1许可/可重建与M2受控串行适配组件，M5缺图BLOCKED。

第六检查点（当前制品，以本段为准）：M2临时键盘与薄服务已编译，debug `3556212b2bf9c37b2c9ccb6e32d0f5eed5ad2f33d7b28b05d6a3b3e94d8b3aa7`，unsigned release `a647402ed38dfff9a2218228435943f7e40be65882f9a155264d110f8f022075`。48 app / 61 state单测、debug/release lint与构建PASS，两实际APK静态权限/备份XML PASS。忽略冻结artifacts/m2/m2-*及SHA256SUMS。未启用/切换系统IME，服务生命周期和多宿主仍BLOCKED/NOT_RUN，不称M2完成。

串行controller新增回调/输入屏障、128事件有界内存队列、同字段显式宿主重试、同步切字段中止旧标点尾巴；单workerJNI适配持全局租约（含私密预留，不调用native），探针/修复与实际会话互斥；敏感元数据由EditorPolicy。视图用同一HitMap绘制/触控，候选带session/revision/index；布局更新取消旧候选触控但保留静态长删，重复删不在异步屏障后积压。注册服务受BIND_INPUT_METHOD保护，未启用；复用Trime LifecycleInputMethodService/JNI，避开旧前端默认prefs/daemon/剪贴板和共享存储初始化，见ADR024。

同3556212b debug在Pixel与模拟器view/controller PASS：真实MotionEvent五键快输、滑出取消、一次提交/空闲标点、确认追加标点、取消队列、私密零上下文读取/正常profile不变、探针互斥，以及长删切字段后停止。view触控首轮受阻、第二轮因测试重挂EditText未恢复焦点而DEL不生效；保留两轮FAIL后修复fixture焦点，原断言不变。full scope两设备**FAIL**：native部分PASS，editor部分仍为R04预存组合音标4/5。资源损坏恢复同哈希两设备PASS。View.draw只渲染自有合成键盘的PNG位于忽略artifacts/m2（不是系统截图/参考对比）。

另已在测量前冻结R08 100词/20句合成语料quality-v1 SHA256 `6d44fe6d9e9c1e8f9c40ffe535ccefd71c0943845bd1b45cde1d4f6452512566`，暂未执行/未报告质量成绩。继续M3独立引擎测量、M4审计/清学习与设置；等待系统设置启用的系统IME和缺失原图不冒称通过。

第七检查点进行中：本地protocol冻结提交28fabd5与41d6795。f13dc431主APK / e74c9023测试APK，R08固定100词20句在Pixel/模拟器通过（Top5 99/100、19/20，整树零写控制通过）；R09十词各20次学习组件两设备通过（10/10 2→1，restart1，clear2）。详见M3记录，R10/系统IME/完整安全仍未验收。后续为严谨模型on/off对照，正在将loaded getter改成无加载副作用的检查并加纯计数查询证据，新APK未绑定全部旧成绩。旧R04失败与视觉阻碍不变。

第七检查点最终（当前）：debug43cf6e699a83e89d2e367a122780350ab45e74552c94a461a7e2e243e45eb531 / test fc0c5fa34c41c012d539882449edc684e87b03dd6e563e1b9f79b6528aec66ff。同哈希两设备R08 Top5 99/100和19/20、R09十词2→1/restart1/clear2、R10上下文9/10改善及model-off不变，全部原控制断言通过；view/controller与损坏恢复同样两设备PASS。48 app/61 state JVM与两套lint PASS；unsigned release73a2031d仅静态/构建PASS。full两设备仍R04 FAIL，不能总括通过。第一次Pixel full安装ADB超时已标BLOCKED，按设备实际APK SHA匹配复用后重试执行，原R04如实复现；不是锁屏或替换测试结果。详见M3记录。

下一步独立M4工作：接入已测的停止引擎/私有路径清学习按钮、离线来源许可界面、固定运行时清单与入口/持久层审计。服务未由用户启用，系统生命周期与多宿主继续BLOCKED/NOT_RUN；M5参考图与体验确认BLOCKED。不得用组件成绩宣布整体开发完成。

第八检查点M4进行中：离线43许可原文/124确切运行时制品、清学习显式确认/取消路径、严格资源白名单与持租约修复已接入。Lua仅允许固定内置模块，移除IO/OS/package/debug等标准库及外部rime.lua；两设备policy/R08/R09/R10/controller/settings/resources分项PASS，full仍R04 FAIL。UID合成canary两设备0命中，只覆盖自己UID缓冲区；native本机路径标记0，release旧前端/daemon/同步/剪贴板已裁剪。49 app/61 state JVM与两套lint PASS。详见testing/M4-20261023.md，多个制品分别绑定，不把旧结果归到新哈希。当前正验证显式本地签名、优化且非debuggable的release引擎，默认release仍unsigned；尚无release运行成绩或完整安全通过。系统IME未启用、参考图缺失，不称开发完成。

第八检查点最终追加：release94b9d40f/test31c08d3f在两设备原生引擎、固定质量/学习/上下文及资源恢复PASS，专用离线模拟器卸装后首装同APK native组件PASS；release旧前端roots仍未带回。debug9cdf03c8/test65eb60dc两设备完整回归native PASS而R04 FAIL，分项settings/resources/R08/R09/R10/controller/canary PASS，app UID canary前后0。49 app/61 state JVM和两套lint PASS、32工具测试PASS；默认unsigned release重建/对照正在收尾。M4依旧WIP，未启用IME与原图缺失不被组件成绩替代。

隔离源码重建发现新构建缺陷：b4fb49d在全新源码/build目录Gradle成功但APK3MB缺四运行时资产，整体FAIL且未安装。正在把固定资源生成接入preBuild，并让实际APK资源hash进入静态审计；原M4原目录已运行制品结果不受篡改，新隔离重建不能宣称通过。详见testing/REBUILD-20261023.md。运行时ZIP内容保持原锁不变。

隔离重建修复已本地提交2ad1de3：从该commit归档、全新源码/build/.cxx重建62任务后，62444813字节unsigned APK整包SHA与bbd28866基线完全相同，实际资源/权限/备份审计PASS。仍共享已固定SDK/Gradle/公共table和model缓存，完整M6干净环境获取未验收；见REBUILD记录。性能v1/v2混入session冷启动的短测原始成绩保留，v3连续同字段协议已在测量前提交6ae38b3，尚不据此宣布性能通过。

宿主异常边界追加WIP：专用模拟器真实EditText 11故障最终相同test d35ef8d9，从1/11红测到b0343481 debug 11/11绿测。开始batch/查询后重核字段，异常失败关闭、结束batch不撤销已接受commit；full继续包含此组件和原R04，native/故障组件PASS而R04 FAIL。controller/settings/resources/R08/R09/R10同b0343481模拟器PASS。当前Pixel仍冻结94b9d40f/ee19b44e作长期组件观察，未安装新宿主版，所以新宿主版本真机回归尚未运行。最终APK字体逐字节匹配已核字体许可，未知字体hash新增拒绝。详见HOST_FAILURE记录；不宣布完整安全或完成。
