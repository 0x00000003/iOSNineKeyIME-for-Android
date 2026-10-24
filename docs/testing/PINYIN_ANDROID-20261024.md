# 选拼音 Android 接入检查点（2026-10-24，整体产品仍WIP）

协议先冻结于0d68eb4；展开页独立几何检查修正先冻结于dae4134。复用原SDK/NDK/Gradle/native目录，无新依赖、模型或工具链副本。

## 已实现

选拼音按钮现在发送真实只读查询，最多100条引擎注释经受限解析生成32个不同音节；普通按键仍取20+1个候选，不增加该批查询。选项按session/revision绑定，串行引擎再次核原raw及未确认数字范围；原生层独立校验旧raw、范围、编码及已确认边界。选择只改当前组合，不重置Rime会话/已选段/历史，不自动提交。继续选择下一音节，回键盘后选词；取消、结束字段、隐藏清会话状态，私密/密码路径禁止产生这些操作。

页面为项目设计的4列音节、8项分页和取消/返回，保留48dp触控；没有iOS二级页面原图，不能声称该页1:1。仅处理可验证的连续数字尾段和明确音节前缀，未知注释/中段混合格式不猜测改写；这是已说明的实现边界。

## 真实失败与修复

- v1/v2 Pixel组件：双音节/替代前缀通过，部分选词操作FAIL。v2细分失败为E_PINYIN_KEY：测试使用20项数据窗口中的候选，却没有先展开实际可见候选区。修正测试触控顺序，不改候选/提交断言；旧红记录保留。
- v3及final Pixel组件：生产NineKeyView→Controller→SerialRimeEngine→JNI→真实EditText通过，包括旧raw/伪造范围/修改已确认段拒绝、无隐式session创建、已选前缀与历史保持、最终恰好一次、私密切字段禁用。
- 系统v1：前3项音节触屏已通过，展开候选后错误套用五态主键盘固定高度而FAIL。新页没有对应iOS参考，v2以真实稳定inset/三帧与四行色块边界核触点；原五态高度/a11y门禁未改。v2独立UID真实系统IME 8/8通过，使用自有UI标签辨认控件、实际画面几何/色块定位触点，无生产状态/事件/IC/cursor桥。
- emulator-5580本轮连接检查失败，组件NOT_RUN；没有启用模拟器IME，不把旧模拟器成绩继承到新APK。

## 最终制品与范围

| 制品 | SHA256 | 范围 |
| --- | --- | --- |
| debug | fdc658b2dfde58914c575770cde50cad2b71f14a9e517e4cced532459357df3d | Pixel实际组件/系统触屏；不是最终发布包 |
| 优化非debuggable本地调试证书版 | 990b62cccf28f89eb66866b67b4ac1a04853ced85f624eac6b1c3da435f551a5 | Pixel实际系统触屏；不是正式签名 |

组件最终：[pinyin-android-final-pixel](evidence/m1/pinyin-android-final-pixel.json)。两个APK各自Pixel系统选拼音8/8、五态9/9、同字段重显5/5、敏感删除36/36、异步队列20/20通过。系统各scope完整SHA、host/test SHA、nonce、实际触点和8条断言见[检查点JSON](evidence/m5/pinyin-integration-checkpoint.json)。原始截图只含自有合成键盘，位于忽略目录，公开只留SHA/矩形。JVM 104 state、50 debug/50 release app、45工具测试通过；debug/release/独立host构建及lint通过。实际两APK无INTERNET、旧/新备份规则、固定资产和零打包字体静态审计通过；不代替全系统安全/备份迁移。

原a11y窗口坐标/lifecycle仍FAIL且源码未改，不用这次物理scope关闭TalkBack。完整多宿主、性能、隐私持久层、许可闭包、正式签名和M6同APK全门禁仍待逐项推进，不宣布整个应用完成。
