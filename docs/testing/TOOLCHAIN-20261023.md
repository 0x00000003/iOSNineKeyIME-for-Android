# SDK/JDK与依赖锁独立复核（WIP）

先将项目自己生成的233个APK里188份同hash重复制品改hardlink，保留所有文件路径/字节/原测试记录，释放约9.3GiB到24GiB余量。只操作忽略artifacts，不清理外部目录；m6-own-apk-storage-dedup.json。

固定官方SDK/JDK归档全新HTTPS下载，匹配此前记录publisher SHA1或SHA256后追加SHA256锁third_party/toolchain-payloads.json，未用nightly/动态latest解析；commandlinetools文件名含_latest但固定numeric build13114758/19.0及SHA256。SDK每个普通文件和23个内部symlink与归档核对，symlink最后创建并拒绝越界；JDK459个普通文件解包后另与tar逐文件核对。仅沿用同固定条款既有SDK接受marker、固定公开package.xml元数据及公共词库/table/model输入，SDK/JDK二进制和Gradle cache不复用旧安装。无签名key、无adb调用。

从2ad1de3历史源码git archive，新建SDK/JDK/.gradle/build/.cxx，Gradle cache初始为空。首次构建8m5s因一个Maven Central AAR TLS握手中断FAIL（m6-fresh-sdk-first-build.json），不降TLS或放宽verification；原固定规则重试5m13s成功。整包62444813 bytes / unsigned bbd28866af7501b99593c28e58f92976449f4ee8ef7be7da009abbbb2b16081a 与基线完全一致，m6-clean-sdk-rebuild.json。javaToolchains实际仅检测新目录Temurin21.0.7+6，auto-detection=Disabled。此历史源码仍含后续决定移除的图标字体，不是当前无字体政策或完整M6应用验收；SDK/依赖首次获取与整包一致的具体条件已经记录，不推为新OS镜像的完全hermetic重建。

依赖锁复核发现旧lockAllConfigurations默认LockMode.DEFAULT，对某些发布/测试配置无锁状态不报错；严格dependency verification始终开启，曾遗漏部分lock状态不是等于无hash。一次全量:dependencies报告触及未用UTP配置被严格verification拒绝，记录artifacts/m4-no-font-default-locks-state.txt，没有添加其白名单。改为只运行实际debug/release/测试/lint任务--write-locks补实际使用配置，并在root/包含convention build的settings早期激活锁并设LockMode.STRICT。不能把先前三个debug配置的锁说成完整variants锁。

删除未用字体依赖后实际releaseRuntimeClasspath 124→122项（含state），去掉community-material-typeface及compose runtime-android；其传递约束不再提升iconics-typeface-api5.5.0-b01→当前5.4.0和kotlin-stdlib-jdk7/8 2.0.0→1.8.0，其余既有运行时版本不变。所有仍在严格verification中，需实际无字体APK/完整回归及最终清单绑定，不能静默接受其他版本漂移。

严格模式无--write-locks实际默认debug/test/release/unit/lint构建成功，优化本地release+测试APK构建也成功。负测初版init脚本误向included build查找:app而失败，未当锁门禁成绩；修正findProject guard后，两次独立Gradle实际解析均按预期拒绝：新配置没有lock state、重新加入已删除字体依赖不在lock state。见[m4-strict-lock-negative.json](evidence/m1/m4-strict-lock-negative.json)。没有生成/安装负测APK，没有放宽verification或写新锁来消除错误。锁覆盖实际使用配置；未使用UTP等配置仍会失败关闭，不声称所有可能配置都已可构建。

当前debug338bb5be实际APK 73132301字节，五项资产及权限/备份XML审计PASS，packaged_fonts空数组；每个release变体还须独立核验。删除字体前的历史审计仍按当时规则保留。

负测fixture已纳入tests/fixtures/strict-lock-negative.init.gradle，并按此Git文件再次实跑两条任务，evidence保存fixture/log SHA：`./scripts/gradle.sh --init-script "$PWD/tests/fixtures/strict-lock-negative.init.gradle" :app:codexMissingLockProbe` 及 `:app:codexUnexpectedFontProbe`（第二条同一init参数）。都必须非零退出且分别匹配缺lock state/不在lock state，不能把一般Gradle报错当预期拒绝。

最终debug dd8f688f、unsigned9cdd4df2、本地签名dedeee3c实际APK均零字体、权限/备份/锁定资源静态PASS；122项运行时清单已由实际APK资产回写third_party/runtime-inventory-release.json。完整分项回归见CHECKPOINT记录，不包含系统IME全门禁。复用bootstrap工具增加了逐文件JDK post-check与每个新目标独立日志；原始首次环境构建和手工复核记录不改写为新版脚本全流程重跑。

96d627a代码检查点再做独立完整获取路径：新源码/SDK/JDK/初始空Gradle cache首次构建62/62任务成功，12m34s。unsigned整包61720617字节/SHA9cdd4df28cd25e1fb19ebd2438f4078d99c1890514b5dda0ac1bdb97c3bff0c1，与冻结基线完全一致；同源auditor实际静态零字体/权限/备份/资源PASS。[完整收据](evidence/m1/m6-source-96d-clean-toolchain.json)、[实际APK审计](evidence/m1/m6-source-96d-audit/audit.json)。SDK/JDK新安装并逐文件核归档、JDK459文件，现有SDK同条款接受marker及固定公开table/model仍复用。此包43正文，不能将它归给后续新增NDK notice的44正文补丁或完整M6签收。
