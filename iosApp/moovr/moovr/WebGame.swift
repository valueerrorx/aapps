import SwiftUI
import UIKit
import WebKit

// Huelle fuer das Phaser-Spiel: vollflaechige WKWebView, Assets nur aus dem App-Bundle (wie Android MainActivity)
struct WebGameView: UIViewControllerRepresentable {
    // Praefix der JS-Bruecke (moovrPause/moovrResume)
    let game: String
    // nur Screenshots: JS, das nach dem Menue einen Zustand herstellt ("" = Menue)
    let sceneScript: String?

    func makeUIViewController(context: Context) -> WebGameController {
        WebGameController(game: game, sceneScript: sceneScript)
    }

    func updateUIViewController(_ controller: WebGameController, context: Context) {}
}

// Hauptmenue abwarten (Laden + Blende fertig), dann [action] ausfuehren
func afterMenu(_ action: String) -> String {
    """
    (function go() {
        var fading = typeof cover !== 'undefined' && cover && cover.game;
        if (typeof game !== 'undefined' && game && game.isBooted && game.state.current === 'menu' && !fading) {
            setTimeout(function () { \(action) }, 300);
        } else {
            setTimeout(go, 200);
        }
    })();
    """
}

final class WebGameController: UIViewController, WKNavigationDelegate, WKUIDelegate {
    private let game: String
    private let sceneScript: String?
    private var webView: WKWebView!
    private var lastCrash: Date?

    init(game: String, sceneScript: String?) {
        self.game = game
        self.sceneScript = sceneScript
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { nil }

    override func loadView() {
        let config = WKWebViewConfiguration()
        config.setURLSchemeHandler(BundleAssets(), forURLScheme: BundleAssets.scheme)
        config.websiteDataStore = .default()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.dataDetectorTypes = []
        config.preferences.javaScriptCanOpenWindowsAutomatically = false
        config.preferences.isFraudulentWebsiteWarningEnabled = false
        let scripts = config.userContentController
        scripts.addUserScript(WKUserScript(source: Self.oggToM4a, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        scripts.addUserScript(WKUserScript(source: Self.noSelection, injectionTime: .atDocumentEnd, forMainFrameOnly: true))

        let web = WKWebView(frame: .zero, configuration: config)
        web.isOpaque = false
        web.backgroundColor = .black
        web.scrollView.backgroundColor = .black
        web.scrollView.isScrollEnabled = false
        web.scrollView.bounces = false
        web.scrollView.contentInsetAdjustmentBehavior = .never
        web.allowsBackForwardNavigationGestures = false
        web.allowsLinkPreview = false
        web.navigationDelegate = self
        web.uiDelegate = self
        #if DEBUG
        if #available(iOS 16.4, *) { web.isInspectable = true }
        #endif
        webView = web
        view = web
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(pause), name: UIApplication.willResignActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(resume), name: UIApplication.didBecomeActiveNotification, object: nil)
        webView.load(URLRequest(url: BundleAssets.start))
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIApplication.shared.isIdleTimerDisabled = true
        // Screenshots: immer dieselbe Querlage, damit das Bild gerade gedreht werden kann
        if sceneScript != nil, let scene = view.window?.windowScene {
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeRight))
        }
    }

    // App im Hintergrund: Spielzeit und Audio anhalten (wie Android onPause)
    @objc private func pause() {
        UIApplication.shared.isIdleTimerDisabled = false
        webView.evaluateJavaScript("window.\(game)Pause && \(game)Pause()")
    }

    @objc private func resume() {
        UIApplication.shared.isIdleTimerDisabled = true
        webView.evaluateJavaScript("window.\(game)Resume && \(game)Resume()")
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        if let sceneScript, !sceneScript.isEmpty { webView.evaluateJavaScript(afterMenu(sceneScript)) }
    }

    // nur eigene Assets laden; fremde http(s)-Links nur nach Tipp im Browser
    func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = action.request.url else { return decisionHandler(.cancel) }
        if BundleAssets.isOwn(url) { return decisionHandler(.allow) }
        openExternal(url, action)
        decisionHandler(.cancel)
    }

    // keine weiteren Fenster
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let url = action.request.url { openExternal(url, action) }
        return nil
    }

    private func openExternal(_ url: URL, _ action: WKNavigationAction) {
        let web = ["http", "https"].contains(url.scheme?.lowercased() ?? "") && !(url.host ?? "").isEmpty
        if web && action.navigationType == .linkActivated { UIApplication.shared.open(url) }
    }

    // Web-Prozess vom System beendet: einmal pro 30 s neu laden, keine Schleife
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        let now = Date()
        if let lastCrash, now.timeIntervalSince(lastCrash) < 30 { return }
        lastCrash = now
        webView.load(URLRequest(url: BundleAssets.start))
    }

    // WebKit auf iOS spielt kein Ogg Vorbis: dieselben Klaenge als AAC (.m4a) laden, game.js bleibt unveraendert
    private static let oggToM4a = """
    document.addEventListener('DOMContentLoaded', function () {
        var L = window.Phaser && Phaser.Loader && Phaser.Loader.prototype;
        if (!L || !L.audio) return;
        var audio = L.audio;
        L.audio = function (key, urls, autoDecode) {
            if (typeof urls === 'string') urls = urls.replace(/\\.ogg$/, '.m4a');
            return audio.call(this, key, urls, autoDecode);
        };
    }, true);
    """

    // kein Markieren, keine Lupe, kein Kontextmenue beim Halten
    private static let noSelection = """
    (function () {
        var s = document.createElement('style');
        s.textContent = '*{-webkit-user-select:none;user-select:none;-webkit-touch-callout:none;-webkit-tap-highlight-color:transparent;}';
        document.head.appendChild(s);
    })();
    """
}

// Liefert Dateien aus dem Bundle-Ordner "assets" (Spiel wie Android) und "ios-assets" (iOS-Ergaenzungen)
final class BundleAssets: NSObject, WKURLSchemeHandler {
    static let scheme = "aapps"
    static let host = "game"
    static let start = URL(string: "\(scheme)://\(host)/index.html")!

    private let roots: [URL] = ["assets", "ios-assets"].compactMap {
        Bundle.main.resourceURL?.appendingPathComponent($0, isDirectory: true).standardizedFileURL
    }

    static func isOwn(_ url: URL) -> Bool {
        url.scheme?.lowercased() == scheme && url.host?.lowercased() == host && url.user == nil && url.port == nil
    }

    func webView(_ webView: WKWebView, start task: WKURLSchemeTask) {
        guard let url = task.request.url, Self.isOwn(url) else {
            return reply(task, status: 403, data: Data(), type: "text/plain")
        }
        guard let file = resolve(url.path), let data = try? Data(contentsOf: file) else {
            return reply(task, status: 404, data: Data(), type: "text/plain")
        }
        reply(task, status: 200, data: data, type: Self.mime(file.pathExtension))
    }

    func webView(_ webView: WKWebView, stop task: WKURLSchemeTask) {}

    // nur Dateien innerhalb der Wurzeln, keine Pfad-Tricks
    private func resolve(_ path: String) -> URL? {
        if path.contains("..") || path.lowercased().contains("%2e") { return nil }
        let rel = path.hasPrefix("/") ? String(path.dropFirst()) : path
        for root in roots {
            let file = root.appendingPathComponent(rel).standardizedFileURL
            var dir: ObjCBool = false
            if file.path.hasPrefix(root.path + "/"),
               FileManager.default.fileExists(atPath: file.path, isDirectory: &dir), !dir.boolValue {
                return file
            }
        }
        return nil
    }

    private func reply(_ task: WKURLSchemeTask, status: Int, data: Data, type: String) {
        let headers = ["Content-Type": type, "Content-Length": String(data.count), "Cache-Control": "no-store"]
        guard let url = task.request.url,
              let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: "HTTP/1.1", headerFields: headers) else {
            return task.didFailWithError(URLError(.badURL))
        }
        task.didReceive(response)
        task.didReceive(data)
        task.didFinish()
    }

    private static func mime(_ ext: String) -> String {
        switch ext.lowercased() {
        case "html": return "text/html; charset=utf-8"
        case "js": return "text/javascript; charset=utf-8"
        case "json", "jsona": return "application/json"
        case "xml": return "application/xml"
        case "png": return "image/png"
        case "jpg", "jpeg": return "image/jpeg"
        case "m4a": return "audio/mp4"
        case "ogg": return "audio/ogg"
        case "txt": return "text/plain; charset=utf-8"
        default: return "application/octet-stream"
        }
    }
}
