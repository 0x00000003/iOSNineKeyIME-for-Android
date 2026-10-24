# Android 工程

`trime/`为Android构建根，包含固定Trime派生源码、项目输入法、纯状态组件与合成测试宿主，作为同一Git仓库的一部分管理。

在仓库根目录执行 `./scripts/gradle.sh :app:assembleDebug`。工具链、签名和测试流程见[开发手册](../docs/DEVELOPMENT.md)，功能及兼容范围见[当前状态](../docs/STATUS.md)，第三方来源见[NOTICE](../NOTICE.md)。
