# 共创与贡献

本项目由维护者与GPT（通过OpenAI Codex）协作开发。维护者负责产品方向、交互参考、反馈和发布决策；GPT/Codex协助代码实现、排错、测试执行及文档。第三方代码和数据的原作者归属保持不变。

## 提交署名

提交作者使用实际贡献者自己的Git身份。对于确有GPT/Codex参与的提交，在提交消息正文之后空一行，添加：

```text
Co-authored-by: Codex <codex@openai.com>
```

GitHub将该邮箱关联到Codex共同作者账号。维护者保留本人的GitHub关联作者身份，可使用GitHub提供的noreply邮箱避免公开私人邮箱。

GitHub的多作者格式与账号邮箱关联要求见[官方文档](https://docs.github.com/en/pull-requests/how-tos/commit-changes/creating-a-commit-with-multiple-authors)。未使用AI的提交不自动添加AI署名；不要冒用其他个人、机器人或公司身份。

## 开发与验证

从[开发约定](../AGENTS.md)、[当前状态](STATUS.md)和[开发手册](DEVELOPMENT.md)开始。变更提交相应测试与脱敏证据，保留真实失败，说明已覆盖与未覆盖范围。AI协助、测试通过和正式审核是不同状态，发布仍以具体制品和记录为准。

问题报告和测试只使用合成内容，不包含私人聊天、密码、用户词库或设备标识。图片、依赖与第三方资源注明来源和允许用途。
