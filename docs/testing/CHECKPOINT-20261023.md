# 本地开发检查点：未达到发布门禁

本轮基于615a477及随后WIP提交的代码/构建锁补丁；最终源版本以本地Git提交为准，测试用APK/Test APK完整SHA锁定。日期2026-10-23（纽约）；部分原始UTC时间跨到24日。没有远端、push或公开发布。

## 最终制品

| 制品 | SHA256 | 字节 | 使用范围 |
| --- | --- | --- | --- |
| debug | dd8f688f6c8bdc1cb3384e5e9a023b63384a1083641d23bde60af7a1a37dcb7c | 73209823 | 真实JNI/触控/宿主组件回归 |
| debug test | 68bc0af2f9d03b6f995346c51ae0b2903797d5e61134ad227f2435a188f918c1 | 358992 | 包含必测分页及R04原断言 |
| optimized local signed release | dedeee3c9f43e69ef608e01f33db1b6e7382bca89096fa6aabff48a29c9a1642 | 61728608 | 非debuggable、R8优化，本项目调试证书，仅本地验证 |
| release test | ee19b44e393fd2f8507a53c90301b1355ed5b76e2a05e731e35e6a3da93965f3 | 149407 | 只接受明确的引擎/资源/质量/学习/上下文/性能组件scope |
| default unsigned release | 9cdd4df28cd25e1fb19ebd2438f4078d99c1890514b5dda0ac1bdb97c3bff0c1 | 61720617 | 未签名，不可直接安装；23个非签名ZIP entry与上项相同 |

冻结树在忽略artifacts/m2/native-paging-final-apk-set和native-paging-final-release-apk-set，探针使用--apk-root避免其他构建覆盖；每次匹配已安装base APK SHA、使用nonce拒绝旧结果。最终三变体实际资产/manifest/备份XML/零字体均PASS，原生库SHA66ae89210f5f8421726ee16250ec688d78fcda49bf9e6f6d4fcd14580752c722。

## 实际结果

- 49 app、73 state JVM，debug/release lint与默认构建PASS；优化本地release+test构建PASS；38工具测试PASS（Python不是Android验收）。
- 同debug哈希，Pixel/API36模拟器full均FAIL：native、11宿主异常/同步切字段、跨120分页PASS，原R04敏感预存组合音标仍4/5。没有替换失败门槛或将expected failure改成整体PASS。
- 同debug哈希，两设备controller合成canary、settings（清学习仅点取消）、resources、R08、R09、R10、预准备无学习profile重启分项PASS。R08词Top1 89/100、Top5 99/100，句Top5 19/20；R09十词2→1/restart1/clear2；R10上下文改善9/10、off无加载/查询。
- 同optimized release哈希，两设备engine/resources/R08/R09/R10 PASS；完整release宿主/系统IME/视觉门禁未运行，不能继承debug结果。
- 两设备自己debug UID的合成canary日志前后0命中；只扫描该UID main/system buffer，未保存原始日志，不覆盖system UID/其他宿主或全部SEC04。
- 真正20项原生候选分页、禁用键绘制/命中分离、controller异步屏障见[CANDIDATE_PAGING](CANDIDATE_PAGING-20261023.md)。单键6仅4候选导致的首测前提FAIL保留，完整音节64仍验证global120确切选择。
- 旧94b9d40f模型开/关各30分钟JNI观测见[NATIVE_PERF](NATIVE_PERF-20261023.md)，属于历史APK与组件，不是上述新APK的完整P01—P04成绩。
- 历史2ad源码用新下载SDK/JDK、初始空Gradle cache重建，TLS失败后原规则重试整包bbd28866一致；固定公共table/model输入仍共享。见[TOOLCHAIN](TOOLCHAIN-20261023.md)，不冒称当前release全新OS镜像/hermetic或M6通过。

[26条最终记录汇总](evidence/m1/m2-final-regression-summary.json)的整体pass=false、release_gate=NOT_ACCEPTED，各原始JSON/哈希可追查。静态审计、unsigned对照与DEX旧前端裁剪记录同在evidence/m1/m2-final-*。

## 具体阻碍与未交付

1. 用户尚未在系统设置启用/切换本输入法。未擅自切换默认键盘；系统IME生命周期、EditText/Compose/WebView多宿主、旋转/导航inset、完整P01/P02/P04仍待该条件。
2. iPhone13原截图/录屏、iOS版本/键盘设置及体验确认缺失，M5视觉BLOCKED。自有View.draw合成键盘图片不算参考对照、系统截图或1:1。
3. R04敏感未知预存组合音标删除FAIL；不得为过关读取密码/私密上下文。保留保守宿主DEL及全部失败记录，需继续解决宿主兼容边界。
4. 敏感模式现仅直接英/数；预准备no-learning profile重启PASS不等于完整隐私中文/全新安装首个敏感字段/服务全路径零写。
5. 目标OS备份/设备迁移、完整SEC01—05、正式签名、全部词库二次来源/模型对应源分发审查及最终同一release全门禁未关闭。无真实用户数据操作、系统备份/恢复或刷机。

本检查点只供本地审查和继续开发，不能称应用完成、可日用、安全全通过或像素一致。

执行备注：首次静态audit命令手写APK basename错误，只产生缺文件报错、没有审计结果；后按各output-metadata.json选取实际文件名重跑，最终三份audit均绑定上表哈希。原错误输出保留在忽略artifacts，不计作Android断言失败或通过。
