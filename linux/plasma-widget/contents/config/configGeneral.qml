import QtQuick 2.15
import QtQuick.Controls 2.15 as QQC2
import org.kde.kcmutils as KCM
import org.kde.kirigami 2.20 as Kirigami

KCM.SimpleKCM {
    property string cfg_widgetType

    Kirigami.FormLayout {
        QQC2.ComboBox {
            id: widgetType
            Kirigami.FormData.label: "Toon:"
            model: ["Agenda", "Cijfers", "Berichten"]
            currentIndex: Math.max(0, model.indexOf(cfg_widgetType === "grades" ? "Cijfers" : cfg_widgetType === "messages" ? "Berichten" : "Agenda"))
            onActivated: index => cfg_widgetType = ["calendar", "grades", "messages"][index]
        }
    }
}
