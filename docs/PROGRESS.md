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

## 2026-10-23 — M3质量/学习受控测量（WIP）

quality-v1在13208fe冻结，learning-v1在28fabd5冻结，context-v1在41d6795冻结，均在测量前固定。R08首轮虽99/100与19/20命中达标，但整树mtime控制FAIL；定位到测试反复select同schema保存user.yaml访问时间。只核验自动选中schema后原零写断言保持并通过，两设备得分不变。R09同f13dc431/e74c9023 APK十词全部2→1/restart1/clear2，清除拒绝引擎占用、私有兄弟symlink不越界均通过。完整SEC04/R04/系统IME未通过，不把组件当完整交付。

R10四格对照测试进行中；发现旧loaded getter的GetDb有加载副作用，改非加载getter并增加无文本query计数，需新APK回归。详见M3记录，保留所有失败历史，按贡献流程提交。

M3最终追加：43cf6e69/fc0c5fa3同制品在两设备R08/R09/R10及触控/恢复全部组件PASS，R10 9/10真实排序改善、off不变/0查询、on5110查询；无用户库/全profile不变控制保持。full仍R04 FAIL。Pixel重复安装首轮超时记BLOCKED，按当前已装APK+test实际SHA相同才复用重试并执行full，不按版本号跳过，也不声称首装。48 app/61 state、两套lint与实际两APK静态门禁PASS；release73a2031d未签名/运行。下一步M4独立界面/清学习/清单，按发布流程管理。

## 2026-10-23 — M4独立加固（WIP）

前置0da82a5。新增固定离线许可/确切Maven POM及parent哈希清单、清学习确认取消测试；保留首轮AlertDialog队列时序FAIL后修正fixture，43正文均校验。Lua固定内置模块/削减标准库、未知资源与symlink拒绝、持native租约恢复，真实两设备分项PASS。合成私密canary两UID日志计数0、不落原文，native构建路径去除。R8旧listener Keep roots定位并移除后旧daemon/同步/剪贴板不再出现在release中，worker失败而非假成功。49 app/61 state单测及lint通过。32项工具测试仅主机工具边界，不代替Android。

详见testing/M4-20261023.md与ADR028—031。正在接独立优化release引擎验证，前两次源集配置构建失败保留。M4整体不勾选；系统IME/多宿主仍未运行，R04敏感预存组合音标FAIL，参考图BLOCKED。没有默认键盘或Pixel网络/锁屏更改、没有远端推送。

M4优化release追加：第三次源集构建成功，首轮两设备test入口因target R8合并Regex ABI而启动崩溃；保留FAIL后以编译协议constant pool确定少量Kotlin ABI保留规则，普通/本地签名release一致，未关闭优化。最终94b9d40f release/31c08d3f test两设备引擎、R08、R09、R10与资源恢复PASS，静态权限/实际备份XML PASS、旧前端roots未被带回。专用模拟器同94b9d40f离线卸装后首装native组件PASS，Pixel数据未清。default release仍unsigned，不把本地调试证书验证当正式发布。最终debug9cdf03c8另做完整回归，R04失败继续保留，详见M4记录。

M4本地WIP提交b4fb49d已落。后续先冻结JNI独立性能协议，再运行10秒smoke与30分钟组件观察；不替代P01—P04。并从b4fb49d git archive在项目内全新源码/build目录重建，复用固定SDK/Gradle/资源缓存和本项目调试key，不能称全干净工具链重建。

隔离b4fb49d重建实际暴露FAIL：Gradle成功不等于完整APK，新包缺手工生成的四个资产。保留3MB失败制品与补强审计失败证据，接入fresh staging资源生成/全哈希preBuild，工具34项PASS；资产ZIP仍a51758a5不变。ADR032及REBUILD真实记录已写，后续再隔离重建，不把它写成M6通过。

M5探索短测保留：v2总P95约162ms，分段processKey约161ms，候选读取约5ms。检查固定JNI reset实现确认每词reset销毁session，下一词首键混入冷启动；协议v3测量前冻结为warmup后连续同字段（不重启session），显式记录该条件，原失败预算字段不覆盖，不修改产品或降低阈值。完整P01/P02仍未验收。

2ad1de3隔离源码重建补测PASS（同机共享固定缓存）：62任务全部执行，3m8s，unsigned整包bbd28866与原基线完全相同，补强APK资产/备份静态检查通过。首轮3MB缺资产FAIL不覆盖。完整SDK/cache初次获取、系统IME和视觉/性能门禁继续未关闭。即将冻结性能APK树并通过run_probe --apk-root绑定，避免其他构建改变长测两组被测版本。

长测后台期间检查宿主连接边界，先在专用模拟器真实EditText注入11组异常/同步切字段；初始1/11通过，保留红测。发现batch begin切字段后旧commit虽最终标STALE仍会改正文，及batch end异常会掩盖已接受提交，准备修复。Pixel冻结长测不安装任何新包；原R04失败不受本项替换。

宿主异常修复专用模拟器绿测：同最终red测试d35ef8d9，主APK b0343481，11/11通过、敏感上下文0；49 app/61 state及debug lint PASS。批次begin/查询后重核字段，捕获连接异常，endBatch不撤销accepted commit。原R04不改，11组组件继续加入完整scope必测；Pixel仍运行冻结性能长测，本轮未在Pixel安装新宿主版，不冒称真机回归已通过。

宿主修复完整模拟器回归完成：b0343481的native/11故障组件/controller/settings/resources/R08/R09/R10 PASS，full继续原R04 FAIL。实际font由最终APK提取hash核对固定MaterialDesign-Webfont许可，debug/release均匹配，未知字体新增静态拒绝；工具35项PASS。Pixel长期组件尚在冻结旧版94b9d40f，未冒称新宿主修复已真机通过。

M6独立环境准备：项目内233个APK中188份同哈希副本改硬链接，所有路径/字节/证据保留，释放约9.3GiB，余量约24GiB；无外部目录清理。固定官方SDK/JDK归档重新下载并核原publisher SHA1或SHA256，追加本地SHA256锁，独立SDK各文件/内部symlink核归档并新建Gradle cache进行2ad1de3隔离构建。共享仍仅公开词库/table/model缓存和同固定条款的既有SDK许可接受标记；未用签名key或adb。此时构建尚在进行，不提前宣称通过。

字体许可hash核对后进一步检查实际AAR枚举，存在品牌命名常量（不是原始Apple作品的证明）；自有前端本就只用系统TextView，因此准备删除未用字体依赖，并收紧实际APK审计为零打包字体。保留历史字体许可证据，待真实构建和新依赖锁复核后再写已移除；长测仍绑定原94b9d40f历史版本，不能作为未来字体移除版本的全门禁成绩。

固定94b9d40f/ee19b44e的model on/off各30分钟JNI组件均实际结束：on14340键2264次exact-once，off14353键2266次；合计P95/P99分别12.096/15.613ms、11.128/14.681ms，31×2内存样本、thermal全0、own UID流量0、profile不变；未替代UI/完整P01—P04或后续代码版本成绩。新SDK/JDK/空Gradle-cache历史2ad源码重建经历真实TLS失败后原规则重试成功，整包bbd28866再现。当前在补齐所有用到的variants/包含build锁，保持verification，未白名单被拒绝的UTP。

候选分页原100上限正在改真正原生20项窗口+lookahead，全局ID/offset+epoch约束，不消费commit，当前71 state/49 app及lint/构建PASS但Android跨100测试仍待测；M2继续WIP。字体依赖移除与严格锁模式一并复核，完成前不归到旧哈希。

最新WIP收尾：debug dd8f688f/test68bc0af2两设备full强制包含跨120分页与11连接故障，两组件PASS、R04仍4/5导致FAIL。settings/resources/R08/R09/R10/no-learning-restart/canary-controller两设备同哈希PASS，UID canary前后0。optimized dedeee3c/testee19b44e两设备五引擎scope PASS；unsigned9cdd4df2与signed23个非签名ZIP entry相同，三APK实际零字体/权限/备份/资产审计PASS。49 app/73 state、38工具、两套lint及构建PASS。历史新SDK整包重建和两次30分钟旧APKJNI观察独立记录；不继承性能/安全成绩。详见CHECKPOINT-20261023；M1—M6未完成门禁保持未勾选，本地WIP提交。

96d627a代码检查点后：再次实查Pixel仍Awake，own IME未启用/未默认（仅保存布尔，无原始IME列表）；专用离线API36模拟器只卸载本项目合成release，断网无默认network，重装同dedeee3c引擎组件PASS。系统IME/D05/P02整体不因此勾选。当前96d源码新SDK/JDK/初始空Gradle cache已完成7个包逐文件核验（JDK459文件），正在构建，尚不预报整包一致。

后续M4许可复核发现NativeBaseConvention显式静态C++，原43正文缺固定NDK的LLVM runtime/toolchain NOTICE；已从已核归档提取原文、比对SDK字节并登记source/archive/member/SHA，准备44项离线正文。Settings数量断言相应更新，其他断言不改，新APK尚未构建/回归。当前96d新工具链重建仍在独立目录运行，不将它误归到新增正文版本。

96d627a全新源码/SDK/JDK/初始空Gradle cache首次实际重建结束：62/62任务、12m34s，unsigned整包9cdd4df2与冻结当前基线完全相同；同历史source43正文auditor复核PASS。该目录与后续NDK44正文补丁分开，后者正在本项目根构建；未把历史结果归到改资产后的包。

738a365补齐NDK原文后新哈希收尾：debug b0ca3dd5/test1f988fd6，release582016c3/testb1948467，unsigned22f2f226。26项记录已核：两设备debug full仍R04 FAIL、其余分项/44项完整原文显示/UID canary0通过；两设备release五scope通过，专用断网新装组件PASS。49 app/73 state、38工具、双lint与三实际APK审计PASS。新源码warm输入62任务3m22s整包22f2复现。代码/文档本地WIP，不关闭外部图/系统IME与R04等门禁；正式交付未完成。

R04继续独立调查：冻结HOST_GESTURE_RESEARCH_PROTOCOL，拟用公开API字符级删除委托在自有普通/真实密码遮罩EditText测36个案例；几何仅test oracle提供，不接生产私密路径、不读IC正文、不替换原R04门槛。先看宿主实际行为，再评估是否有可行路径。

宿主删除委托第一阶段实际两设备36/36，通过真实密码遮罩、组合音标/多个mark/ZWJ/RTL等合成案例，IC正文getter0；只用test Layout几何，不是生产修复。原editor同test仍R04 4/5 FAIL。下一步单独冻结结构元数据（非正文）坐标调查，明确不是零结构信息请求或生产私密策略验收。

第二阶段冻结HOST_BOUNDS_RESEARCH_PROTOCOL：仅自有合成字段用TextBoundsInfo结构坐标（一次请求并Parcel往返）代替字符Layout oracle，正文getter仍拒绝；明确结构查询非零，生产私密策略/R04不改。先测18案例，再判断最小生产请求区域与跨字段异步是否可做。

第二阶段两设备各18/18 PASS（testc6c6e8bf/mainb0ca不变）：通过一次结构坐标请求/Parcel往返而非字符Layout oracle完成每个合成删除，正文getter0，结构请求18，已知marker未观察到。同test第一阶段各36/36；原editor仍4/5 FAIL。生产HostAdapter及严格私密策略未改，下一步需明确“不得读上下文”是否包括这类不含正文的结构信息，并在系统设置启用IME后验证最小区域/异步/多宿主。

合成prototype完成后，结构信息隐私边界进入ADR037评估；生产路径尚未接入新API，原R04两设备仍FAIL。其他系统IME/原图/目标OS门禁和WIP制品/源码/测试记录已分别保存。

隐私策略规定：仅禁止文字正文，必要结构信息可以使用。已将定义写入AGENTS及ADR037，采用该隐私边界；继续生产HostAdapter/Controller异步删除及字段/超时/不支持处理。R04原失败继续保留，待实际新路径与系统测试，不将18例研究当作完成。

构建缓存去重检查发现重复隔离目录、未使用模拟器和安装归档。回收9处本项目忽略的可再生成目录，可用空间实际增加24.10GiB；保存各重建receipt/log，所有已冻结APK/测试/源码/未提交编辑保留，两份交付包60文件SHA逐项复核PASS。不将缓存清理当Android测试或R04修复。

磁盘后续修正：run_probe改为已核SHA的单实体APK池与每记录硬链接，Gradle输出隔离inode、既有路径不覆盖、失败无partial遗留；40项tooling通过。后续同APK分项测试不会再按record复制大包。

采用正文禁读、必要结构可用的隐私策略后，生产HostAdapter/Controller/薄服务接入结构异步删除：当前09f88435/f5652c74两设备full探针/controller PASS，原敏感5字素5/5、metadata6/6、正文getter0/结构请求11、19故障全部PASS。3个顺序红测真实失败后barrier修复，追加end同步回调epoch保护，77 state/49 app/40工具、debug lint/build/实际APK审计PASS。旧DEL4/5 FAIL保留，结构桥接不算系统IMS验证，真实IME/多宿主/全安全及参考图仍未通过。默认unsigned release构建/lint正在进行，不提前报告结果。

收尾增量修复HostAdapter取消回调同步重入：先失效连接/清理，再取消，begin取消后重新核session。21案新test712e60e9配旧09f88435在模拟器真实红测reentrant_begin FAIL（前19 PASS）；同test配新77636c33两设备full 21/21 PASS，正文getter0，原R04字素5/5。默认unsigned02c7f870仅构建/lint/实际APK审计PASS，未签名运行。77 state/49 app/40工具与两套lint通过；系统IMS、多宿主、正式签收/原图仍不宣称完成。

本轮生产结构删除WIP代码/记录已本地提交07cab95；磁盘回收2b71556、同APK记录单实体保存c8d89fe。最终再测项目占用仍28GiB量级、系统可用26GiB，未再次生成工具链副本。当前测试APK与源文件摘要均绑定该代码检查点，系统IME/视觉/完整发布门禁未关闭。

继续开发：工作区初始干净df10d4c，唯一在用工具链，余量26GiB。两个本项目IME均已启用；查得选中旧582release，已切当前776debug并验证，无首次启用绕警告。增加独立UID:testhost，STRICT锁/既有依赖验证、debug宿主与test/lint构建通过；只新增约11MB构建输出，不复制SDK。两设备readiness16/16（保留API37附加flag导致旧预期FAIL与诊断）；真实Pixel core尚FAIL，5键已注入但图中仅26，正在定位served字段重启与触控交错；不称M2通过。原图仍缺。详见testing/SYSTEM_HOST-20261023.md。

本轮系统核心最终befe66f4真实Pixel10/10、敏感字素删除36/36 PASS（独立UID，无坐标回调桥）；修复导航inset/父测量并用实际窗口稳定门控触控。IME导航控件高度不同于宿主手势条，错误等值断言的FAIL保留；最终协议保持上屏/动作次数原断言。两设备full组件PASS，四布局×四inset16/16，50app/77state/42工具、两套lint/实际APK静态审计PASS；新增testhost lint通过。未覆盖系统敏感正文getter拒绝、异步删除期间连续输入/生命周期/全安全和原图，继续WIP，按贡献流程提交。
