# 输入事务同步诊断 v1（运行前冻结）

固定AOSP fe2cff11d37cb1b20bfb9d998b426e12a07130b8的UiAutomation源码显示公开二参数injectInputEvent会等待窗口动画，KeyEvent DOWN前/UP后还会同步WindowManager输入事务。AccessibilityWindow的屏幕矩阵来自输入窗口句柄逆矩阵。由此**推测**原无障碍查询可能早于输入事务完成；这不是设备框架版本证明，也未认定平台缺陷。

新增独立scope lifecycle-sync-diagnostic，不修改原lifecycle的3秒窗口边界/导航稳定/5轮隐藏重显/实际选词仅一次断言。在原choose的宿主IME inset可见且动画结束后，仅当本合成宿主持有焦点时，通过公开UiAutomation同步注入虚拟KEYCODE_UNKNOWN的DOWN/UP（无字符、无快捷动作），随后精确核对合成夹具和selection未变，再查真实无障碍窗口/节点。任何注入失败、fixture变化或旧窗口不稳定仍FAIL。没有内部Event/IC/候选桥、dump坐标点击、切换第三方IME或force-stop。

结果单独记录2×barrier次数、fixture不变及三APK SHA/nonce。不得把诊断PASS改写成原协议PASS、产品修复或TalkBack验收；需进一步验证普通系统服务和原门禁。如无效即保留失败，不因此更改生产窗口。

主来源（只读研究，未复制进产品）：
- https://android.googlesource.com/platform/frameworks/base/+/fe2cff11d37cb1b20bfb9d998b426e12a07130b8/core/java/android/app/UiAutomation.java
- https://android.googlesource.com/platform/frameworks/base/+/fe2cff11d37cb1b20bfb9d998b426e12a07130b8/core/java/android/app/UiAutomationConnection.java
- https://android.googlesource.com/platform/frameworks/base/+/fe2cff11d37cb1b20bfb9d998b426e12a07130b8/services/core/java/com/android/server/wm/AccessibilityWindowsPopulator.java
