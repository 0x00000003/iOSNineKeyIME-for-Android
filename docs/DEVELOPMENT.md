# 开发手册

## 初始化环境观察
2026-10-23 已观察到 Python3、Git、Java、adb 可执行路径，用户 SDK 默认目录存在；没有全局 gradle/cmake/ninja 在 PATH，ANDROID_HOME/ANDROID_SDK_ROOT 未设置。Java 路径名提示 JDK21，但实际版本以 doctor 结果为准。**目录存在不证明所需 SDK/NDK/CMake 已安装**。本阶段未联网安装工具链、未安装APK、未启用/切换输入法、未读取真机输入或用户词库。

固定 Trime 的 Gradle/AGP/Kotlin 见 UPSTREAM，不按当前主分支或旧 README 直接选择。JDK 运行版本、Java 编译目标、Kotlin 目标是不同概念。M1 读取构建 convention 和 Gradle 官方兼容表，验证 `./gradlew --version` 后锁定，记录实际 SDK/NDK/CMake 组件清单。可按需安装到明确目录，不更改全局环境/其他项目。

## 初次工作
1. 读 AGENTS 和 STATUS，`git status --short`；先保留现有修改，禁止 reset --hard、clean -fd 等清场。
2. 运行文档检查、doctor 和 tooling 单测，建立本轮测试记录。
3. 按锁定 SHA 取源码和资源到项目 `.cache/upstream/`。下载仅允许 HTTPS 的上游官方源；使用 archive 时校验路径穿越/符号链接和哈希，不执行 curl-pipe-shell。Git 临时 checkout 的 `.git` 不导入 android/trime。
4. 递归固定子模块，补齐来源/commit/许可并生成源码清单，检查 ZIP 的 `__include` 和词库入口，不下载未知打包APK作为本项目制品。
5. 先构建可复现基线，建立自己的 applicationId、安全配置和资源部署，再运行 M1 探针。

## 构建命令约定（M1 导入后才可用）
在 `android/trime/` 内使用上游 Wrapper。先 `./gradlew tasks --all` 确认真正任务名，然后至少运行：

```sh
./gradlew --version
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease :app:lintRelease
```

这些命令是未来执行清单，**当前无 Gradle 工程，未运行、未通过**。上游 `checkReleaseBuilds=false` 不得当作 lint 通过，release lint 必须显式执行。native ABI 配置及 arm64-only 选项从实际 convention 读取，不能虚构一个 Gradle property。使用项目内 `GRADLE_USER_HOME`、独立测试输出目录，依赖锁/verification 和 Wrapper checksum 纳入Git；SDK 路径放忽略的 local.properties，密钥只在项目外安全位置，签名配置从明确的本地环境读取且日志不输出值。

## 设备流程
用 `adb devices` 选择用户授权的测试设备，不记录序列号到项目；多设备一律指定 `-s`。只读取测试需要的系统版本/ABI/显示参数，不扫描相册、短信、账号、剪贴板或其他应用。debug 测试 APK 可在选定测试设备进行常规安装，不替换他人包；首次启用由设备操作者确认Android系统警告；测试仅在已启用的本项目debug/release之间切换。仅在测试应用输入合成文本，保留其他输入法以便恢复。无设备：先执行 emulator/host 测试并记录真实阻碍；不可标注真机PASS。

## 文档与提交
每轮记录目标、修改文件、命令/环境、状态、证据和下一步；测试记录区分 PASS/FAIL/BLOCKED/NOT_RUN，不以退出码0概括未覆盖测试。测试记录的 tested_commit 和最终 docs commit 可不同；记录测试时工作区补丁或本地基线，并对 APK 保存哈希。不要试图把“当前提交自己的哈希”写在该提交内形成循环；初始化commit ID见Git/final，后续日志在下一提交引用前一哈希。

每个里程碑本地提交；先 `git diff --check`、审查 `git diff` 和 staged paths，扫描敏感文件，再暂存已核验的任务路径。首次新仓库可在完整目录审查后整体暂存，后续用明确路径；不得 `git add` 父目录。公共分支遵循仓库贡献流程。重要测试缺失只能提交 WIP，PROGRESS 写明不能验收。

## 排错和恢复
部署失败保存错误码/资源版本，保留原已验证包并清理本次 staging（只限本项目临时数据）；不能删用户词库来“修复”每次失败。升级学习库先验证 schema 迁移，在私有 noBackup 中事务转换；失败回滚或提供明确手动重置，避免默认导出敏感词库。崩溃/ANR 只用合成场景复现，日志脱敏后入记录。包名变化需检查 providers/资源路径；密集输入错乱先看 session/revision 和唯一 commit 口，而不是增加延迟掩盖竞态。

## 本机组件差距（doctor实读）
当前SDK platforms只有android-34和android-36.1；build-tools为30.0.3/36.1.0/37.0.0；NDK为23.1.7779620、CMake为3.22.1、未见cmdline-tools子目录。固定上游要求compileSdk36/build-tools36.0.0、默认NDK28.0.13004108/CMake3.31.6，不能把存在其他版本当作依赖齐全。JDK实测21.0.7。M1先解决并记录确切差距，不在初始化阶段伪造构建成功。

## M1 本地检查点的实际命令与范围

工程已导入。使用 `./scripts/gradle.sh` 保持SDK/Gradle/Android用户配置在项目`.cache`内；不要再按上面的M0环境观察理解当前状态。已实跑基线、debug、androidTest、unsigned release、35项JVM测试与debug/release lint；对应结果见testing/M1-20261023.md。

当前应用是兼容性探针，没有注册可日用IME。发布签名未配置，不读取外部密钥环境；debug密钥仅在本项目忽略的`.private`。构建APK前依次准备锁定lite ZIP、运行prepare_probe.py（需新staging目录）、package_android_probe.py，再运行Gradle。package脚本同时写资源manifest；临时输出不能替代原始资源哈希。工具链干净环境重建仍未验收。

上游导入文件保留原字节。首次整体`git diff --cached --check`报告上游既有空白问题；不批量格式化上游。原始4928文件逐一对比清单，19项本地修改另有哈希ledger，并对新增文件和修改相对原文件的差异单独检查空白。证据日志副本仅脱敏和去掉行尾空白，不改变测试结果；原始输出保留在忽略的artifacts。

## probe-3公共预编译词典（开发检查点）

prepare_prebuilt_tables.py仅允许6个固定公共table/reverse，要求.cache/host-compat-profile-1/build与profile-2/build字节一致；输出third_party/prebuilt-tables.json和忽略的.cache/probe-prebuilt。绝不递归复制整个profile或userdb。然后在新的.cache/android-probe-resources staging运行`python3 scripts/package_android_probe.py --prebuilt`；当前脚本拒绝覆盖已存在stage，保留旧stage后才重建，勿用全局清理。原始source词典在已核验lite ZIP/host准备目录，runtime只用prebuilt fallback，不能删构建来源或许可。

两个fresh staging重建的ZIP与manifest字节一致；当前APK13e97020对应资源daec81fe。尚无完整一键干净SDK/JDK重建证明，不能以同缓存重建称M6 REL01。设备测试命令`python3 scripts/run_probe.py --target pixel --scope engine --record 新名称`；editor/full会保留私密组合音标FAIL，不能为得到退出0静默跳过。Pixel目标只能来自忽略的.private/test-device；不打印serial、不启用/切换IME。专用emulator-5580以同脚本显式选择，始终检查qemu。原始输出和APK留artifacts，脱敏记录留docs/testing/evidence。

## 当前 WIP 构建补充（优先于历史M0/probe段落）

已有薄IME服务，仍未由用户启用，实际功能范围及制品以STATUS首段和对应哈希记录为准。Gradle preBuild现在自动生成并核验五项固定运行时资产（包括NOTICE），不再要求手工保留build目录。默认release unsigned；显式localReleaseProbe才用本项目调试证书签优化非debuggable测试制品。禁止把该证书当正式发布。

实际项目与included convention build使用STRICT依赖锁及严格verification；新增配置无lock state、意外依赖均失败关闭。常规构建不要加--write-locks绕过失败，应先定位、核来源/许可与差异，再审查锁的有意更新。未用UTP配置没有为报告任务额外白名单。

独立工具链命令：`python3 scripts/bootstrap_clean_toolchain.py --destination .cache/rebuild-new --source-commit 2ad1de3 --fetch-archives`。目标必须新建，使用固定HTTPS归档/哈希，约需12GiB以上余量；沿用已接受同条款SDK marker与公共table/model输入，创建新SDK/JDK/Gradle cache，不调用adb。脚本输出目标内bootstrap-receipt.json及gradle-build.txt，网络失败保留原状态，不改TLS/哈希或自动宣布成功。实际历史2ad源码首次TLS失败、同规则重试整包复现条件见TOOLCHAIN记录；这不替代当前版本同哈希回归或完整M6。

## 独立系统宿主（2026-10-23）

`:testhost`复用现有工具链，独立UID、debug-only，无native/Rime依赖。先构建`./scripts/gradle.sh :testhost:assembleDebug :testhost:assembleDebugAndroidTest :testhost:lintDebug`，再运行`python3 scripts/run_system_host.py --target pixel --scope core --record 新的英文小写记录名`；scope还含readiness/private-delete/privacy-queue，精确定义见SYSTEM_HOST与SYSTEM_PRIVATE_QUEUE_PROTOCOL。runner核实际三APK/hash/nonce，只在明确用户授权下选已经启用的本项目测试条目；未启用标BLOCKED，不调用ime enable。

主应用debug组件测试与系统IME同UID时可能争用native租约，组件运行前仅选已启用的本项目优化版，随后系统runner可切回debug；不改第三方输入法。`-PlocalReleaseProbe=true`把testBuildType切到release，所以此配置没有assembleDebugAndroidTest任务；debug及默认unsigned构建和显式本地签名release需分开命令运行，无需复制工具链或新隔离目录。本轮误把两类任务合并的配置失败已纠正，不算Android测试失败或成功。

主机参考测量使用Pillow11.3.0，非APK/运行时依赖；`requirements-visual.lock`从该固定版本的官方PyPI元数据记录wheel SHA，禁止sdist动态构建。当前复用本机已有同版本库，未声称已核验其原始安装wheel。独立重建测量环境时可只在项目内执行 `python3 -m pip install --require-hashes --no-deps --target .cache/python-visual -r requirements-visual.lock`，再以 `PYTHONPATH="$PWD/.cache/python-visual" python3 -m unittest discover -s tests/tooling -v` 验证；不全局安装或复制Android工具链。测量输出不是Android/视觉验收PASS，输入图片始终私有只读。
