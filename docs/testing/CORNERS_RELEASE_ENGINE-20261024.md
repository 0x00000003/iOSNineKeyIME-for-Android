# 同优化APK双设备引擎回归（2026-10-24，WIP）

c37aa83434d7629ad26eb879ed05952b282d15738266e1644b0449d202c3803a / release-test2fcb8fdd479e822d977a9b0c433c0f23848746a7fcb16d4f4a7cc0b6a62308a3，在授权Pixel与本项目API36 ARM64模拟器各运行engine/resources/quality/learning/context五scope，全部实际PASS。冻结APK树为硬链接，独立nonce、实际已安装两APK完整SHA匹配，未重复下载SDK/NDK/Gradle。

- R08：两设备100词Top1=89、Top5=99；20句Top1/Top5=19；无学习profile整树字节/mtime不变、无userdb、每fixture历史清空、无自动host commit等控制通过。
- R09：十个非首词各20次学习，10/10由2到1，restart保持1，清本合成profile回2；相邻sentinel不变、预留native时拒绝清除、每次提交精确一次。没有清用户日用学习库。
- R10：model/history四格十组，9/10有改善；off未加载/0 query且排名不变，on实际5110 queries，无学习和静态资源不变控制通过。
- resources：主动损坏本项目公共资源/模型的拒绝、隔离重建、既有profile保持等原断言通过；不是清除用户数据/目标OS备份迁移。

全部十条JSON及哈希入口为 evidence/m5/reference-ui-corners-release-engine-checkpoint.json，原始记录在evidence/m1/reference-ui-corners-optimized-{pixel,emulator}-{engine,resources,quality,learning,context}.json。这些是引擎组件，不是完整系统IME质量/上下文/全路径隐私或P01—P04。

已发现测试元数据缺陷：test2fcb的host_editor_component.reason仍硬编码历史“R04 remains FAIL in debug component tests”，其status实际为NOT_RUN，此五scope根本没有跑host/editor，不能把这段旧文字解释为新失败。当前debug5c46的full与实际系统触屏36/20另有独立PASS；历史JSON不改写，后续测试源码将删除这种跨版本状态文案。

元数据修复：release-test f7b2e4a739558c92a378907209024ba701eb70c88a89a5bd25934cef8bb5795a去掉跨版本R04结论，保留host/editor NOT_RUN。release-test构建/lint通过；同c37分别在Pixel与模拟器实际engine复测PASS，两记录明确新测试SHA和正确NOT_RUN说明。原2fcb十条记录保持不变。
