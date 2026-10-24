# 开源合规与资源来源

## 已核对与未核对边界
核对依据是本次固定commit的实际LICENSE、文件SPDX、LICENSE-SCOPE与GitHub元数据，见 [UPSTREAM](UPSTREAM.md) 和 [NOTICE](../NOTICE.md)。当前只研究，没有形成APK/完整SBOM，不能声明所有传递依赖已审计。

| 类别 | 许可判断与已看证据 | 集成动作/阻碍 |
| --- | --- | --- |
| Trime前端/派生代码 | 根GPLv3正文，manifest/build等SPDX为GPL-3.0-or-later；GitHub检测GPL-3.0较粗略 | 派生原创程序用GPL-3.0-or-later；保留版权/SPDX、声明修改，分发提供对应源码与构建材料 |
| librime | 固定1.17.0 LICENSE BSD-3-Clause | 保留版权、条件、免责声明及二进制归属；子依赖另审 |
| 万象schema/词库/Lua | 固定v18.1.7根CC-BY-4.0；尚未逐文件审计完毕 | 分开记录代码/数据，不擅自统一改GPL；归属amzxyz及贡献者、来源/许可链接、修改说明；审查每个文件例外与二次来源 |
| 万象数据仓库 | 根CC-BY-4.0只覆盖无另声明的原创词库/拼音数据/文档等；LICENSE-SCOPE明确原创程序代码未授予独立复制/修改/再分发/商业使用许可 | 不复制其Python/Kotlin/Shell/Updater/工具源码到产品；需用时另找清晰授权实现或取得许可；根许可不可覆盖例外 |
| 语法模型.gram | LTS资产元数据已见；制品未下载、单独归属与授权文件范围待核对 | 打包前锁asset/哈希，确认该二进制为许可数据资产且有可随包提供归属；不从README“开源”字样推导所有二进制授权 |
| librime-lua / octagram / predict / Lua依赖 | 固定Trime gitlink已记录，未完成许可原文审计 | M1读取每个固定commit LICENSE/NOTICE和传递依赖；代码可用性与法律授权分别判定 |
| OpenCC/Boost/LevelDB/yaml-cpp/snappy等 | 当前只知依赖图，未逐个核对实际版本授权 | 不将上游README列表示作完整合规证据；生成递归SBOM，保留license/notice |
| Android/JVM依赖 | 版本目录已保存，不代表所有AAR/JAR/字体图标已核对 | 对最终依赖树和被打包资源逐项查版权/许可，删未用SDK，禁止动态版本 |
| Apple/iPhone素材 | 原截图尚无像素可用 | 只作合法参考，不分发Apple字体/私有图标/品牌；图形自行绘制，避免对外声称Apple官方产品 |

## GPL和数据资源的分离
同一安装包存在GPL程序与独立数据并不使第三方数据自动改为GPL；反过来CC-BY根目录不能重新授权嵌入的第三方代码。若Lua作为程序与GPL派生组合，须评估兼容条件及其原始许可，而非按“它是配置”跳过。对边界不清的具体资源暂停其打包，继续不依赖它的工作，不因单个阻碍停掉所有开发。不得把许可待核对状态写成已获授权。

分发GPL派生APK时须有对应版本的完整源码、修改、可复建脚本和必要安装信息义务评估，不只是贴Trime链接。归属和许可证在设置“关于/开源许可”离线可读，不靠外网网页。

## 依赖/资源登记模板
每项新增资产：ID、用途、官方来源URL、版本/完整commit或asset id、SHA256、大小、下载时间、原作者/贡献者、许可证SPDX或完整文本、例外/第三方来源、修改内容、对应源码位置、运行时是否会访问网络、审批/待核对原因、验证记录。不得空填“开源”。最终生成SBOM与NOTICE，根upstream-lock的research条目不能代替实际runtime_assets。

## M6分发核对
检查APK对应源码/资源版本与NOTICE一致；完整源码和依赖获取方式可重建；所有字体/图标/词库/model许可齐全；没有未授权data仓库工具；没有签名私钥、个人输入或用户词库；清楚列出视觉与算法差异。公开项目名/包名可另定，当前iOSNineKeyIME为内部开发名，不表示Apple关联。

## 当前开发检查点增量

M1/M2已有实际APK；早期“没有APK/只研究”保留为初始化历史。当前模型为固定lotem compact LGPL数据、Lua仅自写marker，原Wanxiang LTS gram不打包。原生插件/嵌入Lua/字体授权及逐文件SHA增量见third_party和M1 probe-4记录。AboutLibraries自动JSON有136依赖和许可ID，不能替代逐项传递版权/对应源审计；完整M4/M6分发检查仍未通过，现不对外发布。薄服务/状态/视图修改继续GPL-3.0-or-later，来源补丁账更新；没有Apple素材。

M4补充：固定43份正文、额外三个确切版本MIT来源、124个releaseRuntimeClasspath制品与全部POM/显式parent链SHA已落third_party清单；离线界面138条来源。原始Maven POM发布者许可声明与独立审查分开。Lua仅自写内置marker，用户/共享脚本及IO/OS/package/debug不加载；资源hash/白名单拒绝未知文件和symlink。上述进展未关闭全部词库二次来源、模型数据对应源和最终分发义务，仍禁止宣称完整合规或发布。

最新检查点：运行时字体依赖已移除（122个确切制品，相关传递约束变化已记录），三种实际APK零打包字体。历史SDK/JDK新下载且初始空Gradle cache整包复现是限定重建证据，不能解除上述词库二次来源/模型对应源条件；当前仍开发版WIP，不作完整分发合规声明。完整哈希及测试范围见[CHECKPOINT](testing/CHECKPOINT-20261023.md)。
