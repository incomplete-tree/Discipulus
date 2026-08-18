import WidgetKit
import SwiftUI
import AppIntents

struct Provider: AppIntentTimelineProvider {
    typealias Entry = SimpleEntry
    typealias Intent = ConfigurationIntent

    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), configuration: ConfigurationIntent(), events: sampleEvents)
    }

    func snapshot(for configuration: ConfigurationIntent, in context: Context) async -> SimpleEntry {
            let entry = SimpleEntry(date: Date(), configuration: configuration, events: loadEvents())
            return entry
        }

    func timeline(for configuration: ConfigurationIntent, in context: Context) async -> Timeline<SimpleEntry> {
        let currentDate = Date()
        let events = loadEvents()
        var entries: [SimpleEntry] = []

        // Create timeline entries
        let calendar = Calendar.current
        let midnight = calendar.startOfDay(for: currentDate.addingTimeInterval(86400))

        for event in events where event.startTime > currentDate {
            // Add entry 10 minutes before event starts
            let tenMinutesBefore = calendar.date(byAdding: .minute, value: -10, to: event.startTime)!
            if tenMinutesBefore > currentDate {
                entries.append(SimpleEntry(date: tenMinutesBefore, configuration: configuration, events: events))
            }

            // Add entries for the last 5 minutes countdown
            if event.startTime.timeIntervalSince(currentDate) <= 300 {
                for i in 0...5 {
                    let date = event.startTime.addingTimeInterval(TimeInterval(-60 * Double(i)))
                    if date > currentDate {
                        entries.append(SimpleEntry(date: date, configuration: configuration, events: events))
                    }
                }
            }

            // Add entry at event start time
            entries.append(SimpleEntry(date: event.startTime, configuration: configuration, events: events))

            // Add entry 5 minutes after event start time
            let fiveMinutesAfter = event.startTime.addingTimeInterval(300)
            entries.append(SimpleEntry(date: fiveMinutesAfter, configuration: configuration, events: events))
        }

        // Add an entry for midnight to ensure a refresh for the next day
        entries.append(SimpleEntry(date: midnight, configuration: configuration, events: events))

        // Sort entries by date
        entries.sort { $0.date < $1.date }

        return Timeline(entries: entries, policy: .atEnd)
        }

    func recommendations() -> [AppIntentRecommendation<ConfigurationIntent>] {
        return [AppIntentRecommendation(intent: ConfigurationIntent(), description: "Komende lessen")]
    }

    private func loadEvents() -> [Event] {
            let sharedDefaults = UserDefaults(suiteName: "group.DUGUWCFH8P.dev.harrydekat.discipulus")
            var events: [Event] = []

            if let eventArray = sharedDefaults!.array(forKey: "events") as? [[String: Any]] {

                events = eventArray.compactMap { eventData -> Event? in
                            guard let id = eventData["id"] as? Int,
                                  let name = eventData["name"] as? String,
                                  let isCompleted = eventData["isCompleted"] as? Bool,
                                  let startTimeMillis = eventData["startTime"] as? Int64,
                                  let endTimeMillis = eventData["endTime"] as? Int64,
                                  let infoType = eventData["infoType"] as? Int else {
                                print("Failed at parsing the event: \(eventData)")
                                return nil
                            }

                            let startTime = Date(timeIntervalSince1970: TimeInterval(startTimeMillis) / 1000.0)
                            let endTime = Date(timeIntervalSince1970: TimeInterval(endTimeMillis) / 1000.0)

                            let shortName = eventData["shortName"] as? String
                            let location = eventData["location"] as? String
                            let startHourIndicator = eventData["startHourIndicator"] as? Int
                            let endHourIndicator = eventData["endHourIndicator"] as? Int

                            return Event(
                                id: id,
                                name: name,
                                shortName: shortName,
                                location: location,
                                infoType: infoType,
                                startHourIndicator: startHourIndicator,
                                endHourIndicator: endHourIndicator,
                                startTime: startTime,
                                endTime: endTime,
                                isCompleted: isCompleted
                            )
                        }
            }

            return events

    }
}

struct SnapshotProvider: TimelineProvider {
    let kind: SnapshotWidgetKind

    func placeholder(in context: Context) -> SnapshotEntry {
        SnapshotEntry(
            date: Date(),
            kind: kind,
            grades: [],
            average: nil,
            messages: [],
            unreadMessages: 0
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(loadSnapshot())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        let entry = loadSnapshot()
        completion(
            Timeline(
                entries: [entry],
                policy: .after(Date().addingTimeInterval(30 * 60))
            )
        )
    }

    private func loadSnapshot() -> SnapshotEntry {
        let sharedDefaults = UserDefaults(suiteName: "group.DUGUWCFH8P.dev.harrydekat.discipulus")
        let grades = (sharedDefaults?.array(forKey: "grades") ?? []).compactMap { item -> GradeSnapshot? in
            guard let item = item as? [String: Any] else { return nil }
            return GradeSnapshot(
                subject: (item["subject"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Vak",
                grade: (item["grade"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "-"
            )
        }
        let messages = (sharedDefaults?.array(forKey: "messages") ?? []).compactMap { item -> MessageSnapshot? in
            guard let item = item as? [String: Any] else { return nil }
            return MessageSnapshot(
                sender: (item["sender"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Bericht",
                subject: (item["subject"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Zonder onderwerp",
                isRead: item["read"] as? Bool ?? true
            )
        }
        let average = sharedDefaults?.string(forKey: "grades_average")?.isEmpty == false
            ? sharedDefaults?.string(forKey: "grades_average")
            : nil
        let unreadMessages = Int(sharedDefaults?.string(forKey: "messages_unread") ?? "") ?? 0

        return SnapshotEntry(
            date: Date(),
            kind: kind,
            grades: grades,
            average: average,
            messages: messages,
            unreadMessages: unreadMessages
        )
    }
}
