# 未解决项与接续边界

当前更新：旋转组合丢失已在e309/913两APK各自真实Pixel7/7修复；私密错误提示已实现，最终debug模拟器View通过。WebView密码结构删除入口返回false仍FAIL，正文getter0；邮箱非自有焦点窗口未操作、原因未定；原a11y坐标仍FAIL。见[旋转修复](testing/CONFIGURATION-20261024.md)。旧失败不改写。见[兼容红测](testing/HOST_COMPAT-20261024.md)，这些不能被旧TextView/物理scope成绩覆盖。

选拼音已接入并通过两个当前APK的Pixel真实组件/系统8项测试，当前debug fdc658b2 / 优化990b62cc。见[新检查点](testing/PINYIN_ANDROID-20261024.md)。不再把该按钮列为未实现；下列旧制品成绩为历史范围，剩余重点是多宿主/旋转、a11y、完整隐私与性能、视觉差异和M6交付收尾。

2026-10-24更新：5张参考和已授权Pixel均可用，debug5c46/优化c37的实际触屏9/5/36/20已有独立PASS；具体未知显示设置、字形/Emoji/Android导航差异和原a11y窗口坐标FAIL仍在，不能继续把“无图/无启用授权”当作当前阻碍。最新范围见testing/CORNERS-20261024.md。

本表不是应用完成声明。不存在已验收的可日用IME；当前APK含五态参考前端/薄服务开发检查点。所有阻碍和未实现工作不能合并称为“全部测试通过”。

| 项目 | 状态 | 具体原因 / 下一步 |
| --- | --- | --- |
| 原万象LTS模型授权 | BLOCKED（不打包，已有替代路径） | 具体LTS gram已核哈希、host格式及4查询通过。根CC-BY和数据scope未对该release gram直接列明；保留README“数据资产”与release“模型也是词库”的证据，需进一步确认具体许可归属后打包。不集成data仓库未授权程序。已另用有命名数据授权证据的lotem LGPL compact替代模型，host/Android实际加载与受控学习通过并已打包；原模型疑点不再阻碍替代路径。分发对应源/完整NOTICE和质量R08/R10仍待验收。 |
| R04敏感预存组合音标 | 组件PASS / 系统分项PASS / 整体WIP | 历史DEL路径4/5 FAIL保留。当前debug5c46与优化c37各自真实跨UID系统36字素/20隐私队列通过，无手工cursor/IC桥，敏感正文getter0；debug双设备full的敏感5/metadata6/28结构故障也通过。Compose/WebView/跨行/复杂RTL与完整隐私持久层仍未验收；未知结果失败关闭，不宣称R04整体PASS。见testing/CORNERS-20261024.md。 |
| M5参考图/体验确认 | WIP / 部分条件待确认 | 五张深色图已实现、实拍并做宽度归一测量，iPhone13/iOS27.2已确认；显示缩放/文字大小、浅色/按下原图/其他状态/录屏与用户体验签收仍缺。系统字体/Emoji/导航条及固定常用列表有差异，不称1:1。 |
| M1冷部署与可重建 | WIP | 双profile debug探针耗时大，已验证构建时公共词典预编译并在Pixel/离线模拟器运行，大幅减少本轮debug探针观察耗时；历史2ad源码已用新SDK/JDK/初始空Gradle cache复现整包，完整当前制品分发对应源/许可闭包仍待收尾。 |
| M2实际IME和多宿主 | 原a11y门禁FAIL / 物理子协议PASS | debug5c46与优化c37各自在独立UID宿主真实触屏五态9/同字段重显5/敏感字素36/异步队列20通过，敏感正文getter0；原a11y窗口坐标/lifecycle仍FAIL。debug双设备view20、full28结构故障/敏感5/metadata6和controller通过；Compose/WebView实际输入、旋转/复杂多行仍未验收。见CORNERS记录；831原core10是历史版本。 |
| M3/M4完整功能与安全 | 组件PASS / 全路径WIP | 同优化c37两设备engine/resources/quality/learning/context分项PASS：固定100词20句Top5为99/100、19/20，十词学习/重启/清合成库10/10，十组上下文四格9/10改善；全路径敏感/全部文件与日志/目标OS备份迁移仍未验收，组件不能替代完整安全。 |
| M5/M6性能/发布 | NOT_RUN | 本地调试证书的优化release引擎可运行，许可界面/初步手册已接入；旧固定APK模型开/关30分钟JNI观察完成。正式签名、系统IME性能20次、触控/帧/宿主压力和同APK全门禁仍未完成。按发布流程提供制品。 |

测试前确认设备处于解锁状态；仅切换已启用的本项目debug/release。未更改Pixel网络、锁屏或其他应用数据。专用模拟器网络关闭由本项目隔离测试完成。

M5重显阻碍（2026-10-24 WIP）：新五态实际绘制完整，但AccessibilityWindowInfo/根窗口坐标落在屏幕下方，footer节点不可访问，真实reference-ui/lifecycle未通过。多组最小窗口/查询A/B失败保留；旧临时UI同手机对照core10/10、首次新UI原三包复测9/10（send丢失）均不能当当前修复。三态键框/色块接近不等于完整视觉通过，英文/Emoji截图、字号/图标/选拼音/各门禁继续独立推进。

最新c51增量：英文/Emoji已通过独立屏幕像素触控并补齐六张五态/按下实拍，9/9与上部宿主按钮通过；并非仍缺这五态的图。原无障碍坐标/lifecycle仍FAIL，透明整窗/模式事件不作为已修复。字号/线图已实拍校准，仍有系统字形、Emoji固定默认/外形、原生导航等差异；逐音节选拼音、其他参考状态和完整系统安全/性能/release仍未完成，不能宣布可验收交付。
