# 同字段重显真实回归（WIP，2026-10-24）

依据预先冻结 SYSTEM_SAME_FIELD_RESTORE_PROTOCOL-20261024，以实际外部像素定位/系统触屏、独立UID GuardedEditText、公开IMM hide/show，同一个EditText对象不替换；预期正文仅宿主内存核固定合成，不写Git。隐私策略仅禁止正文读取，允许必要结构信息。

最终host `306227bac17c9d8661393c2ed625f778792452f4ffc353dc3c0acbc425ebe293` / test `0daa38ab8ab6ced038f71a62615e61716e78e7ff2e0bf9b4057468cad5b1f776`，分别绑定实际安装主APK、nonce/code与host_pass。旧c51在email通过、四敏感失败，全部完成15次真实tap；新179a同协议5/5、15tap，各敏感0正文读取/2cursor/1bounds/1gesture。另一次新179a同test完整5/5。

- evidence/m2/reference-ui-samefield-pixel-red.json：原红测，no-learn定位中断。
- reference-ui-samefield-stable-pixel-red.json 与 reference-ui-samefield-caret-pixel-green-attempt.json：加公开UI idle屏障后，对照旧password失败、新password通过；两者在no-learn定位中断，整组均FAIL。
- reference-ui-samefield-detail-pixel-attempt.json：仅添加非正文失败点后新179a 5/5。
- reference-ui-samefield-finalhost-pixel-red.json：同最终test，旧c51 1/5。
- reference-ui-samefield-finalhost-pixel-green.json：同最终test，新179a 5/5。

服务改为保留同一字段最新结构caret，隐藏时结构selection回调只更新RAM；新字段/真正finish仍重置。来源/50app/83state报告SHA与实际APK静态在 evidence/m5/reference-ui-caret-checkpoint.json；45工具单测PASS。

局限：上述no-learn早期定位中断尚未确定原因；新179a随后reference-pixels在number阶段FAIL（reference-ui-caret-pixel-targets.json），只有Chinese/press-cancel两项通过，不能沿用旧c51或a871九项成绩。原无障碍窗口坐标/lifecycle仍FAIL；同字段五例不能关闭完整I03/I05/R04、学习/持久层/日志/性能或M6。

增量：原179a五态number阶段失败不覆盖。冻结0cfc1ea/95e283c帧/私密协议后，仅修改独立host/test的真实屏幕等待，不改主APK179a：reference-ui-caret-finalframe-pixel-targets 9/9（各实际键盘tap/capture三帧，按下保持期间三帧后CANCEL）、reference-ui-caret-stableframe-pixel-private 36/36（四敏感类型×九字素，正文getter0）、reference-ui-caret-pixel-private-queue 20/20（同20原精确oracle，额外确认第二续输键前仍pending）。两延迟例300ms未放宽；先验证稳定帧上的三个键面，待决期间按该外部观察帧注入，未桥接IC/服务坐标。长按8次结构手势，旧字段pending切换后正文不变，新字段仅一次c；全部敏感正文getter0。原无障碍门禁仍FAIL，此scope不取代。

179a API36专用模拟器full组件28结构故障/敏感5/metadata6及controller PASS，记录 m1/reference-ui-caret-emulator-{full,controller}.json。优化联合构建首次因生成测试源码未声明lint依赖FAIL，改TaskProvider后成功；b127fe87静态及真实Pixel同字段5/5通过。优化版其余scope必须分别绑定该SHA，不继承debug数据。
