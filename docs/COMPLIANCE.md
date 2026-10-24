# 开源合规与资源来源

项目原创程序与派生代码采用 GPL-3.0-or-later；第三方程序、数据、模型和研究快照保持原许可证。根 LICENSE 不重新授权第三方资源。实际来源、版本和哈希见[上游锁](../upstream-lock.json)与[NOTICE](../NOTICE.md)。

| 类别 | 来源与当前处理 |
| --- | --- |
| Trime 前端与派生代码 | 固定源码及递归来源，保留GPL版权、SPDX和修改账 |
| librime及原生插件 | 逐项保留原许可证、来源与固定commit；详见third_party清单 |
| 万象schema与词库 | 保留数据来源及各文件的授权说明，不以仓库根许可证覆盖例外 |
| 万象数据仓库工具 | 不集成未获许可的工具程序代码 |
| 语法模型 | 当前打包固定lotem compact数据模型；原Wanxiang LTS候选模型未打包 |
| Android/JVM与原生依赖 | 固定依赖版本、验证哈希和运行时清单，离线保留许可正文 |
| 字体与图形 | APK使用Android系统字形及原创图形，不打包Apple字体或原版图标 |

## 可复查清单

- `third_party/trime-sources.json`：递归源码来源。
- `third_party/trime-modifications.json`：上游修改及文件哈希。
- `third_party/runtime-inventory-release.json`：实际release依赖制品与POM来源。
- `third_party/offline-notice-sources.json`：离线许可正文与哈希。
- `third_party/android-probe-resources.json`：固定运行时资源。

当前已有APK、运行时清单和离线许可界面。完整词库二次来源、模型对应源及正式分发检查仍有未关闭项目，见[已知问题](OPEN_ISSUES.md)。这些状态独立于代码是否可公开阅读，不应改写成已完成全面审计。

## 贡献与发布

新增依赖或资源时记录官方来源、固定版本/commit、SHA256、作者、许可证、例外及修改。第三方原文保持原样；对授权范围不清的资源先核实再集成。

发布APK时同时提供对应源码、构建材料、NOTICE、资源来源和验证范围；核对制品与源码版本，不提交签名私钥、个人输入、用户词库或私人完整参考原件。README包含此次提供的Android与iOS完整截图直接拼接，不裁剪、不缩放、不生成界面。iOS截图仅供文档参考，不随APK打包，不由根GPL许可证重新授权；来源、排版说明和哈希见[截图说明](images/README.md)。
