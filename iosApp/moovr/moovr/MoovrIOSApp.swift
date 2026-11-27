import SwiftUI

// moovr (Clumsy Cow): Phaser-Spiel aus apps/moovr/src/main/assets in der Spiel-Huelle (WebGame.swift), gratis, ohne Kauf
@main
struct MoovrIOSApp: App {
    var body: some SwiftUI.Scene {
        WindowGroup {
            ZStack {
                Color.black.ignoresSafeArea()
                // Spiel nicht unter Notch/Dynamic Island, unten bis zum Rand
                WebGameView(game: "moovr", sceneScript: Self.sceneScript())
                    .ignoresSafeArea(edges: .vertical)
            }
            .statusBarHidden()
            .persistentSystemOverlays(.hidden)
            .defersSystemGestures(on: .all)
            .preferredColorScheme(.dark)
        }
    }

    // nur Debug: -aappsScene MENU|PLAY (Store-Screenshots)
    private static func sceneScript() -> String? {
        #if DEBUG
        switch UserDefaults.standard.string(forKey: "aappsScene")?.uppercased() {
        case "MENU": return ""
        case "PLAY": return "fadeOut('0');"
        default: return nil
        }
        #else
        return nil
        #endif
    }
}
