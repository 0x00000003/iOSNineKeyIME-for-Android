# 旋转组合保持与私密错误提示（2026-10-24，WIP）

本轮修复两个具体问题：同字段旋转后未确认拼音被清空；敏感英文/数字面板没有显示编辑失败。没有关闭完整M2/M4/M5/M6门禁。

## 失败定位与实现

原优化990b62cc真实Pixel旋转3/4，未确认组合丢失。首个候选cb7d10db仍失败；有界debug生命周期诊断59af7142证明同步配置重建已保留，随后异步onStartInput再次重建会话。上述失败记录保持原样。followup候选9a8f93e6调试版旋转7/7后，删除诊断代码，并额外要求新EditorPolicy非敏感，构建本轮最终候选。

服务仅在配置重建期间保留普通中文组合且无宿主待提交项；随后一次重启还需框架restarting标记、相同InputBinding token、字段/输入策略/结构光标一致、同会话及一秒内条件。敏感字段继续建立隔离会话，必要光标只在RAM保留；隐藏/结束/切字段仍结束旧会话。没有读取正文、自动重放提交或新增依赖，固定锁未变。该范围不等于任意宿主生命周期均通过。

敏感面板失败时显示固定“编辑失败 · 点此重新连接”入口，不再被私密提示覆盖。重新连接不自动重放失败退格；WebView实际退格若不支持仍判FAIL，不能用错误提示代替删除成功。

## 当前制品与真实测试

| 制品 | SHA256 | Pixel旋转 |
| --- | --- | --- |
| debug | e30978937df0bedc0ae5c41625195981e3c6a6b41673ce5cb0ce000c88a9fd4f | 7/7 PASS |
| 优化非debuggable、本地调试证书 | 91350a65112bebb3d7a92db11d1d3830fdefe2bc71da831fb1e59ab83b6d7ec1 | 7/7 PASS |

旋转7项包含原4项、密码结构光标跨两次旋转、部分已选词继续提交、配置中切换新字段防旧候选泄漏。测试只请求自有Activity方向并恢复原请求，不改系统全局旋转/字体/导航，不操作其他应用。

记录：[debug旋转](evidence/m2/configuration-final-debug-rotation.json)、[优化旋转](evidence/m2/configuration-final-optimized-rotation.json)、[debug选拼音8/8](evidence/m2/configuration-final-debug-pinyin.json)、[优化选拼音8/8](evidence/m2/configuration-final-optimized-pinyin.json)。最终debug的Pixel/API36模拟器各自view20项+私密错误2项、pinyin真实JNI/视图及full组件均PASS；其余系统回归按检查点逐条列出，未执行不继承旧fdc/990成绩。104 state、50 debug app、50 release app、45工具测试PASS；debug/release/host构建及lint通过。两实际APK静态权限/旧新备份XML/固定资产/零打包字体审计通过，不能替代运行时安全门禁。

[当前优化WebView](evidence/m2/configuration-final-optimized-web.json)仍2/3 FAIL；provider155.0.8059.43的密码连接cursor请求1次返回false、后续bounds/gesture均0，敏感正文getter0。固定错误实际可见，物理点击重新连接后密码长度维持3（没有退格重放），继续输入后长度4，恢复功能两个布尔均true。这不关闭原删除FAIL，也不推断其他provider能力。

红测：[旧私密错误视图](evidence/m1/private-error-visible-red-emulator.json)、[首候选失败](evidence/m5/configuration-attempt-v1.json)、[异步重启定位](evidence/m5/configuration-trace-v1.json)。诊断版不作为交付包。首次静态审计命令使用不存在的通用APK文件名，未执行审计；改为读取AGP output-metadata后，对两个确切APK实际审计通过，原命令日志保留。

当前完整范围/源文件和制品hash/磁盘记录见[本轮检查点](evidence/m5/configuration-final-checkpoint.json)。仍有WebView密码退格FAIL、邮箱外部焦点阻碍、原a11y窗口坐标FAIL，P01—P04完整性能/安全/许可闭包/正式签名与同APK全门禁尚未关闭。旧选拼音检查点的完整9/5/36/20成绩仅对应旧APK，不转移为本候选成绩。不称1:1或可日用交付。
