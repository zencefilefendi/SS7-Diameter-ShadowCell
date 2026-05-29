import Foundation

/// iOS temporal korelasyon motoru (Android AnomalyScorer.kt'nin Swift portu)
class AnomalyScorer {

    private var eventBuffer: [ThreatEvent] = []
    private let shortWindow: TimeInterval = 60    // 60 saniye
    private let longWindow: TimeInterval = 300    // 5 dakika

    func ingest(_ event: ThreatEvent) -> RiskSnapshot {
        eventBuffer.append(event)
        pruneOldEvents()
        return computeSnapshot()
    }

    func currentRisk() -> RiskSnapshot {
        pruneOldEvents()
        return computeSnapshot()
    }

    private func computeSnapshot() -> RiskSnapshot {
        let now = Date()
        let windowEvents = eventBuffer.filter { now.timeIntervalSince($0.timestamp) <= longWindow }

        guard !windowEvents.isEmpty else { return .safe }

        var score = windowEvents.map(\.score).max() ?? 0

        let shortEvents = windowEvents.filter { now.timeIntervalSince($0.timestamp) <= shortWindow }
        if shortEvents.count >= 2 {
            let shortBonus = Int(Double(shortEvents.map(\.score).reduce(0, +)) * 0.4)
            score += shortBonus
        }

        score += comboBonus(long: windowEvents, short: shortEvents)
        score = min(score, 100)

        return RiskSnapshot(
            timestamp: now,
            totalScore: score,
            level: score.toRiskLevel,
            contributingEvents: windowEvents
        )
    }

    private func comboBonus(long: [ThreatEvent], short: [ThreatEvent]) -> Int {
        var bonus = 0
        let types = Set(short.map(\.type))

        if types.contains(.networkDowngrade) && types.contains(.silentSmsCandidate) { bonus += 25 }
        if types.contains(.networkDowngrade) && types.contains(.locationUpdateBurst) { bonus += 20 }
        if types.contains(.cellTowerAnomaly) && types.contains(.signalDropHandoff) { bonus += 20 }

        let hasAll = types.contains(.networkDowngrade) && types.contains(.silentSmsCandidate) && types.contains(.cellTowerAnomaly)
        if hasAll { bonus += 15 }

        return bonus
    }

    private func pruneOldEvents() {
        let cutoff = Date().addingTimeInterval(-longWindow)
        eventBuffer.removeAll { $0.timestamp < cutoff }
    }
}
