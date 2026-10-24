# 项目展示图片

## 完整截图对照图

[android-ios-comparison.png](android-ios-comparison.png)为README首图，直接拼接此次提供的两张完整截图：左为[Android](android-fullscreen.png)，右为[iOS](ios-fullscreen.png)。不裁剪、不缩放、不添加内容，不生成机身或编辑区。输出左右两半已逐像素核对完整输入，原文件保持只读。

复现方式见[comparison-layout.md](comparison-layout.md)，来源尺寸与哈希见[comparison.json](comparison.json)。这些截图用于文档展示，不随APK打包，不由根GPL许可证重新授权，也不代表全部状态已验收。

## Android原始实机截图

下述三张图片来自本项目在Android上实际运行的系统输入法，使用独立合成宿主。画面仅包含本项目键盘，不包含私人聊天、账号、密码或其他应用内容；未修改键盘像素，也未使用iOS截图替代实现。

| 图片 | 内容 |
| --- | --- |
| [android-nine-key.png](android-nine-key.png) | 中文九宫格与空闲标点栏 |
| [android-latin.png](android-latin.png) | 英文 QWERTY |
| [android-symbols.png](android-symbols.png) | 数字和符号 |

主APK SHA256：`91350a65112bebb3d7a92db11d1d3830fdefe2bc71da831fb1e59ab83b6d7ec1`。系统真实触屏回归9/9通过，执行结果、主/host/test哈希与nonce见[截图执行记录](../testing/evidence/m2/readme-android-reference-ready.json)。逐图尺寸及SHA256见[screenshots.json](screenshots.json)。

这些截图用于展示实际界面，不代表所有设备、字体、主题或无障碍场景均已验收。已知限制见[当前状态](../STATUS.md)。
