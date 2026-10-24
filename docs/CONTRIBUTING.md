# 贡献与共同作者

## GitHub双头像署名

Codex实际参与的提交使用维护者本人的GitHub关联邮箱作为Author，并在提交消息末尾空一行，添加：

```text
Co-authored-by: Codex <codex@openai.com>
```

GitHub将该邮箱关联到[codex账号](https://github.com/codex)，以共同作者形式显示Codex头像。维护者使用自己的GitHub noreply邮箱，避免公开私人邮箱；不把维护者作者身份替换为Codex。

这一格式也见于[OpenAI官方提交约定](https://github.com/openai/symphony/blob/main/.codex/skills/commit/SKILL.md)，[官方仓库实际双作者示例](https://github.com/openai/swarm/commit/6af0b4caf37dca4526dfd98e9fbd8ce36e7eeb22)已显示维护者与Codex。核验范围见[署名核验记录](testing/codex-attribution.json)。GitHub多作者机制见[官方文档](https://docs.github.com/en/pull-requests/how-tos/commit-changes/creating-a-commit-with-multiple-authors)。

不要给未使用Codex的贡献添加该署名，也不要覆盖其他贡献者或全局Git配置。共同作者归属不代表OpenAI官方审核本项目。仓库未推送时，本地只具备提交元数据，尚不存在本项目的GitHub在线展示页面。

## 开发与验证

从[开发约定](../AGENTS.md)、[当前状态](STATUS.md)和[开发手册](DEVELOPMENT.md)开始。提交相应测试与脱敏证据，保留真实失败，说明已覆盖与未覆盖范围。发布以具体制品和验证记录为准。

问题报告和测试只使用合成内容，不包含私人聊天、密码、用户词库或设备标识。图片、依赖与第三方资源注明来源和允许用途。
