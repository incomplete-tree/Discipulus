import WidgetKit
import SwiftUI

struct ConfigurationIntentKey: EnvironmentKey {
    static let defaultValue: ConfigurationIntent = ConfigurationIntent()
}

extension EnvironmentValues {
    var configurationIntent: ConfigurationIntent {
        get { self[ConfigurationIntentKey.self] }
        set { self[ConfigurationIntentKey.self] = newValue }
    }
}

struct NavigatorWidget: Widget {
    let kind: String = "NavigatorWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: Provider()) { entry in
            NavigatorWidgetEntryView(entry: entry).environment(\.configurationIntent, entry.configuration)
        }
        .configurationDisplayName("Komende lessen")
        .description("Zie waar je volgende les of lessen zijn")
        .supportedFamilies(supportedFamilies())
    }

    private func supportedFamilies() -> [WidgetFamily] {
        #if os(iOS) || os(macOS)
        return [
            .systemSmall,
            .systemMedium,
            .systemLarge,
            .systemExtraLarge,
            .accessoryCircular,
            .accessoryInline,
            .accessoryRectangular
        ]
        #elseif os(watchOS)
        return [
            .accessoryCircular,
            .accessoryInline,
            .accessoryRectangular
        ]
        #else
        return []
        #endif
    }
}

struct GradesWidget: Widget {
    let kind = "GradesWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider(kind: .grades)) { entry in
            SnapshotWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Cijfers")
        .description("Bekijk je recente cijfers en gemiddelde")
        .supportedFamilies(snapshotWidgetFamilies())
    }
}

struct MessagesWidget: Widget {
    let kind = "MessagesWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SnapshotProvider(kind: .messages)) { entry in
            SnapshotWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Berichten")
        .description("Bekijk je recente berichten")
        .supportedFamilies(snapshotWidgetFamilies())
    }
}

@main
struct DiscipulusWidgetBundle: WidgetBundle {
    var body: some Widget {
        NavigatorWidget()
        GradesWidget()
        MessagesWidget()
    }
}

private func snapshotWidgetFamilies() -> [WidgetFamily] {
    #if os(iOS) || os(macOS)
    return [
        .systemSmall,
        .systemMedium,
        .systemLarge,
        .systemExtraLarge,
        .accessoryCircular,
        .accessoryInline,
        .accessoryRectangular
    ]
    #elseif os(watchOS)
    return [
        .accessoryCircular,
        .accessoryInline,
        .accessoryRectangular
    ]
    #else
    return []
    #endif
}

struct NavigatorWidget_Previews: PreviewProvider {
    static var previews: some View {
        #if os(iOS) || os(macOS)
        NavigatorWidgetEntryView(entry: SimpleEntry(date: Date(), configuration: ConfigurationIntent(), events: sampleEvents))
            .previewContext(WidgetPreviewContext(family: .systemMedium))
        #elseif os(watchOS)
        NavigatorWidgetEntryView(entry: SimpleEntry(date: Date(), configuration: ConfigurationIntent(), events: sampleEvents))
            .previewContext(WidgetPreviewContext(family: .accessoryRectangular))
        #endif
    }
}
