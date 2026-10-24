# 可触区域策略A/B（运行前冻结）

基线7e82/debug5c46原lifecycle仍在入口窗口坐标FAIL；Window.setWindowAnimations(0)候选9ebeed也FAIL，源码该调用已恢复，不据此解释系统原因。

下一候选只保留原contentTopInsets/visibleTopInsets数值，touchableInsets从REGION换为公开VISIBLE，并清空在此模式忽略的touchableRegion。不改View测量、窗口尺寸、Insets top、导航/默认第三方键盘、候选或上屏逻辑。公开API的可见inset策略与自定义region是不同窗口输入分支；这里只检验分支相关性，不预先认定REGION有框架缺陷。

同host/test先跑原lifecycle五轮，原屏幕底边/完整键界/导航最低界/稳定间距全部保持；候选FAIL则恢复两处策略调用。若通过，继续同SHA原core10/private-delete36/privacy-queue20、五态真实像素/上部宿主按钮9、同字段5、正文拒绝私密36/20及组件/lint/静态检查；不得单次PASS即签收TalkBack或M2/M5。所有结果绑定三SHA/nonce，重要失败与候选制品保留。

Result: candidateefc975 FAIL at original ime-frame-coherent with the same baseline coordinates. Both strategy calls restored; production service SHA equals the corners checkpoint. Evidence: reference-ui-visible-insets-ab.json.
