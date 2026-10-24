# 显示后高度退化回归 v1（运行前冻结）

由reference-ui-keyid-pixel-matrix实际7px窗口FAIL触发。独立UID、实际系统IME，无事件/坐标桥。lifecycle scope固定5轮：新send字段→普通宿主公开IMM hideSoftInputFromWindow→等待IME inset不可见且动画结束→新send字段show→验证own globe/window/nav边界稳定→真实触控64426选词再逗号→宿主内存中精确一次你好+逗号。输出仅案例ID/布尔/窗口几何/三SHA及nonce。scope开始时若已经退化也必须FAIL，不能偷偷force-stop或通过重装绕过。

不修改系统默认第三方输入法、网络或其他应用，不清除学习库。5轮不是完整5000事件压力或所有生命周期通过；需加矩阵五态和全核心/敏感回归。固定高度修复未通过本scope前只能WIP。
