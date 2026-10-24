# 原生候选分页（WIP组件记录）

原M2仅在本地缓存前100个候选，不能分页到100以外。改为每次20项+1项lookahead确定更多，effect/response绑定session/revision/全局offset；只保留当前20项与首候选文本，旧候选ID和错误页offset/组合/非空commit回复拒绝，分页不调用消费型getCommit。选择使用全局index；空格/标点确认仍选global0；收起时重新读取第一页，不把当前页当首选。

视图上一页/下一页通过controller→单worker JNI，不再本地计数装作原生分页。请求中旧候选/翻页按钮禁用，cancel仍可用；实际末页hasMore=false后下一页禁用。保持私密直输不调用native，不新增宿主上下文读取或持久层。

首次实现后原61项state测试与新增10项分页测试全PASS，49 app测试及debug/release lint、构建PASS。实际Android>100候选选择、前后翻/收起和宿主oracle尚待执行；不能仅凭纯状态测试勾选M2或称真机通过。当前Pixel30分钟×2性能观测使用旧94b9d40f，页面代码不归到旧性能哈希。

待运行Android组件协议：真实MotionEvent翻到global offset120，往返100/120；每个状态窗口≤20项；旧页候选ID延迟提交不产生host调用；实际点击global120只上屏一次；收起异步回第一页；再次后页用Space仍确认global0，实际EditText oracle和commit调用数2。只记录index/计数/布尔，不输出候选文字。该测试未替代系统IME启用、多宿主或视觉检查。

## 真实测量与修正

首次两设备FAIL，诊断版记录完整状态而不输出候选正文：单键6仅4候选，hasMore=false、Fetch次数0，正确禁用下一页，测试前提不成立。三份失败JSON（m2-native-*-paging及emulator-paging-diagnostic）保留。改用完整音节64，保持跨120选择/仅一次宿主提交等原断言，主338bb5be/test bb06acbb两设备PASS：11个真实Fetch、global120与global0共2次真实EditText提交。

复核另发现HitMap只含可用键，onLayout误将禁用键按status重排。将所有键绘制geometry与可点击HitMap共享同一KeyBox来源但各自保留，使禁用键保留原绘制位置且无触控目标。补Android实际layout及点禁用previous无Fetch断言，补controller翻页异步屏障/跨字段过期页两项（state总73），end清除首选文本缓存。

最终debug `dd8f688f6c8bdc1cb3384e5e9a023b63384a1083641d23bde60af7a1a37dcb7c` / test `68bc0af2f9d03b6f995346c51ae0b2903797d5e61134ad227f2435a188f918c1`，完整scope强制包含paging（editor-only仍不调用native）。[Pixel full](evidence/m1/m2-final-pixel-full.json)与[emulator full](evidence/m1/m2-final-emulator-full.json)：引擎、11宿主故障、分页及禁用键geometry均PASS，原R04 4/5导致完整scope仍FAIL。不能把本页分项通过称系统IME或M2完成。

49 app/73 state JVM、debug/release lint、默认构建及优化release构建PASS。实际APK零字体/权限/备份/资源审计分别绑定debug/unsigned/signed hash；同哈希其余回归见STATUS和对应evidence，不继承旧版本成绩。
