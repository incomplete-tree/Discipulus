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

private const val TILE_RESOURCES_VERSION = "1"
private const val TILE_FRESHNESS_MILLIS = 5 * 60 * 1000L

abstract class SnapshotTileService : TileService() {
    protected abstract fun snapshotLines(): List<String>

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> {
        val layout = Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .apply {
                snapshotLines().take(4).forEachIndexed { index, line ->
                    addContent(text(line, if (index == 0) 18f else 14f, index == 0))
                }
            }
            .build()

        return Futures.immediateFuture(
            Tile.Builder()
                .setResourcesVersion(TILE_RESOURCES_VERSION)
                .setFreshnessIntervalMillis(TILE_FRESHNESS_MILLIS)
                .setTileTimeline(Timeline.fromLayoutElement(layout))
                .build(),
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = Futures.immediateFuture(
        ResourceBuilders.Resources.Builder()
            .setVersion(TILE_RESOURCES_VERSION)
            .build(),
    )

    private fun text(value: String, size: Float, bold: Boolean): LayoutElement =
        Text.Builder()
            .setText(value)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(size))
                    .setWeight(if (bold) 700 else 400)
                    .build(),
            )
            .build()
}

class CalendarTileService : SnapshotTileService() {
    override fun snapshotLines(): List<String> {
        val event = WearSnapshot.nextEvent(this)
        if (event == null) return listOf("Agenda", "Geen lessen")

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.startTime)
        return listOf(
            "Agenda",
            "${event.shortName ?: event.name.take(18)} · $time",
            event.location?.takeIf(String::isNotBlank) ?: "Geen lokaal",
        )
    }
}

class GradesTileService : SnapshotTileService() {
    override fun snapshotLines(): List<String> {
        val schoolyear = WearSnapshot.schoolyears(this).firstOrNull()
        val average = schoolyear?.averages
            ?.map { it.average }
            ?.filter(Double::isFinite)
            ?.takeIf { it.isNotEmpty() }
            ?.average()
        val grade = schoolyear?.recentGrades?.firstOrNull()
        return buildList {
            add("Cijfers")
            average?.let { add("Gemiddeld ${"%.1f".format(Locale.getDefault(), it)}") }
            grade?.let { add("${it.subject.take(14)}: ${it.grade}") }
            if (size == 1) add("Nog geen cijfers")
        }
    }
}

class MessagesTileService : SnapshotTileService() {
    override fun snapshotLines(): List<String> {
        val message = WearSnapshot.messages(this).firstOrNull()
        val unread = WearSnapshot.unreadMessages(this)
        return listOfNotNull(
            "Berichten",
            "$unread ongelezen",
            message?.let { "${it.sender.take(12)}: ${it.subject.take(16)}" },
        ).ifEmpty { listOf("Berichten", "Geen berichten") }
    }
}
