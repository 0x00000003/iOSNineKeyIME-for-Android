# 参考资料与可访问性

## 外观参考

| 参考编号 | 目标 | 当前状态 | 对验收影响 |
| --- | --- | --- | --- |
| REF-IOS-01 | iPhone13原生中文九宫格截图/录屏 | NOT_AVAILABLE | 未知iOS版本/布局/尺寸/状态行为；S01/S02/D10 BLOCKED |
| REF-PIXEL-01 | 原对话Pixel键盘截图 | NOT_AVAILABLE | 现状对比/屏幕比例待补，不影响引擎探针 |

补图后把脱敏、经用户允许纳入Git的参考放 `docs/reference/images/`；默认私人原图放忽略的 `.private/reference/`，只提交无隐私键盘裁图。另存标注/叠图，不覆盖原件。建立图片登记：来源、允许用途、SHA256、像素宽高、系统版本/密度/字体/主题/导航、输入场景、裁图边界。不能从公共网络找张iOS键盘图充当用户原图。

## 上游证据
`upstream/` 是按固定commit保存的少量许可、构建/清单/schema/Lua等研究快照，以及GitHub tree/release API元数据；这些资料仍归原作者，**不是已集成的应用源码**。不可作为构建资源直接扫描后宣称最终APK安全。来源URL与每个下载文件SHA256在 [依赖锁](../../upstream-lock.json)。不要修改快照以匹配期望；需要新证据另取新版本并保留旧记录。

网页版官方资料（访问2026-10-23，动态页面不作为版本锁）：
- [Trime仓库](https://github.com/osfans/trime) / [v3.3.12](https://github.com/osfans/trime/releases/tag/v3.3.12)
- [万象v18.1.7](https://github.com/amzxyz/rime-wanxiang/releases/tag/v18.1.7)
- [万象Android/Trime说明](https://amzxyz.github.io/rime-wanxiang/doc/trime.html)
- [万象键数转写说明](https://amzxyz.github.io/rime-wanxiang/doc/layouts.html)
- [librime1.17.0](https://github.com/rime/librime/releases/tag/1.17.0)
- [万象数据仓库授权边界](https://github.com/amzxyz/rime-wanxiang-data/blob/52328d36137a68ea9f9b6855a895ac0417b6b6a1/LICENSE-SCOPE.md)
- [Android输入法官方指南](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)
- [Android备份官方指南](https://developer.android.com/identity/data/autobackup)
