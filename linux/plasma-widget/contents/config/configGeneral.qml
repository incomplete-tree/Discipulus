import QtQuick 2.15
import QtQuick.Controls 2.15 as QQC2
import org.kde.kcmutils as KCM
import org.kde.kirigami 2.20 as Kirigami

KCM.SimpleKCM {
    property string cfg_widgetType

    Kirigami.FormLayout {
        QQC2.ComboBox {
            Kirigami.FormData.label: "Toon:"
            model: ["Agenda", "Cijfers", "Berichten"]
            currentIndex: ["calendar", "grades", "messages"].indexOf(cfg_widgetType || "calendar")
            onActivated: cfg_widgetType = ["calendar", "grades", "messages"][index]
        }
    }
}
