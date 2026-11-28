import SwiftUI
import UIKit
import WebKit

// Huelle fuer das Phaser-Spiel: vollflaechige WKWebView, Assets nur aus dem App-Bundle (wie Android MainActivity)
struct WebGameView: UIViewControllerRepresentable {
    // Praefix der JS-Bruecke (moovrPause/moovrResume)
    let game: String
    // nur Screenshots: JS, das nach dem Menue einen Zustand herstellt ("" = Menue)
    let sceneScript: String?
    // nur Screenshots: JS vor dem Spiel (z. B. Spielstand in localStorage)
    var scenePrelude = ""

    func makeUIViewController(context: Context) -> WebGameController {
        WebGameController(game: game, sceneScript: sceneScript, scenePrelude: scenePrelude)
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
    private let scenePrelude: String
    private var webView: WKWebView!
    private var lastCrash: Date?

    init(game: String, sceneScript: String?, scenePrelude: String) {
        self.game = game
        self.sceneScript = sceneScript
        self.scenePrelude = scenePrelude
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
        if sceneScript != nil {
            // iPad: Desktop-Modus nimmt die Bildschirmbreite (hochkant) als device-width, das Spiel waere zu klein
            config.defaultWebpagePreferences.preferredContentMode = .mobile
            scripts.addUserScript(WKUserScript(source: Self.shotSetup + scenePrelude, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }

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
        if sceneScript == nil {
            view = web
        } else {
            // Screenshots: Spiel in eigener Huelle, quer hineingedreht in viewDidLayoutSubviews
            let box = UIView()
            box.backgroundColor = .black
            box.addSubview(web)
            view = box
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(pause), name: UIApplication.willResignActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(resume), name: UIApplication.didBecomeActiveNotification, object: nil)
        if sceneScript == nil { start() }
    }

    // Screenshots: App bleibt hochkant (AppDelegate), das Spiel wird quer hineingedreht und fuellt den ganzen Bildschirm.
    // Kein Warten auf eine Drehung des Simulators; sim-shots.sh dreht das Foto zurueck (rotate.txt = 270).
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        guard sceneScript != nil else { return }
        let b = view.bounds
        guard b.width > 0, b.height > 0 else { return }
        let turn = b.height > b.width
        webView.transform = .identity
        webView.bounds = CGRect(x: 0, y: 0, width: turn ? b.height : b.width, height: turn ? b.width : b.height)
        webView.center = CGPoint(x: b.midX, y: b.midY)
        webView.transform = turn ? CGAffineTransform(rotationAngle: .pi / 2) : .identity
        if !started { start() }
    }

    private var started = false

    private func start() {
        started = true
        // Screenshots: Viewport fest auf die (quer gedrehte) WebView-Groesse statt device-width
        if sceneScript != nil {
            let w = Int(webView.bounds.width), h = Int(webView.bounds.height)
            let fix = """
            new MutationObserver(function (l, o) {
                var m = document.querySelector('meta[name=viewport]');
                if (m) { m.setAttribute('content', 'width=\(w), height=\(h), initial-scale=1, maximum-scale=1, user-scalable=no'); o.disconnect(); }
            }).observe(document, { childList: true, subtree: true });
            """
            webView.configuration.userContentController.addUserScript(WKUserScript(source: fix, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        webView.load(URLRequest(url: BundleAssets.start))
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIApplication.shared.isIdleTimerDisabled = true
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

    // Screenshots: Seite meldet Querformat, auch wenn die App hochkant laeuft (Phaser forceLandscape);
    // aappsWhen(state, fn) = fn, sobald der Spielzustand aktiv und die Blende fertig ist
    private static let shotSetup = """
    window.aappsWhen = function (state, fn) {
        (function go() {
            var fading = typeof cover !== 'undefined' && cover && cover.game;
            if (typeof game !== 'undefined' && game && game.isBooted && game.state.current === String(state) && !fading) { fn(); }
            else { setTimeout(go, 100); }
        })();
    };
    (function () {
        var o = { type: 'landscape-primary', angle: 90, addEventListener: function () {}, removeEventListener: function () {} };
        try { Object.defineProperty(Screen.prototype, 'orientation', { configurable: true, get: function () { return o; } }); } catch (e) {}
        try { Object.defineProperty(window, 'orientation', { configurable: true, get: function () { return 90; } }); } catch (e) {}
    })();
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
