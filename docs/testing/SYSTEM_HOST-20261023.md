# 独立系统IME宿主：实施与真实记录（WIP）

本轮测试使用两个已启用的本项目条目。实查选中旧优化release582016c3；确认两个条目均已启用后，只用选定Pixel的adb -s切到当前debug77636c33并启动本项目应用，无ime enable或系统警告绕过。见ime-selection-20261023.json；调试/优化版同名待在下一应用构建中区分。

## 独立宿主与范围

新增:testhost是独立UID的Java/Android合成宿主，无IME/native/Rime/data项目依赖；只debug变体。固定AGP9.3/SDK36及现有Kotlin stdlib2.3.21，STRICT lock+verification；新模块初次有意生成自己的锁，后续不用--write-locks。只有新模块输出，首次约7MB、追加测试后约11MB；复用唯一SDK/NDK/Gradle cache，不复制工具链。宿主APK2542943字节、首版SHA67e65110，权限列表空、无sharedUID、allowBackup=false，实际ZIP/清单记录见m2/testhost-static-debug.json。

原生EditText覆盖15种类型/action，另有固定本地WebView。WebView无JS、网络loads、file/content访问或导航中继，禁autofill；没有生产IME中的WebView。真正系统测试用UiAutomation只找本项目IME窗口，触控输入并在宿主内存中核固定合成结果，无手工InputConnection/坐标回调桥接。只输出案例ID/布尔/计数及输入类型/action整数；失败图只裁本项目合成IME窗口，放忽略artifacts，不提交像素。Compose还未加入，不能勾选完整I03。

run_system_host.py只操作选定Pixel或本项目emulator-5580。对未启用IME标BLOCKED；系统测试仅可选已启用本项目debug/release，不选第三方IME、不绕过首次启用警告。每次核设备model/qemu、实际三APK hash、独立run nonce、host_pass及instrumentation code，APK使用按SHA单实体池；正文/候选不输出到记录。

冻结当前scope：readiness=15元数据+1本地WebView；core=中文整词一次上屏/空闲标点、英/数/电话、五action、组合回车先确认再发送，共10；private-delete=4敏感类型×9字素，共36。元数据准备通过不等于系统输入通过；系统回调、权限/备份/性能/视觉全门禁各自记录。

## 实际执行

- 首版readiness：API36模拟器16/16；API37 Pixel15种metadata FAIL/WebView PASS，记录保留。
- 诊断发现所有Pixel EditorInfo.inputType比请求值多0x00200000，class/variation和action不变，IC非空。公开API37 TYPE_TEXT_FLAG_ENABLE_TEXT_SUGGESTION_SELECTED与实测一致；按运行时命名公开字段反射核值，只容许此新增位，保留请求每一位和动作断言。随后两设备readiness16/16。并补EditorPolicy JVM案例防该新flag改变密码/禁学习判断。
- 来源：[InputType候选选择标志](https://developer.android.com/reference/android/text/InputType#TYPE_TEXT_FLAG_ENABLE_TEXT_SUGGESTION_SELECTED)。固定编译SDK36未包含该37字段，未为此再下载一套SDK。
- 77636c33真实Pixel core首轮FAIL；触控诊断确认IME窗口与root属于本项目、5次注入成功，第6次查预期候选失败。明确设SOURCE_TOUCHSCREEN后仍FAIL，不能把注入成功当上屏成功。
- window-diagnostic仅IME裁图显示状态为26而非本案例64426。旧宿主连续修改已served字段type/setSingleLine并restartInput，可能使字段重启与首键交错。下一步先用未attached的新字段一次配置、确认InputMethodManager.isActive(newEditor)后再输入原5键；不加sleep、不改预期字串，尚不提前宣称已修复。served字段重启/生命周期需另测，不据夹具修改关闭I03或M2。

所有原始失败JSON及ignored裁图保留；后续成功必须以各record精确SHA为准，不把原readiness或组件成绩继承为system core/privacy成功。未提供iPhone参考原图，M5视觉仍BLOCKED；当前未完成应用验收/正式签收。

## 窗口/导航真实回归（继续中）

新字段一次配置及isActive屏障仍复现首3键丢失，因而旧served字段重启只是假设，未据此关闭问题。等待宿主WindowInsetsAnimation.onEnd后，77636c33的core为8/10：中文/标点、数/电话、后续action和组合回车PASS，Latin与首SEND FAIL。键高改变时a11y新节点先于窗口resize/translation提交可见，注入成功不代表点在新键上。

真实IME裁图还发现旧视图底排与手势条重叠。NineKeyView现在处理传入systemBars/displayCutout左右/底部inset，测量遵守父MeasureSpec，命中同减左inset；debug/release名称分别改为调试测试版/优化测试版。befe66f4 / unsigned133666e8实际APK静态权限/备份/固定资产/零字体审计PASS；50 app/77 state JVM、两套lint/构建PASS。四布局×四inset的实际View measure/layout/MotionEvent为两设备16/16，覆盖左右/底部安全区域无激活和键中心命中。首轮instrumentation实报16/16但runner私有文件解析变量错误，记录总FAIL保留；修复nonce解析并补工具测试后新record两设备PASS，42项tooling PASS。组件通过不等于系统窗口/视觉验收。

frame协议首版错误地要求IME底排间距等于普通宿主的导航inset+6dp。旧776测14px且FAIL，新befe测122px也因预期68px FAIL；这两个FAIL均保留。后续裁图与窗口几何表明IME含独立导航控制区，不能拿宿主手势条高度作等值断言，尚未证实是重复预留。改用真实bottom-gravity窗口到达显示边缘、底排完整落在窗口内且至少避开宿主导航区后，befe core9/10（Latin转PASS、首SEND仍FAIL）。该最低界仍漏掉键盘缩短时旧窗口额外空隙。

下一协议在同一固定方向/导航模式scope内，入口动画结束后要求三次相同实际IME底排间距，再将此间距作为跨布局高度切换的不变量；同时保持显示底边/完整键界/最低导航安全界。不是增加固定sleep，也不改上屏字串/action次数预期；首SEND记录计数进一步定位。system-host-nav-geometry旧record是在诊断Java编译失败后误运行旧测试APK，SHA如实绑定旧test，不能称为新诊断；v2才包含几何。诊断原始像素继续仅ignored本地。

最终窗口不变量协议在befe66f4真实Pixel core10/10 PASS（system-host-layoutbarrier-pixel-core）；实际IME底排间距122px，宿主最低界68px，所有五action各1次且无额外字符。说明此前首SEND由缩短键盘时新节点与旧窗口高度交错引起；未修改生产action逻辑或拉长动作成功期限。

同befe66f4真实Pixel private-delete36/36 PASS（system-host-first-pixel-private-delete）：四敏感类型×九字素，由系统onUpdateCursorAnchorInfo/真实跨UID结构查询与字符删除，不含手工坐标桥接。该scope未给所有IC文字getter设置拒绝/计数器，不能把组件正文getter0直接归到本系统scope；数字密码/选区/连续异步输入/复杂多行/Compose/WebView仍待测。befe两设备full组件PASS（原敏感5/5、21故障），controller两设备PASS；不关闭全部R04/I05。

当前仅一套工具链，testhost/build 11116KiB。新增记录均共享按SHA单实体APK，不按记录复制大包；本轮测.cache 20643268KiB、app/build1444308KiB、artifacts5899236KiB、系统可用24958912KiB。与上轮空间浮动须区分系统其他活动，不做全局清理。debug APK相比776约多0.9MB，ZIP逐项核查实际条目变化约1.5KB，其余是增量打包padding，未多打包词库/工具链；release61732341字节。

## IPC正文拒绝/队列红测

协议5eec11e提前冻结20例。新增GuardedEditText仅在真实TextView IC外计数/拒绝before/after/selected/extracted/surrounding/caps/takeSnapshot，并延迟真实TextBoundsInfo结果，无坐标/anchor桥。befe66f4 +hostea3e5216/test32d0b1f0真实Pixel privacy-queue17/20 FAIL（system-host-privacy-queue-red-pixel）：五敏感类型字素10例、选区5例、普通删除续输和pending跨字段均PASS，敏感正文getter全部0。password/no-learn延迟查询期间a/b续输FAIL，实际观察到已请求未交付；长按八ASCII只发一次结构/手势而FAIL。不能用此前36/36关闭R04全行为。源检查对应服务onUpdateSelection把自己删除引发的光标变化当外移，取消队列/触控；拟增加删除后cursor anchor确认与同操作selection归属，再跑相同20例。
