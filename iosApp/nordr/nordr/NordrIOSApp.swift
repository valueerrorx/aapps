import SwiftUI
import AappsKit

// SwiftUI-Huelle; Oberflaeche und Logik kommen aus Kotlin (apps/nordr/shared)
@main
struct NordrIOSApp: App {
    private let store = StoreKitBridge(bundleID: Bundle.main.bundleIdentifier ?? "solutions.xapient.nordr")

    var body: some SwiftUI.Scene {
        WindowGroup {
            ComposeView(store: store)
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let store: StoreKitBridge

    func makeUIViewController(context: Context) -> UIViewController {
        let defaults = UserDefaults.standard
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        // nur Debug: -aappsScene HEADING (Screenshots), -aappsTrial expire|reset
        #if DEBUG
        let debugTrial = defaults.string(forKey: "aappsTrial")
        let scene = defaults.string(forKey: "aappsScene")
        #else
        let debugTrial: String? = nil
        let scene: String? = nil
        #endif
        return MainViewControllerKt.MainViewController(
            bridge: store,
            versionName: version,
            scene: scene,
            debugTrial: debugTrial
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
