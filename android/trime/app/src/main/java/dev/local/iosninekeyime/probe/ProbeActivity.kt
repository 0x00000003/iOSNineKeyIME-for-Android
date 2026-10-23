// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.probe

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import java.util.concurrent.Executors

class ProbeActivity : Activity() {
    var clearConfirmation:android.app.AlertDialog?=null
        private set
    private val worker = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24) }
        val status = TextView(this).apply {
            text = "离线九键 · 开发检查点\n\n仅使用内置合成语料验证 Rime/JNI 与离线部署。已有临时键盘服务与引擎组件测试，完整隐私、系统IME、视觉和发布验收仍未通过。"
            textSize = 18f
        }
        val run = Button(this).apply { text = "运行合成探针" }
        val repair = Button(this).apply { text = "重建内置资源（保留学习库）"; visibility = View.GONE }
        run.setOnClickListener {
            run.isEnabled = false
            repair.visibility = View.GONE
            status.text = "正在私有 noBackup 目录部署并测试，请稍候……"
            worker.execute {
                var code = "E_PROBE_EXECUTION"
                val passed = try { ProbeRunner.run(applicationContext).getBoolean("pass") } catch (error: Exception) { code = ProbeFailureCode.from(error); false }
                runOnUiThread {
                    if (!isDestroyed) {
                        status.text = if (passed) "合成 JNI 探针通过。\n完整输入法、模型与视觉验收仍未完成。" else if (ProbeFailureCode.repairable(code)) "内置资源校验失败（$code）。可重建固定资源；学习库不会被清空。" else "探针失败（$code），请检查脱敏测试记录。"
                        repair.visibility = if (!passed && ProbeFailureCode.repairable(code)) View.VISIBLE else View.GONE
                        run.isEnabled = true
                    }
                }
            }
        }
        repair.setOnClickListener {
            repair.isEnabled = false
            run.isEnabled = false
            worker.execute {
                var code: String? = null
                try { ProbeRunner.rebuildPublicResources(applicationContext) } catch (error: Exception) { code = ProbeFailureCode.from(error) }
                runOnUiThread {
                    if (!isDestroyed) {
                        status.text = if (code == null) "已从安装包重建固定资源，原受损目录已隔离，学习库保留。可重新运行探针。" else "资源重建失败（$code）。请保留测试记录。"
                        repair.isEnabled = true
                        repair.visibility = if (code == null) View.GONE else View.VISIBLE
                        run.isEnabled = true
                    }
                }
            }
        }
        panel.addView(status)
        panel.addView(run)
        panel.addView(repair)
        panel.addView(Button(this).apply {
            text="系统键盘设置（由你启用/切换）"
            setOnClickListener {startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))}
        })
        panel.addView(Button(this).apply {
            text="离线来源与许可"
            setOnClickListener {startActivity(android.content.Intent(this@ProbeActivity,dev.local.iosninekeyime.settings.OfflineNoticesActivity::class.java))}
        })
        panel.addView(Button(this).apply {
            tag="clear-learning"
            text="清空本应用的输入法学习"
            setOnClickListener {
                clearConfirmation?.dismiss()
                clearConfirmation=android.app.AlertDialog.Builder(this@ProbeActivity).setTitle("清空本地学习？")
                    .setMessage("仅删除本应用输入法的本地学习记录，不删除固定词典。此操作不可撤销，请先收起本键盘。")
                    .setNegativeButton("取消",null).setPositiveButton("清空学习") {_,_->
                        isEnabled=false
                        worker.execute {
                            var code:String?=null
                            try{dev.local.iosninekeyime.privacy.LearningStore.clearNormal(applicationContext)}catch(error:Exception){code=ProbeFailureCode.from(error)}
                            runOnUiThread {if(!isDestroyed){status.text=when(code){null->"本地学习已清空。固定词典保留。";"E_ENGINE_BUSY"->"请先收起本键盘，再重试清学习。";else->"清学习失败（$code），请保留测试记录。"};isEnabled=true}}
                        }
                    }.show()
            }
        })
        setContentView(panel)
    }
    override fun onDestroy() {
        clearConfirmation?.dismiss()
        clearConfirmation=null
        worker.shutdown()
        super.onDestroy()
    }
}
