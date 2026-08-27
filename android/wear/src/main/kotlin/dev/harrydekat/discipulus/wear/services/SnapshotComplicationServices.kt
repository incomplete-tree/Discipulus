package dev.harrydekat.discipulus.wear.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.harrydekat.discipulus.wear.R
import dev.harrydekat.discipulus.wear.WearAppActivity
import java.text.SimpleDateFormat
import java.util.Locale

private data class ComplicationContent(
    val shortText: String,
    val longText: String,
    val title: String,
    val value: Float,
)

private fun tapAction(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, WearAppActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun monochromaticIcon(context: Context) = MonochromaticImage.Builder(
    Icon.createWithResource(context, R.drawable.discipulus_complication_icon),
).build()

private fun smallIcon(context: Context) = SmallImage.Builder(
    Icon.createWithResource(context, R.drawable.discipulus_complication_icon),
    SmallImageType.ICON,
).build()

private fun complicationData(
    context: Context,
    requestCode: Int,
    type: ComplicationType,
    content: ComplicationContent,
): ComplicationData? {
    val action = tapAction(context, requestCode)
    val description = PlainComplicationText.Builder(content.longText).build()
    val title = PlainComplicationText.Builder(content.title).build()
    val text = PlainComplicationText.Builder(content.longText).build()
    val value = content.value.coerceIn(0f, 10f)
    return when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(content.shortText).build(), description,
        ).setTitle(title).setMonochromaticImage(monochromaticIcon(context)).setTapAction(action).build()
        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text, description)
            .setTitle(title).setMonochromaticImage(monochromaticIcon(context)).setTapAction(action).build()
        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(value, 0f, 10f, description)
            .setTitle(title).setText(text).setMonochromaticImage(monochromaticIcon(context)).setTapAction(action).build()
        ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
            monochromaticIcon(context), description,
        ).setTapAction(action).build()
        ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(smallIcon(context), description)
            .setTapAction(action).build()
        ComplicationType.PHOTO_IMAGE -> PhotoImageComplicationData.Builder(
            Icon.createWithResource(context, R.mipmap.ic_launcher), description,
        ).setTapAction(action).build()
        ComplicationType.GOAL_PROGRESS -> GoalProgressComplicationData.Builder(value, 10f, description)
            .setTitle(title).setText(text).setMonochromaticImage(monochromaticIcon(context)).setTapAction(action).build()
        ComplicationType.WEIGHTED_ELEMENTS -> WeightedElementsComplicationData.Builder(
            listOf(
                WeightedElementsComplicationData.Element(value, Color.WHITE),
                WeightedElementsComplicationData.Element(10f - value, Color.DKGRAY),
            ),
            description,
        ).setTitle(title).setText(text).setMonochromaticImage(monochromaticIcon(context)).setTapAction(action).build()
        else -> null
    }
}

private fun nextLessonContent(context: Context): ComplicationContent {
    val event = WearSnapshot.nextEvent(context)
        ?: return ComplicationContent("Vrij", "Geen lessen gepland", "Agenda", 0f)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.startTime)
    return ComplicationContent(
        event.location ?: event.startHourIndicator?.let { "${it}e" } ?: event.name.take(4),
        event.name,
        "${event.location ?: time} · $time",
        (event.startHourIndicator ?: 0).toFloat(),
    )
}

private fun latestGradeContent(context: Context): ComplicationContent {
    val grade = WearSnapshot.schoolyears(context).firstOrNull()?.recentGrades?.firstOrNull()
        ?: return ComplicationContent("-", "Nog geen cijfers", "Cijfers", 0f)
    return ComplicationContent(
        grade.grade,
        "${grade.subject}: ${grade.grade}",
        grade.subject.take(16),
        grade.grade.replace(',', '.').toFloatOrNull() ?: 0f,
    )
}

class NextLessonComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        complicationData(this, 1, request.complicationType, nextLessonContent(this))

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        complicationData(this, 1, type, ComplicationContent("103", "Wiskunde", "103 · 08:30", 1f))
}

class LatestGradeComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        complicationData(this, 2, request.complicationType, latestGradeContent(this))

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        complicationData(this, 2, type, ComplicationContent("7,4", "Wiskunde: 7,4", "Wiskunde", 7.4f))
}
