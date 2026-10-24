# 开发手册

## 工程与工具链

Android 构建根为 `android/trime/`，源码与固定上游来源一同纳入仓库。主应用为 `:app`，纯状态组件为 `:state`，独立合成系统宿主为 `:testhost`。

使用 Temurin 21.0.7+6。SDK、NDK、CMake 和归档哈希以 [工具链清单](../third_party/toolchain-inventory.json)及[工具链制品](../third_party/toolchain-payloads.json)为准。Gradle Wrapper、依赖版本锁及 verification metadata 已固定；不要通过放宽验证或动态版本修复依赖错误。

`scripts/gradle.sh` 将 SDK、Gradle 与 Android 配置定位到项目 `.cache`，并先核对固定 SDK 收据。环境准备与隔离重建工具为 `scripts/bootstrap_clean_toolchain.py`，使用前阅读其 `--help`；不要覆盖现有目录。历史重建条件见[工具链测试](testing/TOOLCHAIN-20261023.md)。

## 常用构建

在仓库根目录运行：

```sh
python3 scripts/check_project.py
python3 scripts/doctor.py
python3 -m unittest discover -s tests/tooling -v
./scripts/gradle.sh :state:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./scripts/gradle.sh :testhost:assembleDebug :testhost:assembleDebugAndroidTest :testhost:lintDebug
```

运行时资产由 `preBuild` 生成并逐项校验，不依赖保留的旧 `build` 目录。APK 在 `android/trime/app/build/outputs/apk/`；按 AGP 的 `output-metadata.json` 选择确切变体与 ABI，不猜测文件名。

默认 release 未签名。仅用于本地优化测试的命令：

```sh
./scripts/gradle.sh -PlocalReleaseProbe=true :app:assembleRelease :app:assembleReleaseAndroidTest :app:testReleaseUnitTest :app:lintRelease
```

该选项使用项目私有调试证书，保持优化且非 debuggable，不是正式发布签名。debug 与该 release 测试构建分开执行。签名密钥、`local.properties` 和环境配置保持 Git 忽略。

## 设备与测试

仅使用专用或经许可的测试设备；多设备命令明确指定 `adb -s`。首次安装后在 Android 系统设置启用本项目输入法，不绕过系统警告。不要读取真实密码、相册、账号、剪贴板或其他应用数据。

当前脚本的 `pixel` 目标从忽略的 `.private/test-device` 读取设备标识，并核对已支持的测试设备型号；该测试目标不是项目的产品型号限制。专用模拟器目标为 `emulator-5580`，脚本核对 qemu。扩展测试目标前应补齐明确的设备选择和环境验证。

组件与系统测试示例（每次使用新的记录名）：

```sh
python3 scripts/run_probe.py --target pixel --scope pinyin --record pinyin-device-run
python3 scripts/run_system_host.py --target pixel --variant release --scope reference-pixels --record reference-device-run
```

组件测试与同 UID 的活动 IME 可能争用原生引擎租约；组件测试前选择另一个已启用的本项目变体。系统 runner 仅选择已启用的本项目输入法，不执行启用操作。

测试仅输入合成内容。原始 APK、输出和截图在忽略的 `artifacts/`；脱敏 JSON 位于 `docs/testing/evidence/`，绑定主/测试 APK 哈希及 nonce。组件、真实系统 IME、首次离线、安全与性能分别记录，不互相替代。

## 变更与来源

提交前审查差异及暂存路径，运行受影响的测试。不要清除未提交源码、学习库、密钥或测试记录来排错。新增或修改的第三方文件更新 `third_party/trime-modifications.json`，新依赖同步许可证、锁和验证记录。

测试记录中的源码修订与文档提交可以不同；以源码哈希、APK SHA256、测试包和执行范围关联证据。历史修订索引见 `docs/testing/source-revisions.json`。

主机参考测量使用 `requirements-visual.lock` 中固定的 Pillow；该依赖不进入 APK。测量输入保持只读，输出几何与色块差异不自动产生视觉验收结论。
