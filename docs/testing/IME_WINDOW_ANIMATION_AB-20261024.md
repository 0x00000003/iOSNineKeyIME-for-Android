# IME自有窗口动画A/B（先冻结，诊断而非修复声明）

基线为7e82da6/debug5c46，独立host/test保持现有哈希与lifecycle五轮协议，原真实窗口到达屏幕底边/完整键界/导航最低界/稳定间距门槛不改。先运行未改基线；如果仍FAIL，仅在自有IME的configureReferenceWindow加入Window.setWindowAnimations(0)，不改系统全局动画/导航/缩放/默认第三方键盘，不改变实际View高度、Insets或上屏oracle。

两次均使用授权Pixel、同host/test、实际主APK SHA/nonce/code/host_pass绑定。基线若已通过，应重复基线，不能把未应用的候选当修复。候选若失败，恢复本项目这一个新增调用，保留实际候选制品与失败记录，继续独立工作。成功也需原core/private/lifecycle和五态全部真实回归后方可接受，不将单次窗口改善当TalkBack验收。

目的仅检验本IME窗口动画路径是否相关；不是Android系统缺陷归因，不承诺关闭系统Insets动画。未运行前没有结果。

Result: baseline5c46 and candidate9ebeed both FAIL at original ime-frame-coherent. The animation call was removed. Evidence: reference-ui-own-animation-ab.json; no system-cause conclusion.
