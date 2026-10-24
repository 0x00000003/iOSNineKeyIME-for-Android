# 双机对照图生成说明

模式：内置imagegen，参考图合成，非CLI/API自建调用。

参考输入：

1. `android-nine-key.png`：本项目Android原始实机键盘，保留左右方向和键位。
2. iOS中文九宫格键盘裁图：只作参考，私人原件保持只读、不纳入Git。排除上方预测词栏及所有聊天内容。

生成提示要点（中文整理）：

> 制作一张用于GitHub README的横版双机对照图，左侧为有居中圆孔的Android手机，右侧为有宽刘海的iPhone，正面、等比例、并排。浅冷灰背景、柔和阴影、清晰的黑色键盘；上方分别标注绿色“Android”和蓝色“iOS”。标题逐字为“熟悉的九宫格，来到 Android。”。两台手机键盘上方仅有空白演示编辑区和绿色光标，不出现聊天、账户、人物、品牌logo或水印。左键盘以Android实机截图为参考，右键盘只使用iOS参考中的四排主键和底部工具区，不展示iOS预测词栏。保持123、#@¥、ABC/DEF/GHI/JKL/MNO/PQRS/TUV/WXYZ、选拼音、空格等位置和可读性，不交换、镜像或增加按键。保留两侧地球与麦克风工具。不得宣称像素等同。底部文字为“左：Android 实机键盘参考　右：iOS 键盘参考｜机身与编辑区为示意”。

人工检查：左右平台顺序、双机完整、标题与平台标注、主要键位可读、没有私人聊天或预测词、来源边界明确。此图为生成的展示示意，不替代原始截图或视觉差分测试。

## 最终英文提示

```text
Create ONE polished side-by-side comparison image for the GitHub README of 'iOSNineKeyIME for Android'. Use case: compositing / product-mockup. Wide landscape canvas, ideally 16:9, about 2400x1350. Calm premium editorial design, pale cool gray background, high contrast black phone screens, subtle soft shadows. Exactly TWO front-facing upright smartphones, equal visual scale, side by side. LEFT is an Android phone with a centered circular camera cutout and RIGHT is an iPhone with a wide notch. Clearly label above the LEFT phone 'Android' in green, and above the RIGHT phone 'iOS' in blue. One single prominent Chinese headline at top, verbatim: '熟悉的九宫格，来到 Android。' No other marketing claims, no logos, no Apple logo, no OpenAI logo, no chat history, no people, no watermark. Both phones have minimal clean neutral notes/editor UI above the keyboards, with only a blank editable area and a thin green cursor, no words or conversations. This upper app UI is a conceptual demonstration; do not reproduce private chat UI. Image 1 is the actual Android keyboard screenshot: use it for the LEFT phone lower screen. Image 2 is the supplied iOS keyboard reference: use its KEY GRID AND BOTTOM TOOLBAR ONLY for the RIGHT phone. CRITICAL PRIVACY: OMIT the iOS prediction row containing Chinese word suggestions near the top of image 2 entirely. Do not reproduce those words anywhere. The right input above the main keys should be blank. Make the main key grids and footer icons from both images as faithful and readable as possible; do not swap the references or mirror them. Show the full 4-row T9 keyboard and globe/microphone footer on each side, all keys and margins intact. Preserve the visible key labels and their positions from each source, including 123, ABC/DEF/GHI/JKL/MNO/PQRS/TUV/WXYZ, #@¥, 选拼音, 空格. Do not invent any extra keys. The Android left screenshot has an idle punctuation row; keep those punctuation marks, but not any loading message. Present a fair visual comparison, not a claim of exact pixel equivalence. Small legible footnote at bottom, verbatim: '左：Android 实机键盘参考　右：iOS 键盘参考｜机身与编辑区为示意'.
```
