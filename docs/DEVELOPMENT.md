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
用 `adb devices` 选择用户授权的测试设备，不记录序列号到项目；多设备一律指定 `-s`。只读取测试需要的系统版本/ABI/显示参数，不扫描相册、短信、账号、剪贴板或其他应用。debug 测试 APK 可在选定测试设备进行常规安装，不替换他人包；用户亲自按 Android 设置流程启用/选择键盘。仅在测试应用输入合成文本，保留其他输入法以便恢复。无设备：先执行 emulator/host 测试并记录真实阻碍；不可标注真机PASS。

## 文档与提交
每轮记录目标、修改文件、命令/环境、状态、证据和下一步；测试记录区分 PASS/FAIL/BLOCKED/NOT_RUN，不以退出码0概括未覆盖测试。测试记录的 tested_commit 和最终 docs commit 可不同；记录测试时工作区补丁或本地基线，并对 APK 保存哈希。不要试图把“当前提交自己的哈希”写在该提交内形成循环；初始化commit ID见Git/final，后续日志在下一提交引用前一哈希。

每个里程碑本地提交；先 `git diff --check`、审查 `git diff` 和 staged paths，扫描敏感文件，再暂存已核验的任务路径。首次新仓库可在完整目录审查后整体暂存，后续用明确路径；不得 `git add` 父目录。公共分支遵循仓库贡献流程。重要测试缺失只能提交 WIP，PROGRESS 写明不能验收。

## 排错和恢复
部署失败保存错误码/资源版本，保留原已验证包并清理本次 staging（只限本项目临时数据）；不能删用户词库来“修复”每次失败。升级学习库先验证 schema 迁移，在私有 noBackup 中事务转换；失败回滚或提供明确手动重置，避免默认导出敏感词库。崩溃/ANR 只用合成场景复现，日志脱敏后入记录。包名变化需检查 providers/资源路径；密集输入错乱先看 session/revision 和唯一 commit 口，而不是增加延迟掩盖竞态。

## 本机组件差距（doctor实读）
当前SDK platforms只有android-34和android-36.1；build-tools为30.0.3/36.1.0/37.0.0；NDK为23.1.7779620、CMake为3.22.1、未见cmdline-tools子目录。固定上游要求compileSdk36/build-tools36.0.0、默认NDK28.0.13004108/CMake3.31.6，不能把存在其他版本当作依赖齐全。JDK实测21.0.7。M1先解决并记录确切差距，不在初始化阶段伪造构建成功。
