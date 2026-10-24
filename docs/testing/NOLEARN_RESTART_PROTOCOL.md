# 预准备无学习profile跨重启调查（测量前冻结于1cf603d）

当前产品敏感字段仅直输英/数；本调查不启用隐私中文，不修改既有零上下文/零写策略或R04。调查独立可行性：先在基线外准备同一固定no-learning profile，3次合成commit后关闭，等待1100ms以排除同秒元数据未变假象；记录整树内容/mtime基线，fullCheck=false重启同profile，不显式重新select schema，确认默认nolearn/初始history0，20次合成提交核对仅一次，关闭后整树必须不变且无userdb，共享资源整树也必须不变。仅记录布尔/计数，原词/候选/digest不落记录。

若失败，先定位启动/关闭写入，不用只跳过重启阶段或缩小目录断言来宣称完整私密模式通过；若成功，也只证明预先准备条件的native组件，不能替代全新安装首个敏感字段、宿主零读取、服务生命周期或完整I05。产品是否能提供隐私中文还需独立架构/全路径测试，不先改UI承诺。

## 冻结协议结果

主APK `338bb5becadc221a229ef9904ab5c188aff990b96fce3bf0122fac60926d85f0` / test `e0090c9ebf67fed506f859d012b7d3fbfb9e81c1b4425ed57c76bc0a866849de`，[Pixel](evidence/m1/m2-native-pixel-nolearn-restart.json)与[专用模拟器](evidence/m1/m2-native-emulator-nolearn-restart.json)均PASS：全部原断言通过，包括完整profile重启后的即时基线和输入/关闭后整树字节/mtime不变、无userdb。准备阶段仍在基线外，1100ms时钟分隔保持，未改变产品策略。该结果支持进一步设计调查，不是完整I05或新安装首个敏感字段验收；本版本私密中文仍未实现。
