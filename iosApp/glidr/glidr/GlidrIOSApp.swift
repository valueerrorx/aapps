import SwiftUI

// glidr (Cool Cows on Ice): Phaser-Spiel aus apps/glidr/src/main/assets in der Spiel-Huelle (WebGame.swift), gratis, ohne Kauf
@main
struct GlidrIOSApp: App {
    var body: some SwiftUI.Scene {
        WindowGroup {
            ZStack {
                Color.black.ignoresSafeArea()
                // Spiel nicht unter Notch/Dynamic Island, unten bis zum Rand
                WebGameView(game: "glidr", sceneScript: Self.sceneScript())
                    .ignoresSafeArea(edges: .vertical)
            }
            .statusBarHidden()
            .persistentSystemOverlays(.hidden)
            .defersSystemGestures(on: .all)
            .preferredColorScheme(.dark)
        }
    }

    // nur Debug: -aappsScene MENU|LEVEL11|LEVEL16 (Store-Screenshots)
    private static func sceneScript() -> String? {
        #if DEBUG
        switch UserDefaults.standard.string(forKey: "aappsScene")?.uppercased() {
        case "MENU": return ""
        case "LEVEL11": return "level = 11; fadeOut(11);"
        case "LEVEL16": return "level = 16; fadeOut(16);"
        default: return nil
        }
        #else
        return nil
        #endif
    }
}
