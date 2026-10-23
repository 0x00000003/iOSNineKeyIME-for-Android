// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class ProbeActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24) }
        val status = TextView(this).apply {
            text = "M1 兼容性探针\n\n仅使用内置合成语料验证 Rime/JNI 与离线部署。尚非可日用键盘，未验收视觉、隐私模式或模型上下文。"
            textSize = 18f
        }
        val run = Button(this).apply { text = "运行合成探针" }
        run.setOnClickListener {
            run.isEnabled = false
            status.text = "正在私有 noBackup 目录部署并测试，请稍候……"
            worker.execute {
                val passed = try { ProbeRunner.run(applicationContext).getBoolean("pass") } catch (_: Exception) { false }
                runOnUiThread {
                    if (!isDestroyed) {
                        status.text = if (passed) "合成 JNI 探针通过。\n完整输入法、模型与视觉验收仍未完成。" else "探针失败（M1），请检查脱敏测试记录。"
                        run.isEnabled = true
                    }
                }
            }
        }
        panel.addView(status)
        panel.addView(run)
        setContentView(panel)
    }
    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }
}
