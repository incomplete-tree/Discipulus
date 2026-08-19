package dev.harrydekat.discipulus.wear.services

import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Locale

abstract class SnapshotTileService : TileService() {
    protected abstract fun lines(): List<String>

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        val column = Column.Builder().setWidth(expand()).setHeight(expand()).apply {
            lines().take(4).forEachIndexed { index, line ->
                addContent(
                    Text.Builder().setText(line).setFontStyle(
                        FontStyle.Builder().setSize(sp(if (index == 0) 18f else 14f))
                            .setWeight(if (index == 0) 700 else 400).build()
                    ).build()
                )
            }
        }.build()
        return Futures.immediateFuture(
            Tile.Builder().setResourcesVersion("1")
                .setFreshnessIntervalMillis(300000)
                .setTileTimeline(Timeline.fromLayoutElement(column)).build()
        )
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
}

class CalendarTileService : SnapshotTileService() {
    override fun lines(): List<String> {
        val event = WearSnapshot.nextEvent(this) ?: return listOf("Agenda", "Geen lessen")
        return listOf(
            "Agenda",
            "${event.shortName ?: event.name.take(18)} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.startTime)}",
            event.location?.takeIf(String::isNotBlank) ?: "Geen lokaal",
        )
    }
}

class GradesTileService : SnapshotTileService() {
    override fun lines(): List<String> {
        val year = WearSnapshot.schoolyears(this).firstOrNull()
        val average = year?.averages?.map { it.average }?.filter(Double::isFinite)
            ?.takeIf { it.isNotEmpty() }?.average()
        val grade = year?.recentGrades?.firstOrNull()
        return listOfNotNull("Cijfers", average?.let { "Gemiddeld ${"%.1f".format(Locale.getDefault(), it)}" }, grade?.let { "${it.subject.take(14)}: ${it.grade}" })
            .ifEmpty { listOf("Cijfers", "Nog geen cijfers") }
    }
}

class MessagesTileService : SnapshotTileService() {
    override fun lines(): List<String> {
        val message = WearSnapshot.messages(this).firstOrNull()
        val unread = WearSnapshot.unreadMessages(this)
        return listOfNotNull("Berichten", "$unread ongelezen", message?.let { "${it.sender.take(12)}: ${it.subject.take(16)}" })
            .ifEmpty { listOf("Berichten", "Geen berichten") }
    }
}
