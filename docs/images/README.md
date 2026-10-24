# 项目展示图片

## 双机对照图

[android-ios-comparison.png](android-ios-comparison.png)为README首屏配图：左侧Android本项目输入法，右侧iOS参考键盘。使用下述Android原始实机图和提供的iOS键盘裁图作为参考，由内置imagegen生成，机身和空白编辑区是示意。未展示私人聊天或iOS预测词栏；私人原件不提交。

生成过程可能重绘键盘细节，此图用于解释项目定位，不是原始截图直接拼接，也不是像素一致性、设备兼容或完整功能证据。原始Android图片和测试记录单独保留。生成提示见[comparison-prompt.md](comparison-prompt.md)，来源哈希与生成图哈希见[comparison.json](comparison.json)。

## Android原始实机截图

下述三张图片来自本项目在Android上实际运行的系统输入法，使用独立合成宿主。画面仅包含本项目键盘，不包含私人聊天、账号、密码或其他应用内容；未修改键盘像素，也未使用iOS截图替代实现。

| 图片 | 内容 |
| --- | --- |
| [android-nine-key.png](android-nine-key.png) | 中文九宫格与空闲标点栏 |
| [android-latin.png](android-latin.png) | 英文 QWERTY |
| [android-symbols.png](android-symbols.png) | 数字和符号 |

主APK SHA256：`91350a65112bebb3d7a92db11d1d3830fdefe2bc71da831fb1e59ab83b6d7ec1`。系统真实触屏回归9/9通过，执行结果、主/host/test哈希与nonce见[截图执行记录](../testing/evidence/m2/readme-android-reference-ready.json)。逐图尺寸及SHA256见[screenshots.json](screenshots.json)。

这些截图用于展示实际界面，不代表所有设备、字体、主题或无障碍场景均已验收。已知限制见[当前状态](../STATUS.md)。
