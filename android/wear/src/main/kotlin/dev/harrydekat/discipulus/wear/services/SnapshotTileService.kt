package dev.harrydekat.discipulus.wear.services

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.harrydekat.discipulus.wear.WearAppActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class SnapshotTileService : TileService() {
    protected abstract fun lines(): List<String>
    protected open fun destination() = "home"

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        val content = lines().take(3)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WearAppActivity::class.java).putExtra("destination", destination()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val column = Column.Builder().setWidth(expand()).setHeight(expand()).apply {
            content.forEachIndexed { index, line ->
                if (index == 0) {
                    addContent(text(line, 18f, 700, 0xFFD0BCFF.toInt()))
                } else {
                    addContent(
                        Box.Builder()
                            .setWidth(expand())
                            .setModifiers(
                                Modifiers.Builder()
                                    .setPadding(Padding.Builder().setAll(dp(8f)).build())
                                    .setBackground(
                                        Background.Builder()
                                            .setColor(argb(0xFF2B2930.toInt()))
                                            .setCorner(Corner.Builder().setRadius(dp(14f)).build())
                                            .build(),
                                    )
                                    .setClickable(Clickable.Builder().setId("open_$index").setOnClick(openApp).build())
                                    .build(),
                            )
                            .addContent(text(line, 13f, 500, 0xFFE6E1E5.toInt()))
                            .build(),
                    )
                }
            }
        }.build()
        return Futures.immediateFuture(
            Tile.Builder().setResourcesVersion("1").setFreshnessIntervalMillis(300000)
                .setTileTimeline(Timeline.fromLayoutElement(column)).build(),
        )
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())

    private fun text(value: String, size: Float, weight: Int, color: Int) =
        Text.Builder().setText(value).setFontStyle(
            FontStyle.Builder().setSize(sp(size)).setWeight(weight).setColor(argb(color)).build(),
        ).build()
}

class CalendarTileService : SnapshotTileService() {
    override fun destination() = "schedule"

    override fun lines(): List<String> {
        val upcoming = WearSnapshot.schedule(this)
            .filter { it.endTime.after(Date()) }
            .take(2)
        if (upcoming.isEmpty()) return listOf("Agenda", "Geen lessen gepland")
        return listOf("Agenda") + upcoming.map { event ->
            "${SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.startTime)} · ${event.shortName ?: event.name.take(18)}"
        }
    }
}

class GradesTileService : SnapshotTileService() {
    override fun lines(): List<String> {
        val schoolyear = WearSnapshot.schoolyears(this).firstOrNull()
        val average = schoolyear?.averages?.map { it.average }?.filter(Double::isFinite)
            ?.takeIf { it.isNotEmpty() }?.average()
        val grade = schoolyear?.recentGrades?.firstOrNull()
        if (average == null && grade == null) return listOf("Cijfers", "Nog geen cijfers")
        return listOfNotNull(
            "Cijfers",
            average?.let { "Gemiddeld ${"%.1f".format(Locale.getDefault(), it)}" },
            grade?.let { "${it.subject.take(14)}: ${it.grade}" },
        )
    }
}

class MessagesTileService : SnapshotTileService() {
    override fun lines(): List<String> {
        val message = WearSnapshot.messages(this).firstOrNull()
        val unread = WearSnapshot.unreadMessages(this)
        return listOfNotNull(
            "Berichten",
            "$unread ongelezen",
            message?.let { "${it.sender.take(12)}: ${it.subject.take(16)}" },
        )
    }
}
