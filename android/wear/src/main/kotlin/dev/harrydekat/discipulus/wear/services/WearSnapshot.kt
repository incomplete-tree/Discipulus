package dev.harrydekat.discipulus.wear.services

import android.content.Context
import android.util.Base64
import android.util.Log
import dev.harrydekat.discipulus.wear.models.ScheduleEvent
import dev.harrydekat.discipulus.wear.models.SchoolYearData
import dev.harrydekat.discipulus.wear.models.WatchMessage
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.Date

object WearSnapshot {
    private const val TAG = "WearSnapshot"
    private const val PREFS = "discipulus_wear_prefs"

    private inline fun <reified T> read(context: Context, key: String): T? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as? T }
        } catch (error: Exception) {
            Log.w(TAG, "Could not read $key", error)
            null
        }
    }

    private fun showCancelled(context: Context) = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean("show_cancelled_lessons", false)

    fun schedule(context: Context): List<ScheduleEvent> {
        val includeCancelled = showCancelled(context)
        return read<Map<String, List<ScheduleEvent>>>(context, "cached_schedule")
            ?.values?.flatten()?.filter { includeCancelled || !it.isCanceled }
            ?.sortedBy(ScheduleEvent::startTime) ?: emptyList()
    }

    fun nextEvent(context: Context): ScheduleEvent? {
        val now = Date()
        val events = schedule(context)
        events.firstOrNull {
            it.startTime.time <= now.time &&
                it.endTime.time > now.time &&
                !(it.isCompleted && it.infoType == 1)
        }?.let { return it }
        return events.firstOrNull {
            it.startTime.after(now) && !(it.isCompleted && it.infoType == 1)
        }
    }

    fun schoolyears(context: Context): List<SchoolYearData> =
        read<List<SchoolYearData>>(context, "cached_schoolyears") ?: emptyList()

    fun messages(context: Context): List<WatchMessage> =
        read<List<WatchMessage>>(context, "cached_messages") ?: emptyList()

    fun unreadMessages(context: Context): Int = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt("cached_messages_unread", 0)
}
