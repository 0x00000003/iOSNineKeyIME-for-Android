# 敏感字段结构删除：生产组件检查点（WIP）

隐私策略规定“仅禁止文字正文，必要结构信息可以使用”（ADR037）。本次改变生产HostEditorAdapter/Controller/薄服务；不是先前只研究的两个probe，也不是系统IME验收。

## 路径与边界

敏感路径同步delete明确拒绝。异步删除只请求一次IMMEDIATE cursor update，过滤为INSERTION_MARKER|EDITOR_BOUNDS；薄服务把onUpdateCursorAnchorInfo传入适配器，仅消费选择位置、当前行插入点和editor bounds/matrix，不取composingText。已知非空选区以空commit替换，不取选中文字；空光标不编辑。

结构请求区域为当前插入行与editor区域交集，经matrix映到screen。TextBoundsInfo只消费start/end、matrix与最多32个尾部UTF-16位置的character bounds，不读grapheme/word/line SegmentFinder、正文或序列化元数据。逐渐扩大的区域由宿主以CHARACTER粒度判定原文字素；FAILED才可尝试下一面积，第一次SUCCESS停止。无fallbackText，无DEL后备，无自动重放。

每个请求捕获连接、session和selection version；字段/连接更换取消作业，查询期间光标变化拒绝。重复bounds和gesture回调只能消费一次。整个作业上限2秒；查询前超时为UNAVAILABLE，手势已发出后超时/抛异常为AMBIGUOUS（宿主可能已编辑），停止并显示E_HOST_DELETE，不自动补删。Controller在等待回调期间阻挡后续事件，长按重复不积压；失败清掉待执行输入并保持错误到重新开始字段。先递增epoch再end旧port，防同步取消回调排空旧字段队列。

公开API坐标定义核对：[EditorBoundsInfo](https://developer.android.com/reference/android/view/inputmethod/EditorBoundsInfo#getEditorBounds())的editor bounds为local，应用CursorAnchorInfo matrix转screen；[CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo#getMatrix())。源码/测试使用固定SDK36，Pixel API37与自有API36模拟器。

## 真实记录

起始测试原生产b0ca3dd5与c6c6e8bf研究test两设备仍为4/5 FAIL，历史不改。新增Controller顺序断言首轮实际76 tests/3 failed（另一次错误的--tests过滤没有运行测试，不算红测）；实现barrier后76/76通过，新增end回调重入保护后77/77通过。49 app JVM与debug lint/构建通过。

第一版生产debug c2f1c8f0，两设备editor组件普通6/6、敏感metadata6/6、敏感grapheme5/5，正文getter0、结构请求11；normal learning tree字节不变。第一版test0ed8中旧h007仍标private_key但敏感同步入口已直接拒绝，未真正注入key异常；其PASS仅能证明拒绝，不能计为已覆盖key异常。后续testf565明确改名private_sync_rejected并断言keyCalls0，保留10个原宿主故障及该入口拒绝检查，不删除原记录。追加19个独立结构故障案例，同main c2f1与新test f5652c74两设备19/19通过：不支持/抛异常/anchor或bounds超时、错selection、切字段/移动光标、gesture前/后抛异常或无回调、重复回调、选区/空光标。正文getter0、无key fallback，应用后异常/超时保留一次删除而不重试。

早一轮debug 09f884357c5bee7b58dffb47717316f89ea239d5e83f4e4ef1a3f495e2821d10（73479289字节），test f5652c744fe70f90abb15ef120a86c3e7198ace3b21ea317ae2f20a48b021c7a。对应final full/controller记录见evidence/m1/structural-delete-final-*；最终结果见各JSON，不预报未完成scope。实际debug清单/备份九域/五资产/零字体审计PASS，单次误写APK文件名的审计命令未运行，随后按output-metadata选择实际文件审计。

## 未覆盖/仍阻碍

- 组件中的cursor anchor通过自有合成EditText结构桥接，**不是实际IMS系统回调**；真实InputConnection查询/删除与生产适配代码执行，但多宿主、生命周期、旋转、系统选择移动竞态仍待用户在设置启用测试IME后验证。没有用adb切换默认键盘。
- 32单元边界/整个两秒预算为失败关闭上限，不是全部Unicode/宿主支持保证。仅测过的5个生产R04字素、各敏感metadata及研究9类案例有证据；长跨行字素/软换行/RTL全部生产组合未覆盖。必要结构信息允许不等于已通过完整隐私安全/备份门禁。
- API<34、无法提供结构坐标或手势的宿主停止此次编辑；不以截断字素的DEL掩盖兼容性限制。Compose/WebView尚待真系统验证，R04整体保持WIP。
- 当前更改未继承旧release的质量/性能/全安全成绩，正式签名与M5原图仍缺。M1—M6均未宣布全部完成。

## 最后同步重入修正

收尾复核发现HostAdapter.begin/end/reconnect取消旧作业时，旧reply可以同步启动更新字段；若之后继续旧清理，会覆盖新连接。现在先失效旧session/连接并完成清理，再取消；begin取消后重查session以保护重入的更新字段。Controller对应的epoch预先失效保护也保留。

追加reentrant_begin/reentrant_end后共21案例。新test712e60e9先配旧09f88435在模拟器实际红测：原19案通过，reentrant_begin阶段FAIL，记录structural-delete-reentry-red-emulator-editor.json保留。相同test配修正后的77636c33两设备full探针21/21 PASS，controller分项另列，不删旧失败或降低断言。

最新debug SHA256 77636c33c8016e47b46f57157c262fef49eddea4a6bc8dae9f0c3268d08a611b，73479289字节；test 712e60e98f1110bfd3e691fa918d46aba069ba4749e3fff6e5e47bee409c4436。默认unsigned release SHA256 02c7f87080273f13bd6ffd0a1afe6e77d2ce7ee8295816ed0bf10d2e3d64458f，61731337字节；未签名/未运行，不继承582016c3的历史release结果。77 state/49 app JVM、40工具、debug/release构建和两套lint PASS；两个最终实际APK清单/备份/资产/零字体审计PASS。全部对应reentry-*记录；没有再次复制SDK或做无关全工具链重建。
