import SwiftUI

// glidr (Cool Cows on Ice): Phaser-Spiel aus apps/glidr/src/main/assets in der Spiel-Huelle (WebGame.swift), gratis, ohne Kauf
@main
struct GlidrIOSApp: App {
    @UIApplicationDelegateAdaptor(ShotOrientation.self) private var orientation

    var body: some SwiftUI.Scene {
        WindowGroup {
            ZStack {
                Color.black.ignoresSafeArea()
                if let scene = Self.scene {
                    // Screenshots: Spiel fuellt den ganzen Bildschirm (wie Android), ohne Balken
                    WebGameView(game: "glidr", sceneScript: scene.action, scenePrelude: Self.progress)
                        .ignoresSafeArea()
                } else {
                    // Spiel nicht unter Notch/Dynamic Island, unten bis zum Rand
                    WebGameView(game: "glidr", sceneScript: nil)
                        // iPad: Spielflaeche 16:10 (768x480) einpassen statt strecken, Rest schwarz; iPhone unveraendert
                        .aspectRatio(Self.isPad ? 768.0 / 480.0 : nil, contentMode: .fit)
                        .ignoresSafeArea(edges: .vertical)
                }
            }
            .statusBarHidden()
            .persistentSystemOverlays(.hidden)
            .defersSystemGestures(on: .all)
            .preferredColorScheme(.dark)
        }
    }

    private static let isPad = UIDevice.current.userInterfaceIdiom == .pad

    // Screenshots: Spielstand mit Sternen in den ersten Welten
    private static let progress = """
    [3, 4, 3, 4, 2, 4, 3, 3, 4, 2, 3, 4, 3, 2, 4, 3, 1, 3].forEach(function (v, i) { localStorage.setItem(String(i), String(v)); });
    """

    // nur Debug: -aappsScene MENU|LEVEL11|LEVEL16|LEVEL23|CLEARED (Store-Screenshots); action = JS nach dem Menue
    static let scene: (name: String, action: String)? = {
        #if DEBUG
        let name = UserDefaults.standard.string(forKey: "aappsScene")?.uppercased() ?? ""
        func level(_ n: Int) -> String { "music.stop(); level = \(n); fadeOut(\(n));" }
        switch name {
        case "MENU": return (name, "")
        case "LEVEL11": return (name, level(11))
        case "LEVEL16": return (name, level(16))
        case "LEVEL23": return (name, level(23))
        // Levelende mit plausiblen Werten
        case "CLEARED": return (name, level(16) + """
             aappsWhen(16, function () {
                gamestate = 'running'; totalAssets = 4; cowsInLevel = 1; cow_initial_health = 3; cloversCollected = 2;
                totalAssetsDestroyed = 0; totalHits = 3; totalHealthwasted = 0; fadeOut('finish');
            });
            """)
        default: return nil
        }
        #else
        return nil
        #endif
    }()
}

// Screenshots: iPhone hochkant halten, das Spiel dreht WebGame.swift selbst ins Querformat.
// iPad nicht: dort bleibt die App im Simulator ohnehin hochkant, erzwungenes Hochformat gibt nur ein kleines Fenster.
final class ShotOrientation: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        GlidrIOSApp.scene == nil || UIDevice.current.userInterfaceIdiom == .pad ? .landscape : .portrait
    }
}
