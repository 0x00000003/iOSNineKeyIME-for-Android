# 项目展示图片

## 原始截图对照图

[android-ios-comparison.png](android-ios-comparison.png)为README首屏配图：左侧Android本项目输入法，右侧iOS原生键盘。两侧均取自原始截图，统一裁到主键区后等比例缩放、并排放置；没有生成机身、编辑区或键盘像素。私人聊天和预测词栏未展示，私人完整原件不提交。

来源为[Android完整键盘截图](android-nine-key.png)和[iOS原图键区裁图](ios-nine-key-area.png)。导出的左右图块逐像素等于对应原图裁剪缩放后的像素；无调色、锐化、文字替换或界面重绘。排版方式和复现命令见[comparison-layout.md](comparison-layout.md)，来源、裁剪框、输出框和哈希见[comparison.json](comparison.json)。

两侧按相同显示宽度排版，原始分辨率、系统字形和图标差异保留。此图不代表全部状态或像素一致性验收。iOS裁图仅用于文档参考，不随APK打包，也不由根GPL许可证重新授权。

## Android原始实机截图

下述三张图片来自本项目在Android上实际运行的系统输入法，使用独立合成宿主。画面仅包含本项目键盘，不包含私人聊天、账号、密码或其他应用内容；未修改键盘像素，也未使用iOS截图替代实现。

| 图片 | 内容 |
| --- | --- |
| [android-nine-key.png](android-nine-key.png) | 中文九宫格与空闲标点栏 |
| [android-latin.png](android-latin.png) | 英文 QWERTY |
| [android-symbols.png](android-symbols.png) | 数字和符号 |

主APK SHA256：`91350a65112bebb3d7a92db11d1d3830fdefe2bc71da831fb1e59ab83b6d7ec1`。系统真实触屏回归9/9通过，执行结果、主/host/test哈希与nonce见[截图执行记录](../testing/evidence/m2/readme-android-reference-ready.json)。逐图尺寸及SHA256见[screenshots.json](screenshots.json)。

这些截图用于展示实际界面，不代表所有设备、字体、主题或无障碍场景均已验收。已知限制见[当前状态](../STATUS.md)。
