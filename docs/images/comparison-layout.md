# 完整截图对照排版

使用此次提供的两张完整截图：第一张Android，第二张iOS。两图高度均为1280像素，原尺寸并排展示，合成1277×1468的PNG。

- 左侧：`android-fullscreen.png`，574×1280；绿色边框，标注“Android / 本项目输入法”。
- 右侧：`ios-fullscreen.png`，591×1280；蓝色边框，标注“iOS / iPhone 原生键盘 · 参考”。
- 标题、边框和间隔均位于截图外；截图本身不裁剪、不缩放、不调色、不重绘。
- 发布输入为原JPEG解码后的无损PNG，不包含EXIF、定位或个人元数据；Android输入保留标准sRGB色彩配置。
- 标签使用本机字体渲染，字体文件不进入仓库或APK。

复现（Pillow版本见根目录的`requirements-visual.lock`，`--font`可指定本机支持中文的字体）：

```sh
python3 scripts/compose_keyboard_comparison.py --font '/System/Library/Fonts/STHeiti Medium.ttc'
```

脚本核对输出两个截图区域逐像素等于完整输入，并检查源PNG的SHA256未变。来源尺寸、文件哈希、标签和输出坐标见[comparison.json](comparison.json)。截图展示具体输入状态，不表示所有宿主行为或像素一致性已验收。
