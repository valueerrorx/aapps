import AVFoundation
import UIKit
import AappsKit

/// Kamera (QR lesen) und Teilen fuer die Kotlin-Seite (DividrHost in apps/dividr/shared/iosMain).
/// Kotlin ruft vom Main-Thread; Rueckrufe gehen ebenfalls auf dem Main-Thread zurueck.
final class DividrHostImpl: NSObject, DividrHost {
    func cameraStatus() -> Int32 {
        if AVCaptureDevice.default(for: .video) == nil { return 3 }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return 1
        case .notDetermined: return 0
        default: return 2
        }
    }

    func requestCamera(done: @escaping () -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { _ in
            DispatchQueue.main.async { done() }
        }
    }

    func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    func makeScanner(onText: @escaping (String) -> Void, onError: @escaping () -> Void) -> UIView {
        ScannerView(onText: onText, onError: onError)
    }

    func stopScanner(view: UIView) {
        (view as? ScannerView)?.stop()
    }

    func share(text: String) {
        guard let top = Self.topController() else { return }
        let vc = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        // iPad: Popover braucht einen Anker
        if let pop = vc.popoverPresentationController {
            pop.sourceView = top.view
            pop.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.midY, width: 0, height: 0)
            pop.permittedArrowDirections = []
        }
        top.present(vc, animated: true)
    }

    private static func topController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}

/// Kamera-Vorschau mit QR-Erkennung (AVCaptureMetadataOutput); Session laeuft auf eigener Queue
final class ScannerView: UIView, AVCaptureMetadataOutputObjectsDelegate {
    private let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "dividr.camera")
    private let preview: AVCaptureVideoPreviewLayer
    private let onText: (String) -> Void
    private let onError: () -> Void
    private var last: String?

    init(onText: @escaping (String) -> Void, onError: @escaping () -> Void) {
        self.onText = onText
        self.onError = onError
        preview = AVCaptureVideoPreviewLayer(session: session)
        super.init(frame: .zero)
        backgroundColor = .black
        preview.videoGravity = .resizeAspectFill
        layer.addSublayer(preview)
        configure()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) nicht genutzt") }

    private func configure() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else {
            DispatchQueue.main.async { self.onError() }
            return
        }
        session.beginConfiguration()
        // hohe Aufloesung: dichte Codes bis Version 40
        if session.canSetSessionPreset(.hd1920x1080) { session.sessionPreset = .hd1920x1080 }
        let output = AVCaptureMetadataOutput()
        guard session.canAddInput(input), session.canAddOutput(output) else {
            session.commitConfiguration()
            DispatchQueue.main.async { self.onError() }
            return
        }
        session.addInput(input)
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        if output.availableMetadataObjectTypes.contains(.qr) { output.metadataObjectTypes = [.qr] }
        session.commitConfiguration()
        if device.isFocusModeSupported(.continuousAutoFocus), (try? device.lockForConfiguration()) != nil {
            device.focusMode = .continuousAutoFocus
            device.unlockForConfiguration()
        }
        let s = session
        queue.async { s.startRunning() }
    }

    func stop() {
        let s = session
        queue.async { s.stopRunning() }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        preview.frame = bounds
        // Vorschau folgt der Oberflaeche
        if let connection = preview.connection, let scene = window?.windowScene {
            let angle: CGFloat
            switch scene.interfaceOrientation {
            case .landscapeLeft: angle = 180
            case .landscapeRight: angle = 0
            case .portraitUpsideDown: angle = 270
            default: angle = 90
            }
            if connection.isVideoRotationAngleSupported(angle) { connection.videoRotationAngle = angle }
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        for case let code as AVMetadataMachineReadableCodeObject in metadataObjects {
            guard let text = code.stringValue else { continue }
            // gleicher Code laufend: nur einmal melden
            if text == last { continue }
            last = text
            onText(text)
        }
    }
}
