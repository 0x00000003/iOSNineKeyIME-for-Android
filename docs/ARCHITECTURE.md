# 架构与集成方案

## Android 前端

派生固定 Trime 源码并复用其 Android 生命周期、JNI 与 librime 集成。当前输入服务 `NineKeyInputMethodService` 继承同源 `LifecycleInputMethodService`，连接项目自己的状态控制器、宿主适配器和九宫格视图，未注册上游完整旧前端。

Android 源码位于 `android/trime/`，固定递归来源与修改账见 `third_party/trime-sources.json`、`third_party/trime-modifications.json`。上游许可证与来源不随项目命名改变。

## 输入链路

```text
NineKeyView → KeyboardController / Reducer → SerialRimeEngine → JNI / librime
                              ↓
                       HostEditorAdapter → 当前 InputConnection
```

纯状态层管理布局、组合、候选、空闲标点与 session/revision；视图负责绘制、命中和短暂面板状态。串行 worker 与原生进程租约约束 Rime 调用，宿主文本通过单一提交口发送，异步结果按字段和修订检查。

候选使用原生20项窗口加1项 lookahead，按全局索引分页。选拼音显式查询至多100条注释，产生最多32个有界音节；引擎和 JNI 共同校验原始组合、未确认数字范围与已选段，只改当前组合，不自动提交或重置已选前缀。

## 资源与本地数据

运行时内置固定万象九键资源与 lotem compact 数据模型；未打包原 Wanxiang LTS 候选模型。实际制品与许可范围以 `upstream-lock.json`、`third_party/android-probe-resources.json` 和 NOTICE 为准。公共词典预编译后与只读资源一同校验，用户学习库与公共数据分开，部署和修复不覆盖学习库。

普通字段允许本地学习；敏感字段不进入中文候选、上下文和持久化路径。结构删除仅消费必要的当前会话坐标/边界，使用 API34+ cursor anchor、TextBoundsInfo 与宿主字符手势；不读正文，不以 DEL 掩盖未知字素边界。失败或不支持时显示错误，可重新连接而不重放失败编辑。

## 视图与生命周期

`ReferenceGeometry` 输出键面与触控框，`NineKeyView` 共享命中几何并使用 Android 字形和原创图形。中文、数字、符号、英文和 Emoji 面板具有明确返回路径；Emoji 搜索只在内存，结束会话清除。绘图高度与48dp触控区域分别处理，保留系统安全 inset。

配置重建可保留同字段普通中文组合；异步重启还需框架 restarting 标记、binding token、字段/策略/结构光标、会话及时间窗口共同匹配。新敏感字段、隐藏或结束遵循隔离和清理规则。当前窗口策略及更广宿主兼容仍有[已知问题](OPEN_ISSUES.md)。

## 验证边界

`:testhost` 为独立 UID 的合成宿主，无原生引擎依赖，通过实际系统 IME 触屏输入核对自身文本。组件探针和系统宿主记录分开；[决策记录](DECISIONS.md)保留实现取舍与历史实验，[当前状态](STATUS.md)列出已验证范围。
