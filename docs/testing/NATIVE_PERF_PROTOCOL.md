# 独立JNI性能观察协议（先冻结，不关闭P01—P04）

M4检查点b4fb49d之后增加，先于实际测量提交协议/测试源。测试仅本项目release进程、自有合成profile、8key/s固定三组digits；120 warm-up keys后，smoke10秒或long1800秒。每键processKey+context+前20候选计时，完整词首候选按内存oracle核对且仅一次提交，字段会话重置；用户学习禁用，两schema显式model on/off overlay，loaded与query计数控制。原固定质量语料/协议不修改。

测量P50/P95/P99、每60秒进程total PSS/private dirty/GC/thermal及模型mmap地址范围（不是RSS），own UID流量差（若API不可用明确unsupported）；不输出输入正文、候选、路径、profile digest、原始maps或logcat。内存包含独立instrumentation开销。私有profile关闭前和公共共享资源整树字节/mtime必须不变，commit必须无丢失/重复，native model on实际query>0/off0，违反即测试FAIL。

P01触控反馈与帧、P02完整键盘20次启动/部署、P03完整IME模型on/off长期增长、P04旋转/字段/宿主oracle全部仍未验收。此组件即使PASS也不勾选R15。350MiB预算只列观察值，不能用组件过程代替完整输入法门禁。不得降低PRODUCT既定阈值，不把短跑外推30分钟。测试不启用IME、不改Pixel网络/锁屏，不读取其他应用。
