import Foundation
import StoreKit
import AappsKit

/// StoreKit 2 fuer die Kotlin-Seite (StoreGate in core/store). Gleich fuer alle Apps:
/// Produkte <Bundle-ID>.full_unlock und <Bundle-ID>.trial (beide nicht verbrauchbar, Testphase mit Preis 0).
/// Laeuft auf dem Main Actor (Kotlin ruft vom Main-Thread), StoreKit-Abfragen sind nonisolated.
@MainActor
final class StoreKitBridge: NSObject {
    private let unlockID: String
    private let trialID: String
    private var listener: StoreListener?
    private var updates: Task<Void, Never>?
    // Abfragen nacheinander; nur die Antwort der neuesten Anfrage zaehlt
    private var refreshTask: Task<Void, Never>?
    private var refreshGeneration = 0

    init(bundleID: String) {
        unlockID = bundleID + ".full_unlock"
        trialID = bundleID + ".trial"
        super.init()
    }

    private struct Snapshot: Sendable {
        let unlocked: Bool
        let revoked: Bool
        let trialStart: Int64
        let failed: Bool
    }

    private func scheduleRefresh() {
        refreshGeneration += 1
        let generation = refreshGeneration
        let previous = refreshTask
        let unlockID = unlockID
        let trialID = trialID
        refreshTask = Task {
            await previous?.value
            let snapshot = await Self.query(unlockID: unlockID, trialID: trialID)
            guard generation == self.refreshGeneration else { return }
            if snapshot.failed {
                self.listener?.onQueryFailed()
            } else {
                self.listener?.onEntitlements(unlocked: snapshot.unlocked, revoked: snapshot.revoked, trialStartMillis: snapshot.trialStart)
            }
        }
    }

    // Transaction.currentEntitlements: nur verifizierte, nicht widerrufene Kaeufe zaehlen
    private nonisolated static func query(unlockID: String, trialID: String) async -> Snapshot {
        var unlocked = false
        var trialStart: Int64 = -1
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result, transaction.revocationDate == nil else { continue }
            if transaction.productID == unlockID {
                unlocked = true
            } else if transaction.productID == trialID {
                // erster Kauf zaehlt, auch nach Wiederherstellen
                trialStart = Int64(transaction.originalPurchaseDate.timeIntervalSince1970 * 1000)
            }
        }
        // Zuruecknehmen nur mit Beleg: letzte Freischalt-Transaktion verifiziert und widerrufen
        var revoked = false
        if !unlocked, let latest = await Transaction.latest(for: unlockID),
           case .verified(let transaction) = latest, transaction.revocationDate != nil {
            revoked = true
        }
        // Ohne App-Store-Konto ist "nichts gefunden" keine verlaessliche Antwort: dann gilt der lokale Cache
        let storefront = await Storefront.current
        let failed = !unlocked && !revoked && trialStart < 0 && storefront == nil
        return Snapshot(unlocked: unlocked, revoked: revoked, trialStart: trialStart, failed: failed)
    }

    private nonisolated static func prices(unlockID: String, trialID: String) async -> (String?, Bool) {
        let products = (try? await Product.products(for: [unlockID, trialID])) ?? []
        return (products.first(where: { $0.id == unlockID })?.displayPrice, products.contains(where: { $0.id == trialID }))
    }
}

// Kotlin ruft vom Main-Thread; @preconcurrency prueft das zur Laufzeit statt zur Compile-Zeit
extension StoreKitBridge: @preconcurrency StoreBridge {
    func start(listener: StoreListener) {
        self.listener = listener
        updates?.cancel()
        // Kaeufe ausserhalb der App (Kaufanfrage bestaetigt, anderes Geraet, Erstattung)
        updates = Task.detached { [weak self] in
            for await result in Transaction.updates {
                // jede Transaktion abschliessen; freigeschaltet wird nur, was verifiziert ist (query)
                await result.unsafePayloadValue.finish()
                await self?.scheduleRefresh()
            }
        }
    }

    func refresh() {
        scheduleRefresh()
    }

    func loadProducts() {
        let unlockID = unlockID
        let trialID = trialID
        Task {
            let (price, trial) = await Self.prices(unlockID: unlockID, trialID: trialID)
            self.listener?.onProducts(unlockPrice: price, trialAvailable: trial)
        }
    }

    func purchase(trial: Bool) {
        let id = trial ? trialID : unlockID
        Task {
            do {
                guard let product = try await Product.products(for: [id]).first else {
                    self.listener?.onPurchaseFailed()
                    return
                }
                switch try await product.purchase() {
                case .success(.verified(let transaction)):
                    await transaction.finish()
                    self.scheduleRefresh()
                case .success(.unverified):
                    // nicht von Apple signiert: nicht freischalten
                    self.listener?.onPurchaseFailed()
                case .pending:
                    self.listener?.onPurchasePending()
                case .userCancelled:
                    self.listener?.onPurchaseCancelled()
                @unknown default:
                    self.listener?.onPurchaseCancelled()
                }
            } catch {
                self.listener?.onPurchaseFailed()
            }
        }
    }

    // "Kauf wiederherstellen" (Apple-Pflicht): mit dem App Store abgleichen, dann neu abfragen
    func restore() {
        Task {
            do {
                try await AppStore.sync()
                self.scheduleRefresh()
            } catch {
                self.listener?.onRestoreFailed()
            }
        }
    }

    func stop() {
        updates?.cancel()
        updates = nil
        refreshTask?.cancel()
        refreshTask = nil
        listener = nil
    }
}
