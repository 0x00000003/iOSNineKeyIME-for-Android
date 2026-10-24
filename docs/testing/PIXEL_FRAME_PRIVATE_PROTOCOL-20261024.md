# 真实像素帧屏障与私密回归 v1（运行前冻结）

原无障碍窗口门禁继续保留FAIL，不能用此scope关闭TalkBack。新独立像素测试继续仅操作明确授权Pixel/已启用本项目IME；不使用服务dump/候选/坐标桥点击，不改生产APK，不写正文/密码/图像到Git。全部fixture合成；像素只用自有键盘裁图，RAM中处理。

已有reference-pixels在179a number阶段失败，仅知2个case/2个tap，不能预先归因。新增失败点标签，并在每次tap/capture前，以最多3秒轮询实际屏幕，要求宿主有焦点、IME动画结束、OS IME inset可见、自有IME包、顶部panel34中性色、来源高度差≤2px（归一591宽）、目标键原独立矩形/填色误差上限保持原值；实际键盘裁图尺寸与静态键面连续3帧一致，间隔40ms。比较只覆盖键区（排除候选/预编辑），RAM hash不输出。只等待，不重放操作，不改变body/action oracle；若超时保留FAIL。需要在同一主APK179a运行，以区分夹具与生产变化；不得称其为产品修复。

新增private-pixels36：password/visible-password/web-password/no-learn ×9原字素（火箭、组合音标、ZWJ家庭、旗帜、肤色、双组合音标、希伯来组合、异体选择、ASCII），每次新GuardedEditText预置A+cluster+B/caret末，实际退格精确AB/caret1，IC全部文字getter拒绝且计数0。相较原private-delete额外要求正文getter0；不缩减字素。

新增queue-pixels20：沿用原privacy-queue固定20例的精确正文/动作/时限断言：五敏感类型×2复杂字素10例、五种选区5例、password/no-learn延迟真实bounds300ms期间a/b续输2例、正常email删除续输1例、敏感长按八字符1例、真实pending切新密码字段1例。延迟续输需实际观察请求未交付后真实触屏输入，并记录第二键前仍pending（若帧屏障过慢导致错过，FAIL，不减少等待或假称队列覆盖）。每例敏感文字getter0，跨字段旧正文/新正文精确核对，实际系统cursor anchor/真实TextBoundsInfo与手势，无IC桥。

任何故障记录只含案例ID、布尔、调用计数、三APK SHA/nonce/注入次数和静态失败点。通过只覆盖对应子协议，不替代原a11y/full生命周期/多宿主/持久层零写或M2/M5/M6。

实现核对：首个稳定帧matrix的普通tap/capture已用三帧，Emoji坐标点击与100ms按下采图仍沿用旧shot；该记录仅声明实际9项oracle PASS，不能称所有入口满足v1三帧。后续统一这两个入口并重跑；按下先等待100ms，三帧捕获延长保持时间，最后CANCEL且正文不变。

queue实现前细化：300ms pending窗口内逐键截三帧可能错过待决窗口。对延迟队列两例，先在同一真实稳定截图分别核退格/a/b三个独立键面，再按该观察帧注入；期间每次确认宿主焦点/OS inset/动画/同一字段身份未变，并在第二个续输键前实际核boundsRequests1/boundsDelivered0。跨字段例只复用旧稳定帧退格，新字段重新三帧。其他键仍三帧；不延长300ms、不去掉第二键pending断言、不把事后输入当队列覆盖。
