# Pixel 连续原生引擎观察

协议v3在测量前本地提交6ae38b3，模型开/关各1800秒，120键预热后固定8键/秒，不追赶延迟；同字段连续会话，不在每个词后重建session。每次计时含processKey/getContext/最多100候选，使用合成词且关闭学习，逐次核对提交仅一次。早期混入session冷启动的v1/v2短测FAIL保留，不能改名为连续输入成绩。原协议见[NATIVE_PERF_PROTOCOL](NATIVE_PERF_PROTOCOL.md)。

| 模型 | 键数 | exact-once commits | P95 ms | P99 ms | 最大进程PSS KiB | 末次累计GC |
| --- | --- | --- | --- | --- | --- | --- |
| on | 14340 | 2264 | 12.096 | 15.613 | 67737 | 33 |
| off | 14353 | 2266 | 11.128 | 14.681 | 37696 | 30 |

两次各31次内存/温度采样，thermal status均0，自己UID收发增量均0；profile断言在native关闭前比较，共享树包含关闭后检查。未测的profile关闭后状态不能由此补称通过。模型映射40927232字节是地址映射长度，不是RSS/PSS；关闭模型映射及grammar query均0。

原记录：[model-on](evidence/m1/m5-steady-pixel-perf-model-on-long.json)、[model-off](evidence/m1/m5-steady-pixel-perf-model-off-long.json)，[62条采样CSV](NATIVE_PERF_SAMPLES-20261023.csv)。制品为优化非debuggable、本项目调试证书签名release `94b9d40f9462fd428442ccff4fa249c5873105c99156971e41cbb15490a4f162`，test `ee19b44e393fd2f8507a53c90301b1355ed5b76e2a05e731e35e6a3da93965f3`；当时尚含后续移除的图标字体，也没有后续宿主异常防护/原生分页代码。冻结制品位于忽略artifacts/m5/steady-input-apk-set，不能将此成绩归给新APK。

分项JNI候选时间满足80/150ms预算；未包括触控到画面帧、系统IME启动/宿主/旋转/滑动或完整模型常驻服务。P01—P04整体、正式M5及M6发布验收仍未通过，视觉参考缺失仍BLOCKED。
