import QtQuick 2.15
import QtQuick.Layouts 1.15
import Qt.labs.platform 1.1 as Platform
import org.kde.kirigami 2.20 as Kirigami
import org.kde.plasma.components 3.0 as PlasmaComponents3
import org.kde.plasma.core as PlasmaCore
import org.kde.plasma.plasmoid
import org.kde.plasma.plasma5support 2.0 as P5Support

PlasmoidItem {
    id: root

    readonly property int unit: Kirigami.Units.gridUnit
    readonly property string widgetType: Plasmoid.configuration.widgetType || "calendar"
    readonly property string title: widgetType === "grades" ? "Cijfers" : widgetType === "messages" ? "Berichten" : "Agenda"
    readonly property string icon: widgetType === "grades" ? "view-statistics" : widgetType === "messages" ? "mail-message-new" : "view-calendar"
    property var snapshot: ({ events: [], grades: [], gradesAverage: "", messages: [], messagesUnread: 0 })

    Plasmoid.icon: root.icon
    Layout.minimumWidth: unit * 14
    Layout.minimumHeight: unit * 7

    function snapshotPath() {
        var location = Platform.StandardPaths.writableLocation(Platform.StandardPaths.GenericDataLocation).toString()
        return decodeURIComponent(location.replace(/^file:\/\//, "")) + "/discipulus/widget-snapshot.json"
    }

    function shellQuote(value) {
        return "'" + value.replace(/'/g, "'\\''") + "'"
    }

    function formatEvent(event) {
        return (event.name || "Les") + " · " + Qt.formatTime(new Date(Number(event.startTime)), "HH:mm")
    }

    function lines() {
        var result = []
        if (widgetType === "calendar") {
            var now = Date.now()
            for (var i = 0; i < snapshot.events.length && result.length < 3; i++) {
                if (Number(snapshot.events[i].endTime) > now) result.push(formatEvent(snapshot.events[i]))
            }
            return result.length ? result : ["Geen lessen gevonden"]
        }
        if (widgetType === "grades") {
            for (var g = 0; g < snapshot.grades.length && result.length < 3; g++) {
                result.push((snapshot.grades[g].subject || "Vak") + " · " + (snapshot.grades[g].grade || "-"))
            }
            return result.length ? result : ["Nog geen cijfers"]
        }
        for (var m = 0; m < snapshot.messages.length && result.length < 3; m++) {
            result.push((snapshot.messages[m].read ? "" : "• ") + (snapshot.messages[m].sender || "Bericht"))
        }
        return result.length ? result : ["Geen berichten"]
    }

    readonly property var displayLines: lines()
    readonly property string subtitle: widgetType === "grades" && snapshot.gradesAverage ? "Gemiddelde " + snapshot.gradesAverage : widgetType === "messages" && Number(snapshot.messagesUnread) > 0 ? Number(snapshot.messagesUnread) + " ongelezen" : widgetType === "messages" ? "Laatste berichten" : "Komende lessen"

    P5Support.DataSource {
        id: source
        engine: "executable"
        connectedSources: ["cat " + root.shellQuote(root.snapshotPath())]
        interval: 30000

        onNewData: function(sourceName, data) {
            if (!data["stdout"]) return
            try { root.snapshot = JSON.parse(data["stdout"]) } catch (error) {}
        }
    }

    function open() { Qt.openUrlExternally("discipulus://" + widgetType) }

    compactRepresentation: Kirigami.Icon {
        source: root.icon
        PlasmaCore.ToolTipArea { anchors.fill: parent; mainText: root.title; subText: root.subtitle }
        MouseArea { anchors.fill: parent; onClicked: root.open() }
    }

    fullRepresentation: PlasmaComponents3.Control {
        anchors.fill: parent
        padding: root.unit / 2
        contentItem: ColumnLayout {
            spacing: root.unit / 3
            PlasmaComponents3.Label { text: root.title; font.bold: true; font.pointSize: Kirigami.Theme.defaultFont.pointSize + 3 }
            PlasmaComponents3.Label { text: root.subtitle; opacity: 0.75; elide: Text.ElideRight }
            ColumnLayout {
                Layout.fillWidth: true
                Repeater {
                    model: root.displayLines
                    PlasmaComponents3.Label { text: modelData; elide: Text.ElideRight }
                }
            }
            Item { Layout.fillHeight: true }
            PlasmaComponents3.Button { Layout.fillWidth: true; text: "Openen in Discipulus"; icon.name: root.icon; onClicked: root.open() }
        }
    }
}
