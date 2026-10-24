// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.iosninekeyime.ui

/** Authored Unicode shortcuts/search labels, rendered by the Android system font.
 * Fixed defaults only: no usage history, input-context reads, downloaded artwork or persistence.
 */
internal object EmojiCatalog {
    data class Item(val text:String,val words:String)
    val categoryNames=listOf("常用（固定列表）","笑脸与情绪","动物与自然","食物与饮料","活动","旅行与地点","物品","符号","旗帜","人物与手势","文字表情")
    private fun items(text:String,words:String)=text.split(" ").map {Item(it,words)}
    private val faces=items("😀 😁 😂 🤣 😊 😎 😍 🥰 😘 😴 🤔 😮 😢 😭 😡 😇 🤩 😅 😉 😌 😜 🥳 🤗 😐 😋 😬 🙃 🫠","face smile happy laugh sad cry emotion")
    private val animals=items("🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🦆 🦉 🦋 🐝 🐢 🐬 🌸 🌻 🌲 🍀","animal dog cat bird nature flower")
    private val food=items("🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍒 🍑 🥭 🍍 🥑 🍅 🥕 🌽 🍞 🧀 🍔 🍟 🍕 🍜 🍚 🥟 🍰 ☕ 🧋","food fruit apple pizza drink coffee tea")
    private val activities=items("⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥊 🏊 🚴 🏃 🧘 🎯 🎮 🎲 🎨 🎼 🎹 🎸 🎤 🎬 🏆 🥇 🎁 🎉","sport activity ball game music party")
    private val travel=items("🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚚 🚲 🛵 🚂 🚄 ✈️ 🚀 🛸 ⛵ 🚢 🏠 🏢 🏰 🗼 🗽 🌋 🏝️ 🌍 🌙","travel car bus train plane rocket space moon earth home")
    private val objects=items("⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 📷 📺 📻 💡 🔦 🕯️ 📚 📖 ✏️ 🖊️ 📝 📌 🔍 🔑 🔒 🛠️ ⚙️ 💰 💵 📦 💌","object computer phone screen tv book light key money gear")
    private val symbols=items("❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💕 💞 💓 💗 💖 💘 💝 ✨ ⭐ 🌟 💫 🔥 💯 ✅ ❌ ❓ ❗ ➕ ➖","symbol heart love star fire check yes no question")
    private val flags=items("🏳️ 🏴 🏁 🚩 🏳️‍🌈 🇨🇳 🇺🇸 🇬🇧 🇯🇵 🇰🇷 🇫🇷 🇩🇪 🇮🇹 🇪🇸 🇨🇦 🇦🇺 🇧🇷 🇮🇳 🇸🇬 🇲🇾 🇳🇿 🇿🇦 🇲🇽 🇦🇷 🇵🇹 🇳🇱 🇸🇪 🇨🇭","flag country")
    private val people=items("👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🙌 👏 🤝 🙏 💪 🧑‍💻","people hand person hello good thumb clap work")
    private val textFaces=listOf("(^_^)","(≧▽≦)","(T_T)","(>_<)","(•‿•)","(¬_¬)","(⌒▽⌒)","(￣▽￣)","(・ω・)","(o_o)","(＾▽＾)","( ´ ▽ ` )").map {Item(it,"text face kaomoji happy sad")}
    private val common=(faces.take(14)+symbols.take(7)+people.takeLast(7))
    private val groups=listOf(common,faces,animals,food,activities,travel,objects,symbols,flags,people,textFaces)
    fun select(category:Int,query:String):List<String> {
        val needle=query.trim().lowercase().take(32)
        val source=if(needle.isEmpty())groups.getOrElse(category){common}else groups.flatten().distinctBy {it.text}.filter {needle in it.words || needle==it.text}
        return source.map {it.text}
    }
}
