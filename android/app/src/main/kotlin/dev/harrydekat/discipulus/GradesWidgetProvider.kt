package dev.harrydekat.discipulus

import android.content.SharedPreferences
import org.json.JSONObject

class GradesWidgetProvider : SnapshotWidgetProvider() {
    override val snapshotKey = "grades"
    override val title = "Cijfers"
    override val route = "grades"

    override fun subtitle(widgetData: SharedPreferences): String =
        widgetData.getString("grades_average", null)
            ?.takeIf { it.isNotBlank() }
            ?.let { "Gemiddelde $it" }
            ?: "Laatste resultaten"

    override fun formatLine(item: JSONObject): String {
        val subject = item.optString("subject").ifBlank { "Vak" }
        val grade = item.optString("grade").ifBlank { "-" }
        return "$subject · $grade"
    }
}
