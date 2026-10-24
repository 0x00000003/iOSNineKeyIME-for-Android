# 系统敏感IPC与删除队列协议 v1（预冻结）

继bd7ed9a的系统core10/10、敏感字素36/36之后，新增privacy-queue scope。此协议在新scope运行前冻结；不更改原36例预期。独立宿主仍通过实际系统IME触控，InputConnectionWrapper只拒绝正文读取、计结构请求，并可延迟真实TextView提供的结构查询结果；不提供字符坐标、不伪造cursor anchor或手势结果。

固定20例：五类password/visible-password/web-password/number-password/no-learn各两例（组合音标、ZWJ家庭）完整删除10例；五类型选区删除5例；password/no-learn各一例在真实结构结果延迟300ms期间按删除、a、b，最终必须Aab且无正文getter调用2例；email普通删除后a、b精确结果1例；password持续按删除，八个ASCII最终全删、松手停止1例；password结构查询未完成时切到新敏感字段并输入c，旧回调不得修改新字段1例。全部仅合成fixture，结果只布尔/计数/案例ID，不输出正文/候选/字符坐标。

每例敏感包装器拒绝并计数before/after/selected/extracted/surrounding/caps等正文API，结构查询/手势分别计数。纯选区允许commitText空字串，不要求结构查询；普通email控制允许读取正文，不能归为敏感零读。300ms是宿主故障夹具延迟，不是生产IME延迟或扩大删除超时。队列例必须观察已开始且未交付的真实结构结果，否则FAIL不算覆盖。继续核实际IME/host/test三SHA、nonce、独立UID；系统隐私持久层/日志/多行复杂宿主另行测试。
