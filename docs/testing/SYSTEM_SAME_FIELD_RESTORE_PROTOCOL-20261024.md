# 同字段隐藏/重显后删除与续输 v1（运行前冻结）

独立scope restore-pixels，复用已验证的外部实际屏幕色块定位/真实触屏，未使用内部Event/IC/候选桥，不替代失败的原无障碍门禁。当前原lifecycle每轮重建字段，未覆盖同一个EditText只隐藏/重显，因此新增独立回归。

明确授权Pixel/已启用本项目IME/独立UID合成宿主，所有fixture固定合成，公开只记ID/布尔/计数/nonce及三SHA，不输出输入正文或宿主截图。密码/no-learn/数字密码的GuardedInputConnection正文getters全部拒绝并计数，要求0；仅当前会话必要结构请求允许。普通email对照允许正常上下文。定位和捕获继续保留参考像素协议全部边界，不使用服务dump坐标点击。

固定5案例：
1. email、password、no-learn三类各一次：新字段预置AB/caret2，实际点a→ABa，公开IMM hide→等IME不可见/动画结束，同一个EditText再show（不调用selectCase/restartInput/改类型或文本），实际退格→AB，再点b→ABb且caret3；精确一次，不回放旧会话。
2. number-password：12/caret2，实际点3→123，同字段hide/show，退格→12，再点4→124/caret3。
3. password隐藏时外移：ABCD/caret4，实际a→ABCDa；hide后由自有宿主setSelection(1)，同字段show，退格→BCDa/caret0，再b→bBCDa/caret1。

hide/show每段最多5秒、正文结果最多3秒，实际tap前截图须满足自有宿主焦点/OS IME inset/自有IME package及键色/几何。不得用新字段掩盖同字段问题、force-stop、切第三方IME或降低字素/零读约束。每个敏感案例记录body_getter_calls/cursor/bounds/gesture请求次数和精确oracle布尔。5/5且注入/fixture/runner新nonce+三SHA/code一致才是此子协议PASS；不等于完整R04/I05、TalkBack、全部生命周期或M2/M5完成。
