# 独立JNI性能观察协议（先冻结，不关闭P01—P04）

M4检查点b4fb49d之后增加，先于实际测量提交协议/测试源。测试仅本项目release进程、自有合成profile、8key/s固定三组digits；120 warm-up keys后，smoke10秒或long1800秒。每键processKey+context+前20候选计时，完整词首候选按内存oracle核对且仅一次提交，字段会话重置；用户学习禁用，两schema显式model on/off overlay，loaded与query计数控制。原固定质量语料/协议不修改。

测量P50/P95/P99、每60秒进程total PSS/private dirty/GC/thermal及模型mmap地址范围（不是RSS），own UID流量差（若API不可用明确unsupported）；不输出输入正文、候选、路径、profile digest、原始maps或logcat。内存包含独立instrumentation开销。私有profile关闭前和公共共享资源整树字节/mtime必须不变，commit必须无丢失/重复，native model on实际query>0/off0，违反即测试FAIL。

P01触控反馈与帧、P02完整键盘20次启动/部署、P03完整IME模型on/off长期增长、P04旋转/字段/宿主oracle全部仍未验收。此组件即使PASS也不勾选R15。350MiB预算只列观察值，不能用组件过程代替完整输入法门禁。不得降低PRODUCT既定阈值，不把短跑外推30分钟。测试不启用IME、不改Pixel网络/锁屏，不读取其他应用。

10秒首次smoke model-on/off均观察到JNI合计P95约157/163ms，仅78/77键，不能写P01通过或外推长测。保持相同调用顺序/协议/阈值，后续追加processKey/getContext/getCandidates独立计时用于定位，测量前再提交此补充；不修改模型/语料/原断言。主APK仍94b9d40f时只更新测试APK哈希，结果分别绑定。

revision2在测量前固定：独立分段计时不变，候选上限由20改为100，与当前SerialRimeEngine实际snapshot一致（首轮20仅探索结果保留）。追加candidate_limit和明确component_candidate_budget_p95_80_p99_150_met/full_product_performance_acceptance字段，组件断言pass不等于性能预算通过。尚不改变产品候选/排序逻辑。

revision2分段测量实际processKey P95≈161ms，context≈1.7ms、candidates≈4.9ms。代码检查发现协议每词末resetRimeSession会销毁整个引擎session，下一词首键承担schema/components重新创建，混合了字段冷启动，不是P01所定义的预热后连续输入。保留v1/v2原结果及false预算字段。revision3在测量前冻结：warmup后只clearComposition，词末不reset session，字段历史按引擎原有20条上限保留，学习仍禁用；显式记录session_reset_per_commit=false，其他调用顺序/固定三词/100候选/8key/s/80与150ms预算不变。该修正把连续同字段与冷启动区分，不能据此宣称P02启动通过；实际服务启动仍需另测。若持续JNI仍超预算，继续定位，不改变门槛。
