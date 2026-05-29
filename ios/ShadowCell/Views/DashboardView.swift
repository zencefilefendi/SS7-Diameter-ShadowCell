import SwiftUI
import Combine

struct DashboardView: View {
    @StateObject private var vm = DashboardViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                riskGauge
                    .padding(.vertical, 32)

                Divider()

                eventList
            }
            .navigationTitle("ShadowCell")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Export") { vm.exportEvidence() }
                }
            }
        }
        .onAppear { vm.startMonitoring() }
        .onDisappear { vm.stopMonitoring() }
    }

    private var riskGauge: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .strokeBorder(riskColor, lineWidth: 12)
                    .frame(width: 160, height: 160)

                VStack(spacing: 4) {
                    Text("\(vm.snapshot.totalScore)")
                        .font(.system(size: 52, weight: .bold, design: .rounded))
                        .foregroundColor(riskColor)
                    Text(vm.snapshot.level.label)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Text("Son 5 dakika · \(vm.snapshot.contributingEvents.count) olay")
                .font(.caption2)
                .foregroundColor(.secondary)

            if vm.snapshot.level >= .high {
                Label("Şüpheli aktivite tespit edildi", systemImage: "exclamationmark.triangle.fill")
                    .foregroundColor(riskColor)
                    .font(.caption)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(riskColor.opacity(0.1))
                    .cornerRadius(8)
            }
        }
    }

    private var eventList: some View {
        List(vm.recentEvents) { event in
            EventRow(event: event)
        }
        .listStyle(.plain)
    }

    private var riskColor: Color {
        switch vm.snapshot.level {
        case .safe:     return .green
        case .medium:   return .yellow
        case .high:     return .orange
        case .critical: return .red
        }
    }
}

struct EventRow: View {
    let event: ThreatEvent

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            scoreChip

            VStack(alignment: .leading, spacing: 2) {
                Text(event.type.rawValue.replacingOccurrences(of: "_", with: " "))
                    .font(.caption)
                    .fontWeight(.semibold)
                Text(event.rawValue)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                Text(event.timestamp.formatted(.relative(presentation: .named)))
                    .font(.caption2)
                    .foregroundColor(.tertiary)
            }
        }
        .padding(.vertical, 4)
    }

    private var scoreChip: some View {
        Text("\(event.score)")
            .font(.caption2.bold())
            .foregroundColor(.white)
            .frame(width: 32, height: 32)
            .background(scoreColor)
            .clipShape(Circle())
    }

    private var scoreColor: Color {
        switch event.score {
        case 0...30:  return .green
        case 31...65: return .orange
        default:      return .red
        }
    }
}

class DashboardViewModel: ObservableObject {
    @Published var snapshot: RiskSnapshot = .safe
    @Published var recentEvents: [ThreatEvent] = []

    private let networkMonitor = NetworkMonitor()
    private var cancellables = Set<AnyCancellable>()
    private var eventBuffer: [ThreatEvent] = []
    private let scorer = AnomalyScorer()

    func startMonitoring() {
        networkMonitor.eventPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] event in
                self?.handleEvent(event)
            }
            .store(in: &cancellables)

        networkMonitor.start()
    }

    func stopMonitoring() {
        networkMonitor.stop()
        cancellables.removeAll()
    }

    private func handleEvent(_ event: ThreatEvent) {
        eventBuffer.insert(event, at: 0)
        if eventBuffer.count > 100 { eventBuffer.removeLast() }
        recentEvents = Array(eventBuffer.prefix(50))
        snapshot = scorer.ingest(event)
    }

    func exportEvidence() {
        // TODO: Implement evidence export (Phase 5)
    }
}
