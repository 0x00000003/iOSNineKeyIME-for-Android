# 当前状态

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
