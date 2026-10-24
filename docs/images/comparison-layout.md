# 原始截图并排方式

首图不使用生成模型。输入为Android实机PNG和iOS原始键区PNG，仅进行裁剪、等比例缩放和排版；文字标注在截图区域之外。

- 左侧：`android-nine-key.png`，取`[0,219,960,920]`，保留主键及底部工具区。
- 右侧：`ios-nine-key-area.png`，来自iOS键盘参考的`[0,134,591,566]`，去掉预测词栏，保留主键及底部工具区。
- 两侧各显示宽648px，按各自比例计算高度；不拉伸、不调色、不改变按键或图标。
- 标题和平台说明使用本机字体，字体文件不复制、不打包。

使用已有Pillow环境执行：

```sh
python3 scripts/compose_keyboard_comparison.py --font /path/to/local-cjk-font.ttf
```

脚本校验源图哈希未变，并校验导出PNG中左右区域逐像素等于裁剪、缩放后的输入图块。图像来源、裁剪与输出坐标见[comparison.json](comparison.json)。比较图仅展示该截图的主键区，不是全屏截图或全部视觉状态验收。
