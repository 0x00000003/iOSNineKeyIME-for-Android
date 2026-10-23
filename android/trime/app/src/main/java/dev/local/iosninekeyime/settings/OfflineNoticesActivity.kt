// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.settings

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import android.view.ViewGroup
import com.osfans.trime.BuildConfig
import com.osfans.trime.R
import org.json.JSONObject
import java.security.MessageDigest

/** Plain offline text only: no WebView, URL launch, remote loader or external-file argument. */
class OfflineNoticesActivity : Activity() {
    var sourceNoticeDialog:AlertDialog?=null
        private set
    var noticeCount=0
        private set
    var dependencyCount=0
        private set
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val raw=assets.open("offline-notices.json").use {it.readBytes()}
        val sha=MessageDigest.getInstance("SHA-256").digest(raw).joinToString(""){"%02x".format(it)}
        if(sha!=BuildConfig.OFFLINE_NOTICES_SHA256){setContentView(TextView(this).apply {text="许可资料校验失败"});return}
        val entries=JSONObject(String(raw,Charsets.UTF_8)).getJSONArray("entries")
        val libraries=JSONObject(resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use {it.readText()}).getJSONArray("libraries")
        val titles=mutableListOf<String>();val bodies=mutableListOf<String>()
        for(i in 0 until entries.length()) {
            val row=entries.getJSONObject(i)
            titles+=row.getString("title")
            bodies+=row.getString("source_path")+"\nSHA256 "+row.getString("sha256")+"\n\n"+row.getString("text")
        }
        noticeCount=entries.length();dependencyCount=libraries.length()
        val runtime=org.json.JSONArray(assets.open("runtime-artifacts-release.json").bufferedReader().use {it.readText()})
        dependencyCount=runtime.length()
        for(i in 0 until runtime.length()) {
            val row=runtime.getJSONObject(i)
            titles+=row.getString("component")
            bodies+=row.getString("component")+"\n制品："+row.getString("file")+"\nSHA256："+row.getString("sha256")+
                "\n精确版本POM许可："+row.getJSONArray("pom_licenses").toString(2)+"\nPOM SHA256："+row.optString("pom_sha256")+"\n\n"+row.getString("scope")
        }
        for(i in 0 until libraries.length()) {
            val row=libraries.getJSONObject(i)
            if(row.optString("tag") !in setOf("native","data","model"))continue
            titles+=row.optString("name",row.getString("uniqueId"))+" "+row.optString("artifactVersion")
            bodies+=row.getString("uniqueId")+"\n版本："+row.optString("artifactVersion")+"\n许可："+row.optJSONArray("licenses").toString()+"\n来源："+row.optString("website")+"\n\n"+row.optString("description")
            dependencyCount++
        }
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(16,24,16,16)}
        root.addView(TextView(this).apply {text="离线来源与许可\n固定源码许可原文 $noticeCount 项，解析依赖 $dependencyCount 项。含源码/构建依赖，不代表完整运行时分发审计已通过。模型为Rime compact LGPL数据；万象LTS模型与未授权数据仓库工具未打包。";textSize=16f})
        val list=ListView(this).apply {
            tag="offline-notice-list"
            adapter=ArrayAdapter(this@OfflineNoticesActivity,android.R.layout.simple_list_item_1,titles)
            setOnItemClickListener {_,_,position,_->
                val text=TextView(this@OfflineNoticesActivity).apply {tag="offline-notice-body";text=bodies[position];setPadding(24,16,24,16);textSize=14f;setTextIsSelectable(true)}
                val scroll=ScrollView(this@OfflineNoticesActivity).apply {addView(text)}
                sourceNoticeDialog?.dismiss()
                sourceNoticeDialog=AlertDialog.Builder(this@OfflineNoticesActivity).setTitle(titles[position]).setView(scroll).setPositiveButton("返回",null).show()
            }
        }
        root.addView(list,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        root.addView(Button(this).apply {text="返回";setOnClickListener {finish()}})
        setContentView(root)
    }
    override fun onDestroy(){sourceNoticeDialog?.dismiss();sourceNoticeDialog=null;super.onDestroy()}
}
