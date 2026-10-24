# 当前需求验收账：未签收

同优化c37的双设备engine/resources/quality/learning/context十条记录已全部通过（词Top5 99/100、句19/20、十词学习/重启/清合成库10/10、上下文9/10）。[当前引擎记录](testing/CORNERS_RELEASE_ENGINE-20261024.md)同时说明旧测试元数据文案与新f7b2测试包两设备复测；不是完整系统质量/隐私或性能验收。两个窗口诊断候选已失败撤销，生产服务恢复已验证检查点。

2026-10-24 当前五态检查点（WIP）：debug `5c46ae95` / 优化本地测试证书 `c37aa834` 已补底角和页脚原创图形尺寸。**两版本分别**在Pixel五态9/9、同字段重显5/5、敏感字素36/36、异步队列20/20 PASS，敏感正文getter0；debug两设备full28故障/敏感5/metadata6、controller和view20通过。83 state、50 debug/50 release app、45工具与构建/lint、三个实际APK静态PASS。原a11y窗口坐标/lifecycle、完整安全/性能/多宿主/正式签名仍未签收，字形/Emoji/导航/未知状态不同，非1:1。见[当前记录](testing/CORNERS-20261024.md)及[三SHA检查点](testing/evidence/m5/reference-ui-corners-checkpoint.json)。下列为历史增量，不替代当前制品证据。

2026-10-24 最新增量（WIP）：debug `179a24e7` 修复同一字段仅隐藏/重显后退格使用过期初始caret的问题。最终同host/test旧c51 1/5→新179a 5/5，另一次新179a 5/5；四敏感场景正文getter全0，隐藏期间外移光标也按真实位置删除。保留旧红测及早期no-learn定位中断；当前五态触控回归在number阶段FAIL，不能继承c51/a871的9/9。50 app/83 state/45工具、debug/testhost lint/build、实际APK静态通过。原无障碍与完整安全/性能/正式签收未关闭，见[同字段记录](testing/SAME_FIELD-20261024.md)。

最新增量（2026-10-24，debug c51dad6b，完整SHA见STATUS）：已启用/选定的自有系统IME独立像素触控9/9，五态+按下6图；两设备view20+搜索会话清除、full28结构故障/敏感5/metadata6和controller通过；83 state/50 app/45工具、debug构建/lint/实际APK静态通过。原无障碍坐标/lifecycle仍FAIL，未宣布阶段完成。下表738a365及后面的77636c33均为历史时点；当前增量如下：

| 需求 | 当前新增真实证据 | 尚缺 / 整体状态 |
| --- | --- | --- |
| R01/R02/R03/R11 | 独立UID系统真实64426首候选仅一次、空闲标点、数字/符号/英文/Emoji真实输入与返回9/9 | 多宿主/部分组合/完整生命周期与性能，WIP |
| R04/R07 | 同c51双设备组件敏感5/metadata6、正文getter0、28结构故障；搜索只在本地、会话结束清除 | 旧831系统36/20不继承；当前完整系统隐私/持久层/日志尚需运行，WIP |
| R05/R06/R16 | 实际c51 APK无INTERNET、备份旧/新规则、固定资产与零打包字体审计PASS | 完整首次离线、目标OS备份/迁移、当前release、许可/对应源全部门禁未关闭 |
| R12 | 五态实际往返与输入通过；敏感数字→符号返回Latin红/绿；上部宿主按钮收到物理触控 | 旋转/切换/多宿主动作/无障碍坐标稳定性，WIP |
| R13 | iPhone13/iOS27.2五张深色原图已到，归一化高度差<0.6来源px，四态主键边差≤1.297px，六张新鲜实拍与图hash | 版面其余状态、字号设置、字形/Emoji/原生导航差异、录屏/用户体验确认；非1:1 |
| R14 | 保留48dp无重叠触控框并分开绘图；双设备真实View20、基本模式名/键标签 | 原系统无障碍窗口坐标及lifecycle明确FAIL，TalkBack/字体缩放/横屏仍未签收 |
| R15 | 本轮构建/测试时间仅执行记录 | 不作P01—P04数据；当前release完整压力与时延/帧/冷启/长期内存未验收 |

详见[参考测试](testing/REFERENCE_UI-20261023.md)、[c51检查点](testing/evidence/m5/reference-ui-calibration2-checkpoint.json)。没有使用独立像素scope替代失败的原无障碍门禁。

原表针对代码检查点738a365，制品完整SHA与26条分项记录见[CHECKPOINT](testing/CHECKPOINT-20261023.md)。`组件PASS`不是相应需求整体PASS；系统IME尚未由用户启用，测试只在自有合成宿主中运行。不得据表内任一分项宣布应用完成。

| 需求 | 当前可复查证据 | 整体尚缺 / 状态 |
| --- | --- | --- |
| R01 九键编码 | 73 state单测及两设备真实JNI/触控→EditText，含跨120原生选择 | 系统IME D01未运行，WIP |
| R02 栏位/部分组合 | reducer/controller实际快速输入、部分提交、取消/空闲标点及延迟ID拒绝 | 系统IME/参考S01与D02未运行，WIP |
| R03 仅一次提交/字段隔离 | host gate、11个真实异常/重入及跨页延迟ID；两设备组件PASS | Compose/WebView/系统生命周期I03/D03未运行，WIP |
| R04 删除/长按 | 普通6组/敏感4组字素、长删跨字段组件通过 | 敏感未知预存组合音标第5组FAIL（4/5）；P0未关闭 |
| R05 离线/禁网 | 三变体实际权限/资源、优化release引擎及当前582016c3独立模拟器离线新装引擎组件 | 同release完整系统IME断网首装/重启D05、完整网络审计未完成 |
| R06 禁备份迁移 | 三变体实际manifest、旧/新XML各9域及noBackup路径检查 | 目标OS备份/迁移行为未运行，不能重置日用机来补测 |
| R07 私密不读/不学 | 6类元数据策略、私密native隔离/普通profile不变；UID canary0；预准备无学习profile跨重启PASS | 完整系统IME全路径I05/SEC04未运行；私密中文未实现，跨重启调查不替代该验收 |
| R08 质量 | 当前debug与release两设备固定100词/20句，Top5=99/100及19/20 | 完整系统IME D08未运行，组件门槛PASS |
| R09 学习/清除 | 同当前两制品两设备10非首位词各20次，10/10改善/重启/清合成库回基线 | 设置清除当前普通库正向流程、完整IME隐私交叉场景未运行；UI自动测试只点取消 |
| R10 上下文 | 当前两制品两设备四格最小对照9/10改善；off0查询/未加载 | D08系统IME同会话/字段清理仍未运行，组件门槛PASS |
| R11 空闲标点优先 | reducer/真实controller整词后标点，预测不占栏 | 系统IME D02及参考矩阵未运行，WIP |
| R12 布局/action兼容 | 英/数/符与动作决策、真实宿主异常组件 | 系统IME、多宿主旋转/切换与D09未运行，WIP |
| R13 参照视觉 | 临时原创布局/自有合成View.draw | iPhone原图/录屏、iOS设置和用户体验确认缺失，BLOCKED；未验证1:1 |
| R14 无障碍/触控 | HitMap及Android实际禁用键几何/命中测试，基本可访问标签 | TalkBack、放大字体/系统inset/横屏D11未运行，WIP |
| R15 性能/压力 | 冻结旧94b9d40f模型开/关各30分钟JNI/PSS/GC/温度真实样本 | 当前版本完整P01—P04未验收，不把JNI候选预算当触控/帧/启动成绩 |
| R16 许可/可重建 | 离线44正文、122精确运行时/POM链、递归源码/修改账、严格锁和资源SHA | 96d新SDK/JDK/初始空Gradle cache整包PASS；738a365的44正文源码以warm固定输入隔离重建整包PASS；词库二次来源/模型对应源及正式分发审查未关闭，WIP |
| R17 P2触感/动画 | 临时UI；未声称iOS算法/素材相同 | 非首次P0/P1签收替代项，待参考后调校 |

M1/M2/M3/M4/M6当前均是WIP，M5视觉BLOCKED；没有勾选任何尚缺证据的阶段完成框。代码738a365/96d627a与性能记录615a477等均按版本提交。后续先由用户按系统设置启用测试IME并提供原图，才可补相应系统/视觉门禁；R04失败需继续定位/设计，不以用户启用或原图到位自动视为解决。

最新增量：生产debug77636c33/test712e60e9在两设备full探针及controller组件PASS，原R04敏感字素5/5、metadata6/6、正文getter0/结构请求11，21故障均PASS。77 state/49 app/40工具及debug lint/实际APK审计PASS。旧表4/5是已冻结旧DEL路径事实，不覆盖；系统IMS cursor anchor仍经合成桥接，因此R04与M2全门禁保持WIP。详见testing/STRUCTURAL_DELETE-20261024.md及structural-delete-summary.json，旧release不包含此修复，当前release成绩另列。
