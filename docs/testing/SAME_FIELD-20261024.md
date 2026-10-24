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
