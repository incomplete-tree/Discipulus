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

class GradesComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val schoolyear = WearSnapshot.schoolyears(this).firstOrNull()
        val grade = schoolyear?.recentGrades?.firstOrNull()
        val average = schoolyear?.averages
            ?.map { it.average }
            ?.filter(Double::isFinite)
            ?.takeIf { it.isNotEmpty() }
            ?.average()
        val tapAction = getTapAction(this)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(grade?.grade ?: "-").build(),
                contentDescription = PlainComplicationText.Builder("Recent cijfer").build(),
            ).setTitle(
                PlainComplicationText.Builder(grade?.subject?.take(4) ?: "Cijfer").build(),
            ).setTapAction(tapAction).setMonochromaticImage(icon()).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    grade?.let { "${it.subject}: ${it.grade}" } ?: "Nog geen cijfers",
                ).build(),
                contentDescription = PlainComplicationText.Builder("Recent cijfer").build(),
            ).setTitle(
                PlainComplicationText.Builder(
                    average?.let { "Gem. ${"%.1f".format(Locale.getDefault(), it)}" } ?: "Cijfers",
                ).build(),
            ).setTapAction(tapAction).setMonochromaticImage(icon()).build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val tapAction = getTapAction(this)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("7,4").build(),
                contentDescription = PlainComplicationText.Builder("Voorbeeld cijfer").build(),
            ).setTitle(PlainComplicationText.Builder("WIS").build()).setTapAction(tapAction).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Wiskunde: 7,4").build(),
                contentDescription = PlainComplicationText.Builder("Voorbeeld cijfer").build(),
            ).setTitle(PlainComplicationText.Builder("Gem. 7,1").build()).setTapAction(tapAction).build()
            else -> null
        }
    }

    private fun icon() = MonochromaticImage.Builder(
        Icon.createWithResource(this, R.drawable.discipulus_complication_icon),
    ).build()

    private fun getTapAction(context: Context): PendingIntent {
        val intent = Intent(context, WearAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
