package dev.harrydekat.discipulus

import android.content.SharedPreferences
import org.json.JSONObject

class MessagesWidgetProvider : SnapshotWidgetProvider() {
    override val snapshotKey = "messages"
    override val title = "Berichten"
    override val route = "messages"

    override fun subtitle(widgetData: SharedPreferences): String {
        val unread = widgetData.getString("messages_unread", null)?.toIntOrNull() ?: 0
        return if (unread > 0) "$unread ongelezen" else "Laatste berichten"
    }

    override fun formatLine(item: JSONObject): String {
        val sender = item.optString("sender").ifBlank { "Bericht" }
        val subject = item.optString("subject").ifBlank { "Zonder onderwerp" }
        return "${if (!item.optBoolean("read", true)) "• " else ""}$sender · $subject"
    }
}
