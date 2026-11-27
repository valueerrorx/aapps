import XCTest

// Startet die App in festen Zustaenden und haengt Screenshots an das Testergebnis
final class ScreenshotTests: XCTestCase {
    private let scenes = ["WHEEL", "SPINNING", "RESULT", "MANY", "REMOVED", "WHEELS", "TEMPLATES", "COMPASS", "EDITOR", "SETTINGS", "ABOUT", "OFFER", "THANKS"]

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testLaunch() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
    }

    func testScenesEnglish() throws {
        capture(language: "en", locale: "en_US")
    }

    func testScenesGerman() throws {
        capture(language: "de", locale: "de_AT")
    }

    private func capture(language: String, locale: String) {
        for scene in scenes {
            let app = XCUIApplication()
            app.launchArguments = ["-aappsScene", scene, "-AppleLanguages", "(" + language + ")", "-AppleLocale", locale]
            app.launch()
            XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
            // Compose braucht einen Moment fuer den ersten Frame
            Thread.sleep(forTimeInterval: 2)
            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            shot.name = language + "_" + scene.lowercased()
            shot.lifetime = .keepAlways
            add(shot)
            app.terminate()
        }
    }
}
