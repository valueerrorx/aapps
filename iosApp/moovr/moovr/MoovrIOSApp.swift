import SwiftUI

// moovr (Clumsy Cow): Phaser-Spiel aus apps/moovr/src/main/assets in der Spiel-Huelle (WebGame.swift), gratis, ohne Kauf
@main
struct MoovrIOSApp: App {
    @UIApplicationDelegateAdaptor(ShotOrientation.self) private var orientation

    var body: some SwiftUI.Scene {
        WindowGroup {
            ZStack {
                Color.black.ignoresSafeArea()
                if let scene = Self.scene {
                    // Screenshots: Spiel fuellt den ganzen Bildschirm (wie Android), ohne Balken
                    WebGameView(game: "moovr", sceneScript: scene.action, scenePrelude: Self.highscore)
                        .ignoresSafeArea()
                } else {
                    // Spiel nicht unter Notch/Dynamic Island, unten bis zum Rand
                    WebGameView(game: "moovr", sceneScript: nil)
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

    // Screenshots: Rekord im Menue statt "null meters"
    private static let highscore = "localStorage.setItem('distance', '742');"

    // nur Debug: -aappsScene MENU|PLAY|RUN|SUPER|RESULT (Store-Screenshots); action = JS nach dem Menue
    static let scene: (name: String, action: String)? = {
        #if DEBUG
        let name = UserDefaults.standard.string(forKey: "aappsScene")?.uppercased() ?? ""
        // Fahrt starten und bei <m> Metern anhalten (Spielzeit, unabhaengig vom Tempo des Simulators)
        func ride(_ m: Int) -> String {
            "fadeOut('0'); var aappsT = setInterval(function () { if (game.state.current === '0' && distance >= \(m)) { game.paused = true; clearInterval(aappsT); } }, 20);"
        }
        switch name {
        case "MENU": return (name, "")
        case "PLAY": return (name, ride(8))
        case "RUN": return (name, ride(19))
        case "SUPER": return (name, ride(57))
        // Ergebnis einer Fahrt
        case "RESULT": return (name, "fadeOut('0'); aappsWhen('0', function () { gamestate = 'lost'; distance = 286; fadeOut('finish'); });")
        default: return nil
        }
        #else
        return nil
        #endif
    }()
}

// Screenshots: App hochkant halten, das Spiel dreht WebGame.swift selbst ins Querformat.
// iPad: Debug-Info.plist erlaubt dafuer Hochformat (project.yml), sonst skaliert iPadOS die Querszene auf 3/4.
final class ShotOrientation: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        MoovrIOSApp.scene == nil ? .landscape : .portrait
    }
}
