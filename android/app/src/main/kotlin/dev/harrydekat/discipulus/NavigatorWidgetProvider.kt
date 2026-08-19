package dev.harrydekat.discipulus

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetPlugin
import es.antonborri.home_widget.HomeWidgetProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Android home-screen widgets backed by the data written by HomeWidget.
 *
 * The provider deliberately uses RemoteViews-only APIs so it works on launchers
 * with no Flutter process running. The Flutter background refresh writes the
 * event snapshot and then asks this provider to redraw.
 */
class NavigatorWidgetProvider : HomeWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(
                context,
                appWidgetManager,
                appWidgetId,
                widgetData,
                appWidgetManager.getAppWidgetOptions(appWidgetId)
            )
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
            HomeWidgetPlugin.getData(context),
            newOptions
        )
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        widgetData: SharedPreferences,
        options: android.os.Bundle
    ) {
        val events = loadEvents(widgetData)
        val layout = layoutFor(options)
        val views = when (layout) {
            R.layout.widget_medium -> mediumWidget(context, events, widgetData)
            R.layout.widget_rectangular -> rectangularWidget(context, events, widgetData)
            else -> smallWidget(context, events, widgetData)
        }

        val openCalendar = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("discipulus://calendar"),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openCalendar)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun layoutFor(options: android.os.Bundle): Int {
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        return when {
            width >= 250 && height >= 100 -> R.layout.widget_medium
            width >= 180 && height >= 50 -> R.layout.widget_rectangular
            else -> R.layout.widget_small
        }
    }

    private fun smallWidget(
        context: Context,
        events: List<Event>,
        widgetData: SharedPreferences
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_small)
        val now = Date().time
        val current = events.firstOrNull { it.startTime <= now && it.endTime > now }
        val next = events.firstOrNull { it.startTime > now }
        val primary = current ?: next
        val colors = widgetColors(context, widgetData)

        if (primary == null) {
            views.setTextViewText(R.id.event_location, "Geen lessen vandaag")
            views.setTextViewText(R.id.event_name, "")
            views.setTextViewText(R.id.event_time, "")
        } else {
            views.setTextViewText(
                R.id.event_location,
                primary.location ?: if (current == null) "Volgende les" else "Geen locatie"
            )
            views.setTextViewText(R.id.event_name, primary.name)
            views.setTextViewText(R.id.event_time, DateUtils.formatTime(Date(primary.startTime)))
            views.setTextColor(R.id.event_location, primary.infotypeColor(true))
        }

        views.setTextViewText(
            R.id.next_event,
            next?.takeIf { it.id != primary?.id }
                ?.let { "${it.name} ${DateUtils.formatDate(it.startTime - now)}" }
                ?: "Geen volgende les"
        )
        applyColors(views, colors, hasEventLocation = true, hasNext = true)
        return views
    }

    private fun mediumWidget(
        context: Context,
        events: List<Event>,
        widgetData: SharedPreferences
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_medium)
        val now = Date().time
        val current = events.firstOrNull { it.startTime <= now && it.endTime > now }
        val next = events.firstOrNull { it.startTime > now }
        val primary = current ?: next
        val colors = widgetColors(context, widgetData)

        if (primary == null) {
            views.setTextViewText(R.id.event_location, "Geen lessen vandaag")
            views.setTextViewText(R.id.event_name, "")
            views.setTextViewText(R.id.event_time, "")
        } else {
            views.setTextViewText(
                R.id.event_location,
                primary.location ?: if (current == null) "Volgende les" else "Geen locatie"
            )
            views.setTextViewText(R.id.event_name, primary.name)
            views.setTextViewText(R.id.event_time, DateUtils.formatTime(Date(primary.startTime)))
            views.setTextColor(R.id.event_location, primary.infotypeColor(true))
        }

        val upcoming = events.filter { it.startTime > now }.take(3)
        for (index in 0 until 3) {
            val viewId = context.resources.getIdentifier(
                "upcoming_event_${index + 1}",
                "id",
                context.packageName
            )
            val event = upcoming.getOrNull(index)
            if (event == null) {
                views.setViewVisibility(viewId, View.GONE)
            } else {
                views.setTextViewText(
                    viewId,
                    "${event.name} · ${DateUtils.formatDate(event.startTime - now)}"
                )
                views.setViewVisibility(viewId, View.VISIBLE)
            }
        }

        applyColors(views, colors, hasEventLocation = true, hasUpcoming = true)
        return views
    }

    private fun rectangularWidget(
        context: Context,
        events: List<Event>,
        widgetData: SharedPreferences
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_rectangular)
        val now = Date().time
        val upcoming = events.firstOrNull { it.startTime > now }
        val colors = widgetColors(context, widgetData)

        if (upcoming == null) {
            views.setImageViewBitmap(R.id.location_circle, circleBitmap(colors.secondary))
            views.setTextViewText(R.id.event_name, "Geen lessen gevonden")
            views.setTextViewText(R.id.event_time, "")
        } else {
            views.setTextViewText(R.id.event_name, upcoming.name)
            views.setTextViewText(
                R.id.event_time,
                DateUtils.formatDate(upcoming.startTime - now)
            )
            views.setImageViewBitmap(
                R.id.location_circle,
                circleBitmap(upcoming.infotypeColor())
            )
        }

        applyColors(views, colors)
        return views
    }

    private fun applyColors(
        views: RemoteViews,
        colors: WidgetColors,
        hasEventLocation: Boolean = false,
        hasNext: Boolean = false,
        hasUpcoming: Boolean = false
    ) {
        views.setInt(R.id.widget_root, "setBackgroundColor", colors.background)
        views.setTextColor(R.id.event_name, colors.primary)
        views.setTextColor(R.id.event_time, colors.secondary)
        if (hasEventLocation) views.setTextColor(R.id.event_location, colors.primary)
        if (hasNext) views.setTextColor(R.id.next_event, colors.secondary)
        if (hasUpcoming) {
            views.setTextColor(R.id.upcoming_event_1, colors.primary)
            views.setTextColor(R.id.upcoming_event_2, colors.primary)
            views.setTextColor(R.id.upcoming_event_3, colors.primary)
        }
    }

    private fun circleBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawCircle(
            48f,
            48f,
            48f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        )
        return bitmap
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
        val backgroundKey = if (isDark) "darkBackground" else "background"
        val primaryKey = if (isDark) "darkPrimary" else "primary"
        val secondaryKey = if (isDark) "darkSecondary" else "secondary"
        return WidgetColors(
            background = json?.optInt(backgroundKey, fallbackBackground) ?: fallbackBackground,
            primary = json?.optInt(primaryKey, fallbackPrimary) ?: fallbackPrimary,
            secondary = json?.optInt(secondaryKey, fallbackSecondary) ?: fallbackSecondary
        )
    }

    private fun loadEvents(widgetData: SharedPreferences): List<Event> {
        val raw = widgetData.getString("events", null) ?: return emptyList()
        val now = System.currentTimeMillis()
        val earliestAllowed = now - 2L * 24 * 60 * 60 * 1000
        val latestAllowed = now + 35L * 24 * 60 * 60 * 1000
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val id = json.optLong("id", Long.MIN_VALUE)
                    val name = json.optString("name").trim()
                    val start = json.optLong("startTime", Long.MIN_VALUE)
                    val end = json.optLong("endTime", Long.MIN_VALUE)
                    if (id !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() ||
                        name.isEmpty() || start < earliestAllowed ||
                        start > latestAllowed || end <= start) {
                        continue
                    }
                    add(
                        Event(
                            id = id.toInt(),
                            name = name,
                            shortName = json.optString("shortName").takeIf { it.isNotBlank() },
                            location = json.optString("location").takeIf { it.isNotBlank() },
                            infoType = json.optInt("infoType", 0),
                            startHourIndicator = json.optIntOrNull("startHourIndicator"),
                            endHourIndicator = json.optIntOrNull("endHourIndicator"),
                            startTime = start,
                            endTime = end,
                            isCompleted = json.optBoolean("isCompleted", false)
                        )
                    )
                }
            }.sortedBy { it.startTime }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private data class WidgetColors(
        val background: Int,
        val primary: Int,
        val secondary: Int
    )
}
