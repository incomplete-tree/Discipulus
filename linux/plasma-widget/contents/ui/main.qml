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
    readonly property string widgetTitle: widgetType === "grades" ? "Cijfers" : widgetType === "messages" ? "Berichten" : "Agenda"
    readonly property string widgetIcon: widgetType === "grades" ? "view-statistics" : widgetType === "messages" ? "mail-message-new" : "view-calendar"
    readonly property string snapshotPath: snapshotFilePath()
    readonly property string snapshotCommand: "cat " + shellQuote(snapshotPath)
    property var snapshot: ({
        events: [],
        grades: [],
        gradesAverage: "",
        messages: [],
        messagesUnread: 0
    })

    Plasmoid.icon: widgetIcon
    Layout.minimumWidth: unit * 14
    Layout.minimumHeight: unit * 7
    Layout.preferredWidth: unit * 18
    Layout.preferredHeight: unit * 10

    function open(route) {
        Qt.openUrlExternally("discipulus://" + route)
    }

    function snapshotFilePath() {
        var path = Platform.StandardPaths.writableLocation(Platform.StandardPaths.GenericDataLocation).toString()
        path = path.replace(/^file:\/\//, "")
        return decodeURIComponent(path) + "/discipulus/widget-snapshot.json"
    }

    function shellQuote(value) {
        return "'" + value.replace(/'/g, "'\\''") + "'"
    }

    function refreshSnapshot() {
        snapshotSource.updateSources()
    }

    function formatEvent(event) {
        var time = Qt.formatTime(new Date(Number(event.startTime)), "HH:mm")
        var location = event.location ? " · " + event.location : ""
        return (event.name || "Les") + " · " + time + location
    }

    function linesForCalendar() {
        var now = Date.now()
        var result = []
        var events = snapshot.events || []
        for (var index = 0; index < events.length && result.length < 3; index++) {
            if (Number(events[index].endTime) > now) {
                result.push(formatEvent(events[index]))
            }
        }
        return result.length ? result : ["Geen lessen gevonden"]
    }

    function linesForGrades() {
        var result = []
        var grades = snapshot.grades || []
        for (var index = 0; index < grades.length && result.length < 3; index++) {
            result.push((grades[index].subject || "Vak") + " · " + (grades[index].grade || "-"))
        }
        return result.length ? result : ["Nog geen cijfers"]
    }

    function linesForMessages() {
        var result = []
        var messages = snapshot.messages || []
        for (var index = 0; index < messages.length && result.length < 3; index++) {
            var marker = messages[index].read ? "" : "• "
            result.push(marker + (messages[index].sender || "Bericht") + " · " + (messages[index].subject || "Zonder onderwerp"))
        }
        return result.length ? result : ["Geen berichten"]
    }

    readonly property var lines: widgetType === "grades" ? linesForGrades() : widgetType === "messages" ? linesForMessages() : linesForCalendar()
    readonly property string subtitle: widgetType === "grades" && snapshot.gradesAverage ? "Gemiddelde " + snapshot.gradesAverage : widgetType === "messages" && Number(snapshot.messagesUnread) > 0 ? Number(snapshot.messagesUnread) + " ongelezen" : widgetType === "messages" ? "Laatste berichten" : widgetType === "grades" ? "Laatste resultaten" : "Komende lessen"

    P5Support.DataSource {
        id: snapshotSource
        engine: "executable"
        connectedSources: [root.snapshotCommand]
        interval: 30000

        onNewData: function(sourceName, data) {
            var raw = data["stdout"]
            if (!raw) {
                return
            }
            try {
                root.snapshot = JSON.parse(raw)
            } catch (error) {
                // The app writes snapshots atomically; ignore a stale or empty file.
            }
        }
    }

    Component.onCompleted: refreshSnapshot()

    compactRepresentation: Kirigami.Icon {
        source: root.widgetIcon

        PlasmaCore.ToolTipArea {
            anchors.fill: parent
            mainText: root.widgetTitle
            subText: root.subtitle
        }

        MouseArea {
            id: compactMouse
            anchors.fill: parent
            hoverEnabled: true
            onClicked: root.open(root.widgetType)
        }
    }

    fullRepresentation: PlasmaComponents3.Control {
        id: card
        anchors.fill: parent
        Layout.minimumWidth: root.unit * 14
        Layout.minimumHeight: root.unit * 7
        padding: root.unit / 2

        contentItem: ColumnLayout {
            spacing: root.unit / 3

            RowLayout {
                Layout.fillWidth: true

                PlasmaComponents3.Label {
                    Layout.fillWidth: true
                    text: root.widgetTitle
                    font.bold: true
                    font.pointSize: Kirigami.Theme.defaultFont.pointSize + 3
                    elide: Text.ElideRight
                }

                PlasmaComponents3.ToolButton {
                    icon.name: root.widgetIcon
                    onClicked: root.refreshSnapshot()
                    PlasmaCore.ToolTipArea {
                        anchors.fill: parent
                        mainText: "Vernieuwen"
                    }
                }
            }

            PlasmaComponents3.Label {
                Layout.fillWidth: true
                text: root.subtitle
                opacity: 0.75
                elide: Text.ElideRight
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: root.unit / 5

                Repeater {
                    model: root.lines

                    PlasmaComponents3.Label {
                        Layout.fillWidth: true
                        text: modelData
                        elide: Text.ElideRight
                    }
                }
            }

            Item { Layout.fillHeight: true }

            PlasmaComponents3.Button {
                Layout.fillWidth: true
                text: "Openen in Discipulus"
                icon.name: root.widgetIcon
                onClicked: root.open(root.widgetType)
            }
        }
    }
}
