import Foundation
import CoreTelephony
import Network
import Combine

/// iOS ağ tipi değişimlerini izler.
/// CTTelephony iOS 16.4'ten itibaren kısıtlandı; ancak temel network type değişimi hâlâ erişilebilir.
class NetworkMonitor: ObservableObject {

    private let networkInfo = CTTelephonyNetworkInfo()
    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.shadowcell.network")

    let eventPublisher = PassthroughSubject<ThreatEvent, Never>()

    private var lastRadioTech: String? = nil
    private var transitionHistory: [(date: Date, tech: String)] = []

    func start() {
        // Radio technology değişimlerini izle
        networkInfo.serviceSubscriberCellularProvidersDidUpdateNotifier = { [weak self] serviceId in
            self?.handleServiceUpdate(serviceId: serviceId)
        }

        // Genel ağ path değişimleri
        pathMonitor.pathUpdateHandler = { [weak self] path in
            self?.handlePathUpdate(path: path)
        }
        pathMonitor.start(queue: monitorQueue)

        // Baseline kur
        if let currentTech = currentRadioTechnology() {
            lastRadioTech = currentTech
        }
    }

    func stop() {
        pathMonitor.cancel()
        networkInfo.serviceSubscriberCellularProvidersDidUpdateNotifier = nil
    }

    private func handleServiceUpdate(serviceId: String) {
        guard let newTech = currentRadioTechnology() else { return }
        let now = Date()

        if let lastTech = lastRadioTech, newTech != lastTech {
            let lastGen = generation(of: lastTech)
            let newGen = generation(of: newTech)

            transitionHistory.append((date: now, tech: newTech))
            // Sadece son 10 geçişi tut
            if transitionHistory.count > 10 { transitionHistory.removeFirst() }

            if newGen < lastGen {
                let score = (lastGen == 4 && newGen == 3) ? 40 :
                            (lastGen == 4 && newGen == 2) ? 55 :
                            (lastGen == 3 && newGen == 2) ? 30 : 20

                let event = ThreatEvent(
                    type: .networkDowngrade,
                    rawValue: "\(lastGen)G→\(newGen)G (\(lastTech)→\(newTech))",
                    score: score,
                    context: "serviceId=\(serviceId)"
                )
                DispatchQueue.main.async {
                    self.eventPublisher.send(event)
                }
            } else if newGen > lastGen && wasRecentDowngrade(before: now) {
                let event = ThreatEvent(
                    type: .networkUpgradeFast,
                    rawValue: "Fast return \(lastGen)G→\(newGen)G",
                    score: 25,
                    context: "downgrade+upgrade within 120s"
                )
                DispatchQueue.main.async {
                    self.eventPublisher.send(event)
                }
            }
        }
        lastRadioTech = newTech
    }

    private func handlePathUpdate(path: NWPath) {
        // Cellular path kaybı + geri kazanımı da izlenebilir
        // TODO: path.availableInterfaces ile daha detaylı analiz
    }

    private func wasRecentDowngrade(before: Date) -> Bool {
        let cutoff = before.addingTimeInterval(-120)
        return transitionHistory.contains { $0.date > cutoff && generation(of: $0.tech) <= 3 }
    }

    private func currentRadioTechnology() -> String? {
        guard let services = networkInfo.serviceCurrentRadioAccessTechnology else { return nil }
        return services.values.first
    }

    private func generation(of tech: String) -> Int {
        if tech.contains("LTE") || tech.contains("4G") { return 4 }
        if tech.contains("5G") || tech.contains("NR") { return 5 }
        if tech.contains("WCDMA") || tech.contains("HSDPA") ||
           tech.contains("HSUPA") || tech.contains("HSPA") ||
           tech.contains("CDMA2000") || tech.contains("EVDO") { return 3 }
        if tech.contains("GPRS") || tech.contains("EDGE") ||
           tech.contains("CDMA") || tech.contains("1xRTT") { return 2 }
        return 0
    }
}
