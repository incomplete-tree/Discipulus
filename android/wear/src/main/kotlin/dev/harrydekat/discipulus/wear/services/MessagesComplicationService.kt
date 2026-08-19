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

class MessagesComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val message = WearSnapshot.messages(this).firstOrNull()
        val unread = WearSnapshot.unreadMessages(this)
        val tapAction = getTapAction(this)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(unread.toString()).build(),
                contentDescription = PlainComplicationText.Builder("Ongelezen berichten").build(),
            ).setTitle(PlainComplicationText.Builder("Berichten").build())
                .setTapAction(tapAction).setMonochromaticImage(icon()).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    message?.subject ?: "Geen berichten",
                ).build(),
                contentDescription = PlainComplicationText.Builder("Laatste bericht").build(),
            ).setTitle(
                PlainComplicationText.Builder(message?.sender ?: "$unread ongelezen").build(),
            ).setTapAction(tapAction).setMonochromaticImage(icon()).build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val tapAction = getTapAction(this)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("3").build(),
                contentDescription = PlainComplicationText.Builder("Voorbeeld berichten").build(),
            ).setTitle(PlainComplicationText.Builder("Berichten").build()).setTapAction(tapAction).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Nieuwe toetsweek").build(),
                contentDescription = PlainComplicationText.Builder("Voorbeeld bericht").build(),
            ).setTitle(PlainComplicationText.Builder("School").build()).setTapAction(tapAction).build()
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
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
