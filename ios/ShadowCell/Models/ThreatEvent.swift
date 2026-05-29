import Foundation

enum ThreatEventType: String, Codable {
    case networkDowngrade       = "NETWORK_DOWNGRADE"
    case networkUpgradeFast     = "NETWORK_UPGRADE_FAST"
    case silentSmsCandidate     = "SILENT_SMS_CANDIDATE"
    case cellTowerAnomaly       = "CELL_TOWER_ANOMALY"
    case locationUpdateBurst    = "LOCATION_UPDATE_BURST"
    case signalDropHandoff      = "SIGNAL_DROP_HANDOFF"
    case pagingAnomaly          = "PAGING_ANOMALY"
}

enum RiskLevel: Int, Comparable, Codable {
    case safe     = 0
    case medium   = 1
    case high     = 2
    case critical = 3

    static func < (lhs: RiskLevel, rhs: RiskLevel) -> Bool { lhs.rawValue < rhs.rawValue }

    var label: String {
        switch self {
        case .safe:     return "GÜVENLI"
        case .medium:   return "ORTA"
        case .high:     return "YÜKSEK"
        case .critical: return "KRİTİK"
        }
    }

    var color: String {  // SwiftUI'da Color olarak kullanılacak
        switch self {
        case .safe:     return "green"
        case .medium:   return "yellow"
        case .high:     return "orange"
        case .critical: return "red"
        }
    }
}

struct ThreatEvent: Identifiable, Codable {
    let id: UUID
    let timestamp: Date
    let type: ThreatEventType
    let rawValue: String
    let score: Int
    let context: String

    init(
        id: UUID = UUID(),
        timestamp: Date = Date(),
        type: ThreatEventType,
        rawValue: String,
        score: Int,
        context: String = ""
    ) {
        self.id = id
        self.timestamp = timestamp
        self.type = type
        self.rawValue = rawValue
        self.score = score
        self.context = context
    }
}

struct RiskSnapshot {
    let timestamp: Date
    let totalScore: Int
    let level: RiskLevel
    let contributingEvents: [ThreatEvent]

    static var safe: RiskSnapshot {
        RiskSnapshot(timestamp: Date(), totalScore: 0, level: .safe, contributingEvents: [])
    }
}

extension Int {
    var toRiskLevel: RiskLevel {
        switch self {
        case 0...30:  return .safe
        case 31...65: return .medium
        case 66...85: return .high
        default:      return .critical
        }
    }
}
