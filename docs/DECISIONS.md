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
