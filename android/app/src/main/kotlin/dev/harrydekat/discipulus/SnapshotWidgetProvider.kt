package dev.harrydekat.discipulus

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetPlugin
import es.antonborri.home_widget.HomeWidgetProvider
import org.json.JSONArray
import org.json.JSONObject

abstract class SnapshotWidgetProvider : HomeWidgetProvider() {
    protected abstract val snapshotKey: String
    protected abstract val title: String
    protected abstract val route: String

    protected abstract fun subtitle(widgetData: SharedPreferences): String

    protected abstract fun formatLine(item: JSONObject): String

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId, widgetData)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateWidget(
            context,
            appWidgetManager,
            appWidgetId,
            HomeWidgetPlugin.getData(context)
        )
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        widgetData: SharedPreferences
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_snapshot)
        val colors = widgetColors(context, widgetData)
        val items = loadItems(widgetData)

        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_subtitle, subtitle(widgetData))
        views.setViewVisibility(
            R.id.widget_subtitle,
            if (subtitle(widgetData).isBlank()) View.GONE else View.VISIBLE
        )

        val lineIds = intArrayOf(
            R.id.snapshot_line_1,
            R.id.snapshot_line_2,
            R.id.snapshot_line_3
        )
        lineIds.forEachIndexed { index, viewId ->
            val item = items.getOrNull(index)
            views.setTextViewText(viewId, item?.let(::formatLine).orEmpty())
            views.setViewVisibility(viewId, if (item == null) View.GONE else View.VISIBLE)
            views.setTextColor(viewId, colors.primary)
        }

        views.setInt(R.id.widget_root, "setBackgroundColor", colors.background)
        views.setTextColor(R.id.widget_title, colors.primary)
        views.setTextColor(R.id.widget_subtitle, colors.secondary)
        views.setOnClickPendingIntent(R.id.widget_root, openRoute(context, appWidgetId))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun openRoute(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("discipulus://$route"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadItems(widgetData: SharedPreferences): List<JSONObject> {
        val raw = widgetData.getString(snapshotKey, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun widgetColors(context: Context, widgetData: SharedPreferences): WidgetColors {
        val isDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val fallbackBackground = if (isDark) Color.rgb(32, 33, 36) else Color.WHITE
        val fallbackPrimary = if (isDark) Color.WHITE else Color.rgb(32, 33, 36)
        val fallbackSecondary = if (isDark) Color.LTGRAY else Color.DKGRAY
        val json = runCatching {
            widgetData.getString("colors", null)?.let(::JSONObject)
        }.getOrNull()
        return WidgetColors(
            background = json?.optInt(
                if (isDark) "darkBackground" else "background",
                fallbackBackground
            ) ?: fallbackBackground,
            primary = json?.optInt(
                if (isDark) "darkPrimary" else "primary",
                fallbackPrimary
            ) ?: fallbackPrimary,
            secondary = json?.optInt(
                if (isDark) "darkSecondary" else "secondary",
                fallbackSecondary
            ) ?: fallbackSecondary
        )
    }

    private data class WidgetColors(
        val background: Int,
        val primary: Int,
        val secondary: Int
    )
}
