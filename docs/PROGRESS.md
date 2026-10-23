# 开发进展日志

## 2026-10-23 — M0 初始化
目标：建立离线Android九宫格输入法工程与可复查开发流程。

完成：创建工程目录与文档；通过上游API与固定commit源码核对Trime、万象、librime和模型数据仓库；建立全部规格与文档、来源/许可快照和依赖研究锁。没有访问同级项目内容。

关键发现：Trime清单默认allowBackup=true，派生必须改；Trime稳定版的librime子模块与1.17.0一致；万象独立九键方案存在且指向wanxiang_lite；模型与词库/工具代码不能按同一授权概括，data仓库明确限制原创程序代码；简体gram候选约398MB，未下载/未实测。

当前结果：初始化结构检查0错误、checker工具13项自测通过，主机库存与敏感标记扫描已有脱敏证据；仅初始化，不是应用交付。具体结构/工具检查及Git结果见 [INIT记录](testing/INIT-20261023.md)。无APK构建/安装、无设备操作、无Android单元/真机/截图/性能/安全行为测试结果。

下一步：M1兼容性探针→M2状态交互；补参考图/录屏、确认目标OS/API后再验收视觉。未验证项不标PASS。后续每轮追加记录，不覆盖本段失败或待办历史。

## 后续日志模板
时间/里程碑/目标 → 修改文件 → 测试记录链接及真实结果 → 失败/解决 → 新决策/风险 → 本地commit（下轮引用） → 下一步。

## 2026-10-23 — M1 进行中（WIP）

从干净的5d5670a开始。导入固定递归Trime来源，项目内安装工具链，完成上游arm64 debug基线和macOS真实九键/Lua/提交/退格探针；新增安全解包、资源依赖扫描与失败回归测试。独立包名、无网/禁备份的Android探针正在编译；尚不提供可日用键盘。设备只读库存确认测试Pixel，未切换输入法。初步资源/模型哈希和逐文件清单已记录，完整许可审计未关闭。

真实失败和重测路径见[M1执行记录](testing/M1-20261023.md)。M1尚未通过，不进入UI视觉验收；后续继续Android探针和原生状态/学习兼容性。

M1追加：独立探针APK实际权限/备份XML检查通过；Pixel第一次JNI getter崩溃已定位修复，同设备重测100次getter及九键/提交/退格通过。对应d04f618e制品已保存在artifacts/m1，测试绑定同哈希。测试设备重新连接并保持唤醒；没有切默认键盘。上游35项JVM测试暴露过期API/标量字体兼容问题，保留失败历史并修复中。完整M1门禁仍未关闭。

M1检查追加：35项Android JVM测试最终全部通过、0跳过，摘要见testing/evidence/m1/android-unit-summary.json。原生probe APK静态门禁与Pixel行为均已存在真实记录；lint/release仍继续，M1整体WIP。

M1来源检查点：debug/release最终metadata构建及实际APK静态门禁通过；API36专用模拟器首次断网安装探针通过。修复导入时23项Git换行转换，4928文件index字节核对通过；本地改动有独立哈希记录。整体M1仍WIP，不勾选完整学习/模型/隐私/性能验收。

## 2026-10-23 — 扩展M1与独立M2纯组件（WIP）

前一检查点已本地提交de7b919。真实host/Pixel/API36模拟器新增三组多音节、部分选词和单词20次学习/引擎重启正对照PASS，debug绑定7ddd343a；同补丁41项app JVM、38项state JVM、debug/release lint及两变体静态APK门禁通过。模型本地只读格式/4查询通过，但未打包/授权关闭/Android性能。记录见M1/M2-20261023；全部门禁范围分开。

失败保留：state新传递依赖pin缺失；敏感Cancel错误触发native Clear；模型host生成头include缺失。逐一修复后重测，不删除断言或减低门槛。新增TouchProtocol共享几何/字段取消，EditorPolicy覆盖密码与无学习元数据。尚无实际InputConnection、真实IME或完整隐私零写证明；M1、M2均WIP，M5缺参考图BLOCKED。下一步完成M1剩余原生/隐私/资产门禁与纯提交控制协议。

## 2026-10-23 — 无学习真实对照与宿主边界（WIP）

前一提交a21c873之后，host/Pixel/API36原生probe-2正常学习/无学习对照通过，源包哈希188fd5c7。默认先创建无学习schema，普通学习显式选择，避免先加载正常用户库。独立profile输入到finalize的完整内容/mtime不变、无userdb；不是完整IME R07。最终49 state/41 app JVM、debug/release构建与lint通过。

真实AndroidEditText组件测试：去重、预编辑替换、断线恢复/切字段、普通字素/选区及6敏感元数据零上下文读取通过；私密预存组合音标仍FAIL（4/5），不降低门槛。修复sendKeyEvent返回值解释、测试UI异常传回、字段fixture顺序、提交flush同步重入；失败记录全部保留。细节及不同APK哈希见M1/M2记录和OPEN_ISSUES。

接续先评估固定数据构建时预编译，减少每个新profile重复编译；M1许可/完整数据、R04私密删除、参考图等未关闭。没有完成可日用应用或1:1验收，不推送。

## 2026-10-23 — 公共词典预编译与替代模型调查（WIP）

前一提交065f5be。6固定公共table/reverse从两个独立host部署导出字节一致，不选userdb；runtime省略原始dict文本，保持解包预算32/128MiB。probe-3资源16项、ZIP daec81fe，fresh staging两次重建一致。debug13e97020在Pixel/离线API36模拟器native-only PASS，editor-only同样保留R04 FAIL；两APK静态、debug/release构建/lint通过。一次debug双profile探针Pixel13005ms（原源码编译探针135694ms）、模拟器干净卸装离线首装3906ms；未把单样本作为release性能成绩。忽略制品已冻结并有校验。

不把原模型许可当作停止所有独立工作的理由：另外固定Rime上游LGPL compact gram候选并下载验证，host格式加载成功，但原特定查询2/4、grammar学习即时/重启rank不一致导致测试FAIL。未打包、未宣称上下文/许可分发全部通过；下一步研究session清理与前文混杂，再评价替代模型。具体证据见M1记录。完整M1/M2门禁仍未完成。

## 2026-10-23 — 模型加载、会话清理与资源恢复（WIP）

前一提交bde02ac。本轮将明确命名授权的Rime compact模型加入固定运行时资产；未集成数据仓库工具。发现clearComposition不清提交前文，新增原生session重建与元数据计数，通过并发会话对照定位学习排名混杂。旧FAIL保留，新控制保持2→1/重启1门槛。最终debug05da4739在Pixel/离线首装模拟器native-only与public-resource-recovery均PASS；损坏固定数据可重建且不改既有学习库，editor-only仍FAIL。

45 app/49 state JVM、debug/release assemble/lint及两APK静态权限/实际备份XML PASS。工具链补官方SDK/JDK制品来源与固定版本检查；依赖清单补Lua/原生插件/字体/数据模型授权，修正librime旧版本。AboutLibraries联合导出触发Gradle任务输出依赖校验FAIL，保留失败并将CSV单独指定项目内输出目录后重跑PASS，未关闭校验。完整发布合规与干净重建仍未通过。详见M1记录第五检查点。无远端操作；下一步继续独立M2串行引擎/状态集成，不宣称M1全门禁或应用完成。

## 2026-10-23 — 串行控制器、临时键盘视图与服务（WIP）

前一提交d9a8466。新增纯Controller、全局native租约与单worker JNI adapter；真实五键在部署前进入队列保序，部分提交与后续标点沿唯一HostEditorAdapter输出，宿主阻塞显式重试并拒绝跨字段。重复删除不排队堆积。薄服务复用Trime生命周期基类及已测JNI；旧高耦合前端不注册，具体偏离记录ADR024。临时键盘有中/英大小写/数字/符号/候选展开分页、系统切换与隐私入口；参考图缺失仍BLOCKED，未声称iOS布局事实。

最终48 app/61 state JVM、两套lint/assemble、两实际APK静态门禁PASS。debug3556212b在Pixel/模拟器view/controller与损坏恢复PASS；full native PASS但editor仍R04 FAIL。实际触控首轮无fresh结果，第二轮定位到嵌入EditText失焦导致DEL无效，恢复fixture焦点后相同要求通过。新视图同帧重建后立即measure/layout匹配HitMap，测试不再依赖长按时waitForIdle。保留失败。未执行系统设置授权/默认切换，服务生命周期未验收；R04与M5未解。见M2记录及USER_GUIDE。

测量前固定R08质量语料100词/20句；不按后续结果更换目标或下调门槛。下一步运行host/Android固定语料与独立学习/上下文对照，继续完整安全/许可审计，开发记录。
