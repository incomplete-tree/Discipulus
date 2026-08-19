package dev.harrydekat.discipulus.wear.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.harrydekat.discipulus.wear.R
import dev.harrydekat.discipulus.wear.WearAppActivity
import java.util.Locale

private fun tapAction(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, WearAppActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun icon(context: Context) = MonochromaticImage.Builder(
    Icon.createWithResource(context, R.drawable.discipulus_complication_icon),
).build()

private fun preview(
    context: Context,
    requestCode: Int,
    type: ComplicationType,
    shortText: String,
    longText: String,
): ComplicationData? {
    val action = tapAction(context, requestCode)
    return when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(shortText).build(),
            PlainComplicationText.Builder("Voorbeeld").build(),
        ).setTapAction(action).build()
        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            PlainComplicationText.Builder(longText).build(),
            PlainComplicationText.Builder("Voorbeeld").build(),
        ).setTapAction(action).build()
        else -> null
    }
}

class GradesComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val schoolyear = WearSnapshot.schoolyears(this).firstOrNull()
        val grade = schoolyear?.recentGrades?.firstOrNull()
        val average = schoolyear?.averages?.map { it.average }?.filter(Double::isFinite)
            ?.takeIf { it.isNotEmpty() }?.average()
        val action = tapAction(this, 1)
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(grade?.grade ?: "-").build(),
                PlainComplicationText.Builder("Recent cijfer").build(),
            ).setTitle(PlainComplicationText.Builder(grade?.subject?.take(4) ?: "Cijfer").build())
                .setTapAction(action).setMonochromaticImage(icon(this)).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(grade?.let { "${it.subject}: ${it.grade}" } ?: "Nog geen cijfers").build(),
                PlainComplicationText.Builder("Recent cijfer").build(),
            ).setTitle(PlainComplicationText.Builder(average?.let { "Gem. ${"%.1f".format(Locale.getDefault(), it)}" } ?: "Cijfers").build())
                .setTapAction(action).setMonochromaticImage(icon(this)).build()
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        preview(this, 1, type, "7,4", "Wiskunde: 7,4")
}

class MessagesComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val message = WearSnapshot.messages(this).firstOrNull()
        val unread = WearSnapshot.unreadMessages(this)
        val action = tapAction(this, 2)
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(unread.toString()).build(),
                PlainComplicationText.Builder("Ongelezen berichten").build(),
            ).setTitle(PlainComplicationText.Builder("Berichten").build())
                .setTapAction(action).setMonochromaticImage(icon(this)).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(message?.subject ?: "Geen berichten").build(),
                PlainComplicationText.Builder("Laatste bericht").build(),
            ).setTitle(PlainComplicationText.Builder(message?.sender ?: "$unread ongelezen").build())
                .setTapAction(action).setMonochromaticImage(icon(this)).build()
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        preview(this, 2, type, "3", "Nieuwe toetsweek")
}
