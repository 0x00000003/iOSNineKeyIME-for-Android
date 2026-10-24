# R04宿主字符级删除委托：测量前冻结的调查协议

保留当前HostEditorAdapter及R04原5个私密字素断言，不改生产私密零正文读取/持久层策略。只在自有debug EditorHarness合成宿主调用API34+ InputConnection.performHandwritingGesture(DeleteGesture, GRANULARITY_CHARACTER)，不启用/切换系统IME。该API的范围由宿主UI toolkit解释，不能从名称推断所有宿主、密码遮罩或字素都正确。

固定9个合成簇：原R04的rocket、e+acute、familyZWJ、ChinaFlag、thumb+skin；补multipleCombining、Hebrew+mark、CJK variation selector、ASCII。以固定前缀xy，在普通单行EditText与实际text-password遮罩字段各运行两种策略，共36案例。只记录g001…编号/模式/策略/布尔/计数/结果码，不记录文字、坐标或正文摘要。

策略A为test-only已知整个末簇范围矩形，仅判断宿主此委托能力，不能移作生产算法。策略B从光标向前1个UTF16单元逐步扩大，最多min(32, initialSelEnd)；坐标仍由test-only Layout oracle提供，不从InputConnection正文或TextBoundsInfo取得。每次FAILED要求宿主完全未变，第一次SUCCESS即停止且必须恰好剩前缀；UNSUPPORTED等立即停止，不靠删除后补偿或继续删到断言成立。没有fallbackText。回调每次最多2秒，总90秒；未回调/异常记录失败且停止，未运行案例不算通过。

包裹IC的正文/上下文getter（before/after/selected/surrounding/extracted/caps）一律计数并抛安全固定码，要求0；不用JNI、用户库、日志正文。EditorInfo仅看初始选区及advertised gestures，不读初始surrounding缓存。宿主自身可操作自己的Editable，test oracle只用于合成预置和结果校验。

API与来源：[DeleteGesture](https://developer.android.com/reference/android/view/inputmethod/DeleteGesture)、[HandwritingGesture](https://developer.android.com/reference/android/view/inputmethod/HandwritingGesture)。已读固定AOSP TextView fe2cff11d37cb1b20bfb9d998b426e12a07130b8：getRangeForRect使用mText GraphemeClusterSegmentFinder及CONTAINS_CENTER；requestTextBoundsInfo的layout text路径不同。该源码是线索，Pixel/API36实际结果必须单独记录，不能泛化到Compose/WebView/所有版本。

即便36案例通过，也只是有oracle几何的宿主委托调查；生产如何以最小结构元数据取得正确坐标、RTL/换行/重入/异步屏障/不支持的宿主/零上下文约束仍未实现/验证。整体R04保持FAIL，不能把此scope替换到完整发布门禁来“变绿”。
