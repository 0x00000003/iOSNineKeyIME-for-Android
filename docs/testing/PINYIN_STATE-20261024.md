# 选拼音纯组件增量（WIP）

这是选拼音生产接入前的 JVM 组件测试，不是 Android、JNI、系统输入法或视觉验收。可安装检查点仍是 debug 5c46 / 优化 c37，按钮尚未提供真实音节选择。

新增 PinyinDisambiguation：从引擎注释提取兼容的明确音节，保留已确认范围和先前明确音节；匹配数字编码及精确编辑范围，拒绝未知格式、过期范围、越界和不兼容选择。声调/大小写/ü 归一化，不读宿主正文、不持久化、不新增依赖；输入最多128字符、每次最多处理100条长度不超过512的注释、输出最多32个不同选项。解析未知格式时返回空，不能猜测并改写输入。

真实执行 `./scripts/gradle.sh :state:test`：

| 阶段 | 结果 | 证据 |
| --- | --- | --- |
| 空实现 RED | 10执行、6断言失败、0错误/跳过 | [RED](evidence/m2/pinyin-state-red.json) |
| 实现 GREEN | 新10/10通过，全state 93/93，无跳过 | [GREEN](evidence/m2/pinyin-state-green.json) |

早先使用 Gradle `--tests '*PinyinDisambiguationTest'` 未发现 Kotest 测试，属于未执行，不算断言红测。随后完整任务实际执行获得上表红绿结果；未改门槛、未删除测试。原始报告仅留忽略目录，公开证据只含计数、SHA及范围，不提交机器名或断言正文。

与[真实主机探针](PINYIN_DISAMBIGUATION_PROTOCOL-20261024.md)分别记账。接下来仍需 session/revision/raw 三重检查、Android 原生改写、事件/异步队列、私密禁用、真实按钮与真机测试；不能将本次93个JVM测试代替这些工作。
