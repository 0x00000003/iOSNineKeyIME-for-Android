# R04结构坐标调查：第二阶段测量前冻结

在第一阶段36案例能力成立之后，只调查坐标能否来自API34+ TextBoundsInfo，不改变生产HostEditorAdapter/EditorPolicy或原R04。结构坐标查询与IC正文getter分开计数；本scope确实请求结构元数据，不能称为“没有请求上下文/元数据”或完整私密策略通过。

复用已冻结9簇×普通/真实text-password单行共18案例，初始cursor只用EditorInfo选区。test host只提供当前editor屏幕可见区域，不提供某个字符或整个末簇的坐标。请求一次TextBoundsInfo；结果必须成功，先Parcelable往返以模拟跨进程结构传递，2MiB预算，原始parcel仅RAM中检查已知合成UTF8/UTF16LE序列是否出现，绝不保存/输出blob或正文摘要。该检查不是所有输入均不可能泄露的证明。

后续只读取start/end、matrix、最后最多32个UTF16单元的character bounds；不读取word/grapheme/line segment finder、标点/空白flags或内容。逐步扩大最后N单元的矩形并映射到screen，委托相同字符级DeleteGesture，第一次SUCCESS必须文本恰好剩合成前缀且光标正确；所有FAILED必须原文本/选区完全不变，无fallback text，不做删除后补偿。每回调2秒、总90秒；正文getter仍计数并拒绝，要求0，单独记录结构请求数及parcel字节型marker检查布尔。

这是自有合成宿主的额外数据接口调查。生产如何获取最小请求区域（而非test提供）、异步队列/字段变更/过期快照/超时/不支持宿主、RTL/多行/屏外/超长簇、当前“不得读上下文”约束下可用结构数据边界尚未实现和验收。没有把结构查询加入生产私密路径，R04仍保留FAIL；不能将研究pass归给系统IME或全部密码/隐私场景。

接口来源：[TextBoundsInfo](https://developer.android.com/reference/android/view/inputmethod/TextBoundsInfo)、[InputConnection](https://developer.android.com/reference/android/view/inputmethod/InputConnection)。固定本机SDK36公开签名与AOSP fe2cff的实现路径已检查，必须另测Pixel/API36实际行为。

## 实测结果

主APK b0ca3dd5不变，test c6c6e8bf72b3d415d40fd6085817b5c54609ce801a5fee6f4555a4048d27c709。[Pixel](evidence/m1/m1-bounds-pixel-gesture-metadata-research.json)、[API36模拟器](evidence/m1/m1-bounds-emulator-gesture-metadata-research.json)均18/18 PASS：普通/真实password各9，结构坐标请求各18次、IC正文/上下文getter0；Parcel往返后的坐标可委托宿主正确删除，全部已知合成字节marker未观察到。该marker检查仅该序列/该parcel，不推断任意数据完全不泄露。

同test再跑第一阶段两设备各36/36，结构请求0；原editor两设备仍4/5 FAIL，host生产代码未改。坐标Snapshot虽然不读文字，但确实是新增结构元数据读取，来源/flags/边界集合由框架提供；算法只取范围/matrix/char bounds，不能把它写成“不取任何额外上下文”。生产最小区域/服务回调/异步队列/字段epoch/超时/多宿主等尚未实现或验证，当前R04不关闭。
