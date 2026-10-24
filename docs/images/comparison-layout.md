# 完整截图直接拼接

使用此次提供的两张完整截图：第一张Android，第二张iOS。两图高度均为1280像素，直接左右粘贴，合成1165×1280的PNG。

- 左侧：`android-fullscreen.png`，574×1280。
- 右侧：`ios-fullscreen.png`，591×1280。
- 不裁剪、不缩放、不加标题、不调色、不重绘、不生成其他内容。
- 发布输入为原JPEG解码后的无损PNG，不包含原文件的额外元数据。

复现：

```sh
python3 scripts/compose_keyboard_comparison.py
```

脚本核对输出左右两半逐像素等于完整输入。来源尺寸、文件哈希和输出坐标见[comparison.json](comparison.json)。截图展示具体输入状态，不表示所有宿主行为或像素一致性已验收。
