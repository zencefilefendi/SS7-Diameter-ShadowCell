import SwiftUI
import BackgroundTasks
import CoreLocation

@main
struct ShadowCellApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    var body: some Scene {
        WindowGroup {
            DashboardView()
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        // Request CoreLocation permissions (Required for some network info on iOS)
        locationManager.delegate = self
        locationManager.requestAlwaysAuthorization()
        
        // Register Background Tasks
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.shadowcell.refresh", using: nil) { task in
            self.handleAppRefresh(task: task as! BGAppRefreshTask)
        }
        
        return true
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        scheduleAppRefresh()
    }
    
    func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.shadowcell.refresh")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // Fetch after 15 minutes
        
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule app refresh: \(error)")
        }
    }
    
    func handleAppRefresh(task: BGAppRefreshTask) {
        // Schedule next refresh
        scheduleAppRefresh()
        
        let networkMonitor = NetworkMonitor()
        networkMonitor.start()
        
        task.expirationHandler = {
            networkMonitor.stop()
        }
        
        // Give it a brief moment to capture radio changes if possible
        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
            networkMonitor.stop()
            task.setTaskCompleted(success: true)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedAlways || status == .authorizedWhenInUse {
            manager.startUpdatingLocation()
        }
    }
}