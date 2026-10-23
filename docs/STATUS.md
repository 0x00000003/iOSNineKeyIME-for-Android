# 当前状态

日期：2026-10-23（America/New_York）。M1兼容性与M2独立纯组件开发检查点，**不是应用完成或可日用输入法**。本地前一提交`de7b919`，本轮补丁见Git与下列记录；开发记录。

| 项目 | 真实状态 | 证据/范围 |
| --- | --- | --- |
| Trime固定递归源码与构建 | 探针PASS | 22来源/4928文件；基线、debug、test、unsigned release构建 |
| 真实引擎九键/多音节/部分选词 | host/Pixel/模拟器探针PASS | 3组合成词、部分选择保留组合、最终完整且仅一次提交 |
| 普通学习正对照 | 单词探针PASS | 非首词20次后2→1，重启仍1；不是R09十词验收 |
| APK权限与备份XML | debug/release静态PASS | extended-debug-apk/extended-release-apk；不是目标OS备份/迁移验收 |
| 首装离线 | API36专用模拟器探针PASS | 原d04f618e首次安装；新扩展版本为回归，不能混用条件 |
| 纯状态/触控组件 | 38项JVM PASS | U01—U04/U06部分覆盖，无真实IME/宿主/Android视图验收 |
| Android JVM与lint | 41项JVM、debug/release lint PASS | 含6项敏感元数据策略；保留lint警告 |
| 密码/隐私零写持久层 | NOT_RUN | 决策测试不是I05/SEC04文件差异证明 |
| 398MB模型 | host格式/查询PASS，未打包 | 4固定查询；授权边界与Android加载/性能仍待完成 |
| iPhone/Pixel参考原图 | BLOCKED | 无附件；M5视觉不验收，不声称1:1 |
| 完整IME/质量/上下文/性能/发布 | NOT_RUN | M1完整资源与隐私门禁未关闭，M2集成至M6未完成 |

最新记录：[M1真实执行](testing/M1-20261023.md)、[M2独立组件](testing/M2-20261023.md)。最终扩展debug SHA256 `7ddd343a07b363e33e74ef2383f7afd273e05b64a6e9ec8eb8e7e06ab4ddba23`；unsigned release `ad4778853a2c1c340c317ba35c8ea168f32b36bccd119e4626c15ee65e8a0cfd`。它们是开发探针；release未签名/运行，不能当M6交付。

接续：先补M1学习关闭/敏感零写、模型授权及Android加载、资源与传递许可闭包；同时完成允许独立验证的纯状态/提交协议。测试设备应连接且保持解锁；不得启用/切换日用默认键盘。参考图缺失保留M5阻碍，继续独立可做任务。
