import XCTest

// Startet das Spiel in den Screenshot-Zustaenden und haengt Bilder an das Testergebnis
final class ScreenshotTests: XCTestCase {
    private let scenes = ["MENU", "LEVEL11", "LEVEL16", "LEVEL23", "CLEARED"]

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testLaunch() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
        XCTAssertTrue(app.webViews.firstMatch.waitForExistence(timeout: 20))
    }

    func testScenes() throws {
        for scene in scenes {
            let app = XCUIApplication()
            app.launchArguments = ["-aappsScene", scene]
            app.launch()
            XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
            // Laden, Menue-Blende und Szene abwarten
            Thread.sleep(forTimeInterval: 6)
            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            shot.name = scene.lowercased()
            shot.lifetime = .keepAlways
            add(shot)
            app.terminate()
        }
    }
}
