import SwiftUI
import WidgetKit

struct NavigatorWidgetEntryView : View {
    var entry: SimpleEntry
    @Environment(\.widgetFamily) var family
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        Group {
        switch family {
        #if os(iOS) || os(macOS)
        case .systemSmall:
            SystemSmallView(entry: entry)
        case .systemMedium:
            SystemMediumView(entry: entry)
        case .systemLarge:
            SystemLargeView(entry: entry)
        case .systemExtraLarge:
            SystemExtraLargeView(entry: entry)
        #endif

        case .accessoryInline:
            AccessoryInlineView(entry: entry)
        case .accessoryCircular:
            AccessoryCircularView(entry: entry)
        case .accessoryRectangular:
            AccessoryRectangularView(entry: entry)

        @unknown default:
            Text("Unsupported widget size")
        }
        }
        .modifier(WidgetBackgroundModifier(colorScheme: colorScheme, useDiscipulusColorscheme: !entry.configuration.useDiscipulusColorscheme))
    }
}

struct WidgetBackgroundModifier: ViewModifier {
    let colorScheme: ColorScheme
    let useDiscipulusColorscheme: Bool

    func body(content: Content) -> some View {
        if #available(iOS 17.0, macOS 14.0, watchOS 10.0, *) {
            content.containerBackground(for: .widget) {
                getWidgetColors(for: colorScheme, native: useDiscipulusColorscheme).background
            }
        } else {
            content.background(getWidgetColors(for: colorScheme, native: useDiscipulusColorscheme).background)
        }
    }
}

struct SnapshotWidgetEntryView: View {
    let entry: SnapshotEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            switch family {
            case .accessoryCircular:
                circularView
            case .accessoryInline:
                inlineView
            case .accessoryRectangular:
                rectangularView
            default:
                regularView
            }
        }
        .modifier(
            WidgetBackgroundModifier(
                colorScheme: colorScheme,
                useDiscipulusColorscheme: false
            )
        )
        .widgetURL(URL(string: "discipulus://\(entry.kind.route)"))
    }

    private var regularView: some View {
        VStack(alignment: .leading, spacing: 4) {
            header
            if entry.kind == .grades {
                if let average = entry.average {
                    Text("Gemiddelde \(average)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if entry.grades.isEmpty {
                    emptyText("Nog geen cijfers")
                } else {
                    ForEach(Array(entry.grades.prefix(3).enumerated()), id: \.offset) { _, grade in
                        Text("\(grade.subject) · \(grade.grade)")
                            .lineLimit(1)
                    }
                }
            } else {
                if entry.unreadMessages > 0 {
                    Text("\(entry.unreadMessages) ongelezen")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if entry.messages.isEmpty {
                    emptyText("Geen berichten")
                } else {
                    ForEach(Array(entry.messages.prefix(3).enumerated()), id: \.offset) { _, message in
                        Text("\(message.isRead ? "" : "• ")\(message.sender) · \(message.subject)")
                            .lineLimit(1)
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .padding()
    }

    private var header: some View {
        Label(entry.kind.title, systemImage: entry.kind.icon)
            .font(.headline)
            .lineLimit(1)
    }

    private var inlineView: some View {
        Text("\(entry.kind.title): \(summary)")
            .lineLimit(1)
    }

    private var rectangularView: some View {
        HStack(spacing: 8) {
            Image(systemName: entry.kind.icon)
                .widgetAccentable()
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.kind.title)
                    .font(.headline)
                    .lineLimit(1)
                Text(summary)
                    .font(.caption)
                    .lineLimit(1)
            }
        }
    }

    private var circularView: some View {
        ZStack {
            Circle().fill(Color.accentColor.opacity(0.2))
            VStack(spacing: 0) {
                Image(systemName: entry.kind.icon)
                    .font(.caption)
                    .widgetAccentable()
                Text(circularValue)
                    .font(.headline)
                    .lineLimit(1)
            }
        }
    }

    private var summary: String {
        switch entry.kind {
        case .grades:
            return entry.average.map { "Gemiddelde \($0)" } ?? "Nog geen cijfers"
        case .messages:
            return entry.unreadMessages > 0 ? "\(entry.unreadMessages) ongelezen" : "Geen nieuwe berichten"
        }
    }

    private var circularValue: String {
        switch entry.kind {
        case .grades:
            return entry.average ?? "—"
        case .messages:
            return "\(entry.unreadMessages)"
        }
    }

    private func emptyText(_ text: String) -> some View {
        Text(text)
            .foregroundColor(.secondary)
            .lineLimit(1)
    }
}
