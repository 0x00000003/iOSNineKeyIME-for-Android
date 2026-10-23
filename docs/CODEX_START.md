# 开发入口

本项目开发约定见[AGENTS](../AGENTS.md)。从[当前状态](STATUS.md)和[开发手册](DEVELOPMENT.md)开始，再阅读[计划](PLAN.md)及对应模块的规格与测试协议。

基本检查：

```sh
git status --short
python3 scripts/check_project.py
python3 scripts/doctor.py
python3 -m unittest discover -s tests/tooling -v
```

实现变更后，运行相关 Android 单元、组件或系统宿主测试，记录实际制品哈希和验证范围。保留历史失败，更新状态、决策与测试索引，再提交经审查的文件。公开问题报告与截图只使用合成内容。
