# 多宿主/旋转增量（整体FAIL/WIP）

协议先冻结a260bbe；生产APK不变：优化990b62cc、debug fdc658b2。新增测试只修改独立合成宿主，复用现有构建环境，不读取其他应用数据/账号或私人输入。

- 当前优化版真实Pixel字段v4：9/10。Done/Send/Search/Next/Go各恰好一次action，数字/电话、URL英文、多行中文选词→换行→英文均PASS。邮箱仍被非本项目TYPE_APPLICATION窗口抢焦点，实际输入断言未执行，记录FAIL/准备受阻，不能据此断言键盘输入错误。没有操作外部窗口或导出其文字/包名，只有窗口类型/焦点及own-host/IME布尔。此前v1/v2零项执行、v3为8/10保留；v4先等待宿主焦点再请求显示输入法，各case独立记录，不让邮箱挡住其他项目。
- 早期焦点/触点结果存在外部窗口干扰；失败记录保留。邮箱阻碍暂保留，继续其他独立工作。
- 当前优化版真实Pixel本地WebView：2/3。禁JS/网络/文件/内容访问的固定内页，实际中文选词、删除、英文输入通过；密码直接输入后退格失败，敏感正文getter0。v4独立连接包装器确证requestCursorUpdates返回false，text-bounds/handwriting请求均0，结构路径在首步拒绝；不是已有TextView的36字素成绩可以覆盖。不得增加正文读取或未经验证DEL来伪装通过。
- 当前优化版真实Pixel旋转：3/4。合成Activity仅自行处理方向/尺寸配置，保留同一EditText；实际横屏紧凑键输入、竖屏返回、已提交文本保留均PASS，未确认组合跨旋转丢失FAIL。原方向请求已恢复，没改全局旋转/字体/导航。生产onStartInput每次新建字段会话的路径需要修复后按同4项重测。

记录：[字段v4](evidence/m2/compat-optimized-v4-fields-pixels.json)、[WebView v4](evidence/m2/compat-optimized-v4-web-pixels.json)、[旋转v1](evidence/m2/rotation-optimized-v1-pixel.json)。全部绑定实际主/host/test SHA与nonce。失败历史完整保留；不是Compose/全部应用/完整隐私或性能通过。

Android官方接口说明可辅助解释返回false的结构能力缺口：[InputConnection](https://developer.android.com/reference/android/view/inputmethod/InputConnection)。这是接口语义资料，实测结论仅针对上述APK/设备/provider，不推断所有WebView均不支持。旋转修复参考的AOSP固定研究源码：[InputMethodService 601f6eb](https://android.googlesource.com/platform/frameworks/base/+/601f6eb61d4ed746c4f3b0170cffa7a9642a2d87/core/java/android/inputmethodservice/InputMethodService.java)，其配置重建会重新调用onStartInput；源码研究不等于Pixel二进制一致或修复通过。

额外确认私密Latin视图将errorCode隐藏在无header的布局中，导致结构删除失败无可见说明；下一补丁应显示固定错误/重新连接入口，不能靠该提示把删除失败改为PASS。
