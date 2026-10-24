# 字段与本地WebView兼容协议（运行前冻结）

在已授权Pixel上对当前fdc658b2/990b62cc独立合成宿主运行真实系统IME。先测字段/action：email、URL直接英文；number、phone数字；multiline真实中文选词后换行再输入，5种Done/Send/Search/Next/Go恰好一次宿主action。宿主正文只作合成oracle比较，公开记录布尔/计数，不输出正文。

WebView只用自有本地内页，无INTERNET、禁文件/内容/网络、CSP禁脚本/外部资源/表单导航。实际触摸DOM输入框，确认系统IME显示并通过自身合成DOM/accessibility oracle核输入/删除/跨字段；不以页面加载成功代替系统输入通过。若目标OS/WebView不提供必要接口，保留具体失败，继续其他分项，不启用网络或降低安全约束。

旋转只允许对本项目合成Activity请求方向并在结束恢复，不改全局旋转/导航/字体设置。检查实际屏幕inset/触点仍可用、已提交合成正文保留且不重复；部分组合跨旋转行为单列，不把普通文本保持当组合保持。无障碍坐标原门禁仍独立FAIL，不借物理scope关闭TalkBack。Compose仍需独立真实宿主证据，不以EditText/WebView冒充。

复用现有工具链与APK池，先构建独立宿主/测试即可；只有发现生产缺陷才重构主包。每条结果绑定实际IME/host/test SHA与nonce，失败不覆盖；不触及第三方应用或真实私人数据。
