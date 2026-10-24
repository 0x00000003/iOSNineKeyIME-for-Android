# 当前状态

当前为可安装的开发版本，已实现中文九键、逐音节选拼音、候选分页、空闲标点，以及数字/符号/英文/Emoji面板。完整发布验收仍在进行。

## 已验证范围

| 制品与范围 | 结果 | 记录 |
| --- | --- | --- |
| debug e3097893 / 优化91350a65，各自Pixel系统触屏 | 旋转7/7、选拼音8/8 PASS | [配置与拼音](testing/CONFIGURATION-20261024.md) |
| 优化91350a65，README采图时的五态系统触屏 | 9/9 PASS | [截图记录](testing/evidence/m2/readme-android-reference-ready.json) |
| debug e3097893，Pixel/API36模拟器组件 | 两设备各自view20+错误提示2项、pinyin和full PASS | [版本检查点](testing/evidence/m5/configuration-final-checkpoint.json) |
| 前一fdc658b2 / 990b62cc制品，各自Pixel系统触屏 | 选拼音8/8、五态9/9、重显5/5、敏感删除36/36、异步队列20/20 PASS | [选拼音检查点](testing/PINYIN_ANDROID-20261024.md) |
| 当前源码的主机验证 | 104 state、50 debug app、50 release app、45工具测试；构建/lint及两实际APK静态审计PASS | [版本检查点](testing/evidence/m5/configuration-final-checkpoint.json) |

以上结果绑定各自APK、测试包与执行范围，不将旧制品成绩直接转移到新制品。README首图为双机展示示意，Android原始运行截图单独保留，详见[图片来源](images/README.md)。

## 已知限制

- WebView密码字段的结构删除仍FAIL；错误提示与重新连接后继续输入已通过验证。
- 邮箱字段存在外部窗口焦点干扰，输入断言未完成。
- 原系统无障碍窗口坐标与部分生命周期门禁仍FAIL。
- 当前版本的完整性能、运行时隐私/安全、首次离线、备份迁移、许可闭包、正式签名和同APK全量回归尚未关闭。
- 当前检查包为ARM64；已测试环境不代表所有Android设备。Android系统字体、Emoji、导航与部分交互和iOS存在差异。

详细问题见[OPEN_ISSUES](OPEN_ISSUES.md)，验收对应关系见[ACCEPTANCE](ACCEPTANCE.md)。历史实现与真实失败保留在[PROGRESS](PROGRESS.md)、[DECISIONS](DECISIONS.md)和 `testing/`。
