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

| 017 | 2026-10-23 | Accepted for probe | 缺省schema首先无学习，正常学习显式选；无学习探针用独立新noBackup profile并对输入至退出全文件内容/mtime比较 | Rime创建session会先实例化缺省schema，之后才select；不能先加载普通用户DB再称私密。host/Pixel/模拟器已测配置，但完整IME字段策略仍未验收 |
| 018 | 2026-10-23 | WIP / failing R04 | 敏感宿主删除禁止读取上下文，DEL结果标DELEGATED；真实效果必须另断言，不能信任sendKeyEvent布尔值 | Android正常ICU删除通过，私密预存组合音标FAIL；保留门槛与失败，后续解决，不用平台差异冒充P0通过 |

| 019 | 2026-10-23 | Accepted for probe-3 | 构建时编译固定公共词典的table/reverse，两个独立部署字节一致后打包；不带原始dict、动态prism/schema或userdb。沿用原解包预算与librime支持的prebuilt fallback | Pixel/API36实际兼容与静态共享数据不变通过；不是完整工具链重建或release性能验收；原始来源与许可仍保留 |
| 020 | 2026-10-23 | Research candidate only | 原Wanxiang模型许可疑点保留，另评估有明确命名gram数据授权指向的lotem compact LGPL模型（40.9MB） | 固定commit/资产/哈希/许可证据；host格式通过但原查询/即时学习断言FAIL，先定位会话与上下文，未打包，不降低R08/R10要求 |

| 021 | 2026-10-23 | Accepted for compatibility | 原Wanxiang LTS模型不打包，采用固定lotem/rime-octagram-data 20260712 compact词数据模型，明确README命名授权与LGPL许可证据；质量R08/R10不预判 | runtime独立模型SHA/size预算，完整分发对应源与NOTICE仍在M4/M6 |
| 022 | 2026-10-23 | Accepted | clearComposition只清当前组合，不能当作字段切换清历史；需要销毁native session并按隐私schema重建 | history/property/raw/commit均有真机断言；用户库不因此清除；未来服务与探针必须共享串行原生所有权 |
| 023 | 2026-10-23 | Probe boundary | 损坏公共资源隔离版本目录并从固定APK重建，不递归删除/改写学习profile；错误仅有限安全码 | Pixel/API36损坏+恢复+重新打开引擎通过；不能替代清学习、备份或完整IME安全验收 |

| 024 | 2026-10-23 | Provisional, refines001/010 | M1底座已在host/Pixel/API36实际加载模型/Lua与T9；M2继承固定Trime LifecycleInputMethodService复用生命周期/JNI，采用受测独立Controller/薄服务，不继承TrimeInputMethodService | 原服务onCreate无条件创建RimeDaemon、AppPrefs、主题/剪贴板/外部同步等路径，与最小默认Application不兼容。避免为临时前端重新引入共享路径副作用；不是重写Rime。旧前端保持不注册，M4继续剥离/audit。系统服务需用户设置启用后实测 |
| 025 | 2026-10-23 | Accepted for WIP | 真实IME和合成探针共用native排他租约；私密字段只持租约不调用native，普通关闭屏障完成后再观察零写 | 该屏障已组件实测，未覆盖系统IME全生命周期；不把普通待完成写入误说成私密输入写入或安全全PASS |

| 026 | 2026-10-23 | Accepted for engine evaluation | R08/R09/R10语料/协议必须在测量前本地冻结；只记录ID/排名，无真实候选dump；quality每项fresh session避免前文混杂，R10四格隔离模型/前文且禁学习 | 初始R08重复schema选择导致user.yaml访问时间变化，保留FAIL后消除测试冗余选择，零写断言不降。R09清除只删停止的私有指定profile、symlink不向外遍历 |
| 027 | 2026-10-23 | Accepted observability | grammar loaded检查不得调用GetDb造成检查副作用，改成只看已加载cache并计无内容query次数 | M3模型on/off对照已用0/5110次查询证明，旧getter的结果只作为数据可加载事实，不独自证明translator使用；需同APK回归 |

| 028 | 2026-10-23 | WIP hardening | 离线许可只加载固定hash资产，运行时清单以实际releaseRuntimeClasspath/POM/显式parent链为准，不用AboutLibraries父ID版本替代选中制品 | 43原文/124制品有锁和UI测试；POM声明不代表完整对应源义务已审计 |
| 029 | 2026-10-23 | Accepted restricted runtime | Lua仅固定内置模块，无用户rime.lua、IO/OS/package/debug/动态加载标准库；公共资源严格白名单、拒绝symlink、故障修复持native租约 | 真机/模拟器policy及损坏恢复通过；仍有Rime类型绑定，不称通用不可信脚本沙箱 |
| 030 | 2026-10-23 | Accepted fork reduction | 精确JNI/DTO keep；移除未注册旧前端listener的Keep roots，禁用旧后台同步worker为failure-only，保留ABI不伪报成功；native项目根prefix-map | release中旧前端/daemon/同步/剪贴板已裁剪，后续每次keep调整重新检查；不以符号缺席当完整隐私证明 |
| 031 | 2026-10-23 | Local validation only | 显式localReleaseProbe用本项目调试key签非debuggable优化release，默认仍不签；独立引擎runner拒绝完整IME/editor gate | 不是M6正式签名/发布；复用原协议/语料、双APK哈希与nonce，release实际执行后才能报告该制品成绩 |
| 032 | 2026-10-23 | Build fix required by actual FAIL | b4fb49d隔离源码Gradle成功但3MB APK缺四个运行时资源；把fresh固定资产生成/哈希核验接入preBuild，任何缺失/漂移失败，不复用未核验旧生成目录 | 独立源码构建必须核对实际APK资源闭包，manifest/备份静态PASS不能代替可运行。固定缓存输入可复用，仍不称SDK/cache全干净重建 |
| 033 | 2026-10-23 | Host boundary fix | 宿主beginBatch及普通查询后重核session/connection，旧字段重入在commit前拒绝；可判定host RuntimeException失败关闭且不日志payload，endBatch错误不撤销已被host接受的commit | 相同测试APK 1/11红→11/11绿，真实EditText oracle；不承诺host提交本身after-apply抛异常可无歧义重试，不读取敏感正文补偿 |

| 034 | 2026-10-23 | Accepted WIP build policy | 默认LockMode.DEFAULT不能拒绝缺状态；settings早期对root/included projects及plugin classpath设STRICT，补实际构建/test/lint配置锁。移除未用Material品牌图标集合，实际所有APK只允许系统字体、零打包字体 | 缺状态/重新加入旧依赖两实际Gradle负测拒绝；不用verification拒绝代替锁证据。依赖约束变化与124→122清单见TOOLCHAIN记录 |
| 035 | 2026-10-23 | Accepted WIP native paging | 以20候选+1 lookahead的原生窗口替代前100本地缓存；全局index、独立Fetch及匹配offset/revision，翻页不消费getCommit；空格确认global0，收起重取第一页 | 原单键6只有4候选，保留测试前提FAIL后使用完整音节64，在双设备跨120实际选择仅上屏一次；禁用键保留绘制几何，触控集合排除 |

| 036 | 2026-10-23 | Research only / production scope unresolved | 原私密DEL保留R04 4/5 FAIL。先冻结并在自有合成宿主测试字符级委托36例与Parcel结构坐标18例，分别记录正文getter0及结构查询次数；不将结构查询暗作“不取上下文” | 两设备分项通过，生产HostEditorAdapter/EditorPolicy未改；上下文边界在ADR037定义，在此之前按严格策略，不关闭R04。最小区域/异步/字段epoch/多宿主需另实现实测 |

| 037 | 2026-10-23 | Accepted scope clarification; supersedes036待定部分 | 隐私策略定义为“禁止文字正文读取，允许必要结构信息”。私密不得取正文/学习/持久化；允许必要字符坐标/边界等结构元数据按当前会话在内存最小使用，并单独计数 | AGENTS约束4补充定义；允许继续生产删除路径实现，不代表R04/I05自动通过。必须覆盖最小区域、异步队列、字段epoch、超时、不支持宿主和真实系统IME |

## ADR038 — 必要结构信息驱动的异步敏感删除（WIP）

依据ADR037隐私范围定义，接受API34+结构查询+宿主字符粒度删除作为生产候选路径。只取当前行内必要坐标、selection和matrix，最多32个尾部单元；不读正文、segment finder、composingText、不持久化元数据。引入Controller异步host屏障及session/selection/连接校验，未知结果不自动重试，无DEL fallback。非敏感ICU路径保留，敏感同步入口拒绝。

这是为修复真实组合音标FAIL做的架构改变，新增平台API但没有新增依赖/网络权限；固定依赖锁不变。21故障合成案例（含begin/end同步重入）和原R04生产组件已有新版本成绩；系统onUpdateCursorAnchorInfo与多宿主仍待用户启用IME，不能据组件成绩宣称P0关闭。详细协议/结果/边界见testing/STRUCTURAL_DELETE-20261024.md。

## ADR039 — 独立UID的真实系统IME合成宿主（WIP）

新增仅debug的:testhost模块，与IME没有项目/native依赖，复用已有固定工具链。它通过真实系统IME窗口触控和宿主oracle验证，不再用同进程IC桥接代替系统门禁。无网络权限，WebView仅本地静态合成页并禁脚本/网络/file/content导航，Compose仍未覆盖。新模块的依赖锁单独生成/审查，默认STRICT不变；settings原始文件修改hash账同步。

系统测试仅在已启用的本项目输入法之间切换，首次启用遵循系统警告流程。实际系统测试已开始但core失败未关闭；各scope、nonce/hash绑定与旧失败见testing/SYSTEM_HOST-20261023.md。协议/准备检查不替代验收。

## ADR040 — View拥有收到的安全inset，系统宿主等待实际窗口稳定（WIP）

NineKeyView只按框架传入的systemBars/displayCutout为自绘键区预留左右/底部，测量遵守父约束，绘制与触控使用同一偏移；不复制字体/图标或增加依赖。双设备16例实际View测试覆盖安全区无误触。系统IME导航控制区与普通宿主手势条不同，测试不得以宿主inset等值约束IME留白；固定方向下入口动画后的实际底排间距作为后续布局切换不变量，另核真实窗口显示底边和导航最低安全界。旧测试/旧制品失败保持记录，不把a11y注入成功当输入成功，不据组件声称1:1或完整M5通过。

## ADR041 — 结构删除完成后确认实际caret，再解队列（WIP）

系统privacy-queue红测17/20表明原SUCCESS立即解屏障会与onUpdateSelection外移取消冲突。新方案在手势SUCCESS后请求仅insertion-marker的IMMEDIATE cursor anchor，获得实际新caret后才解屏障，不猜删除UTF16长度；已执行后不支持/抛异常/超时/异常selection均AMBIGUOUS，不重试手势或回退DEL。查询阶段外移仍按selection版本拒绝，字段/session切换仍失效。只在正在执行/确认的操作内暂认32位置内的collapsed向前selection为自身编辑；以额外anchor与已观察selection交叉核对。selection回调未提供来源标签，不能据此声称任意宿主并发外移绝对可区分；复杂宿主继续验收。

HostAdapter确认caret到薄服务的内存回调，在后续提交前同步预测位置；服务区分最后实际观察selection与预测selection，避免将已收通知重复留在预期队列。无新依赖/持久数据/正文读取。组件原21故障保留并增加7例删除已执行但后置确认失败；实际系统同20例必须重测，未跑前不报告修复成功。

## ADR042 — 参考五态前端、内存Emoji及导航窗口策略（WIP）

依据五张静态参考，以591px来源宽度的键框/暗色中性颜色作为目标，纯ReferenceGeometry与同一HitMap驱动绘制/命中。Reducer加入EMOJI及辅助/Emoji返回来源，组合未清仍先确认，敏感不能进入中文；图片不打包。Emoji固定Unicode目录+本地临时搜索，无历史/网络/存储；字形由系统提供，线图原创。未知二级页/语音未提供/逐音节消歧未实现必须明示，不能靠外形宣称功能一致。

尝试公开captionBar隐藏以去掉Android额外按钮，仍保留navigation/gesture inset与本项目globe长按菜单；首新UIcore和inset通过，但重显时可访问性7px与实际view920px不一致。窗口intrinsic高度显式绑定是诊断阶段候选，未证实必要；caption hide时机继续A/B验证，未签收。新布局无需新增依赖，固定锁不变。测试声明布局dump版本后可读纯本键盘几何，默认dump不输出EditorInfo/正文，禁止将该机制扩展成用户上下文探针。

ADR042追加：固定高度、背景不透明、caption时机、clearCache等A/B未解决坐标问题，不能作为已接受修复；恢复原始窗口策略继续WIP。参考环境为iPhone13/iOS27.2，显示设置仍未知。measure_reference_ui只输出来源/捕获双SHA约束的几何和色块/白色bbox数字，输入FAIL或缺图仍明确保留，不产生自动视觉PASS。

## ADR043 — 稳定窗口/触控区域与独立像素对照（诊断WIP）

尝试透明全尺寸IME窗口、底部ReferenceInputContainer，onComputeInsets只报告实际键盘上沿与触控region；上部内容保持可见且应由宿主接收触控，未开启extract fullscreen。无障碍坐标仍FAIL，该策略未认定修复；独立像素对照6/9与宿主上部物理按钮通过，仅是有限范围证据。原lifecycle/reference-ui门禁不改，15秒lifecycle-diagnostic仅诊断，不签收性能。像素scope仅依屏幕/OS inset/参考色块定位，无内部事件或IC/候选桥，不替代TalkBack。

绘图与触控框分离：参考65/60来源px键面在Pixel下可能小于48dp，保留触控48dp，visualHeight单独控制填充/文字/原创图形。不得删测试或缩小触控范围换视觉PASS。字体只调整系统字形大小/字距/轻微缩放，不打包Apple字体；其他显示设置仍需实测。

## ADR044 — 优化构建的严格 release 单测锁补全

首次 release lint 暴露 releaseUnitTestCompileClasspath 无锁，实际 release 单测又发现 KSP processor 无锁。先检查可解析配置，再以 graph-only 方式解析八个明确的 releaseUnitTest 配置并写锁；避免 AGP 测试自身项目制品的 resolve() 歧义。完整差异审核没有新增/移除 module-version，也未移除配置项，仅已有257项扩展测试配置。STRICT 和制品验证保持启用；不跳过测试或降低锁门槛。新构建的50项 release JVM、release lint/构建和实际优化 APK 静态检查通过。

本地优化测试制品 a871fae3 使用本项目调试证书、非 debuggable；独立 Pixel 五态实际触控9/9通过。该制品是 b94 UI 源码检查点，**不含随后同字段 caret 修复**，不继承其测试，不是 M6 正式签名。失败、差异审核、三SHA真机结果分别见 evidence/m5/reference-ui-release-lock-red.json、reference-ui-release-unit-lock-final-review.json、reference-ui-optimized-build.json 和 evidence/m2/reference-ui-optimized-pixel-targets.json。

## ADR045 — 同字段仅隐藏输入View时保留结构光标

onFinishInputView结束控制器和临时UI，不等于宿主字段结束；onStartInputView若仍保留本字段EditorInfo，应复用已观察的当前selection而非初次initialSel。隐藏期间onUpdateSelection只更新RAM结构坐标，不重启控制器/读取正文/学习；onFinishInput仍清info，新字段onStartInput仍读取新metadata。相同最终独立host/test对照旧c51 1/5、新179a 5/5，两次新版本完整通过，四敏感正文getter0。没有新依赖；不是完整生命周期、任意宿主并发外移或隐私零写验收。

## ADR046 — 生成 release 测试源码的生产者依赖

真实联合release构建/lint失败：generateReleaseAndroidTestLintModel消费prepareReleaseEngineTests输出却无依赖；先前仅按compile/ksp任务名dependsOn漏掉lint。SourceSet改为直接接收Sync的TaskProvider，由文件集合携带生产者关系，删除任务名匹配补丁。原失败保留，完整联合assembleRelease/assembleReleaseAndroidTest/testReleaseUnitTest/lintRelease已通过；无新依赖/版本，STRICT不改，修改来源账同步。b127fe87含ADR045修复，本地调试证书非debuggable，50 release JVM及实际APK静态通过；并非正式发布签名。
