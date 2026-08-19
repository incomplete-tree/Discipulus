import QtQuick 2.15
import QtQuick.Layouts 1.15
import org.kde.kirigami 2.20 as Kirigami
import org.kde.plasma.components 3.0 as PlasmaComponents3
import org.kde.plasma.core as PlasmaCore
import org.kde.plasma.plasmoid

PlasmoidItem {
    id: root

    readonly property int unit: Kirigami.Units.gridUnit

    Plasmoid.icon: "view-calendar"
    Layout.minimumWidth: unit * 16
    Layout.minimumHeight: unit * 7
    Layout.preferredWidth: unit * 18
    Layout.preferredHeight: unit * 8

    function open(route) {
        Qt.openUrlExternally("discipulus://" + route)
    }

    compactRepresentation: Kirigami.Icon {
        source: "view-calendar"

        PlasmaCore.ToolTipArea {
            anchors.fill: parent
            mainText: "Discipulus"
            subText: "Open je schoolplanner"
        }

        MouseArea {
            id: compactMouse
            anchors.fill: parent
            hoverEnabled: true
            onClicked: root.open("calendar")
        }
    }

    fullRepresentation: PlasmaComponents3.Control {
        id: card
        anchors.fill: parent
        Layout.minimumWidth: root.unit * 16
        Layout.minimumHeight: root.unit * 7
        padding: root.unit / 2

        contentItem: ColumnLayout {
            spacing: root.unit / 3

            PlasmaComponents3.Label {
                Layout.fillWidth: true
                text: "Discipulus"
                font.bold: true
                font.pointSize: Kirigami.Theme.defaultFont.pointSize + 3
            }

            PlasmaComponents3.Label {
                Layout.fillWidth: true
                text: "Open je schoolplanner"
                opacity: 0.75
                elide: Text.ElideRight
            }

            RowLayout {
                Layout.fillWidth: true
                spacing: root.unit / 3

                PlasmaComponents3.Button {
                    Layout.fillWidth: true
                    text: "Agenda"
                    icon.name: "view-calendar"
                    onClicked: root.open("calendar")
                }

                PlasmaComponents3.Button {
                    Layout.fillWidth: true
                    text: "Cijfers"
                    icon.name: "view-statistics"
                    onClicked: root.open("grades")
                }

                PlasmaComponents3.Button {
                    Layout.fillWidth: true
                    text: "Berichten"
                    icon.name: "mail-message-new"
                    onClicked: root.open("messages")
                }
            }

            PlasmaComponents3.Button {
                Layout.fillWidth: true
                text: "Open Discipulus"
                icon.name: "application-x-executable"
                onClicked: root.open("calendar")
            }
        }
    }
}
