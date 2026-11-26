import Foundation
import StoreKit
import AappsKit

/// StoreKit 2 fuer die Kotlin-Seite (StoreGate in core/store). Gleich fuer alle Apps:
/// Produkte <Bundle-ID>.full_unlock und <Bundle-ID>.trial (beide nicht verbrauchbar, Testphase mit Preis 0).
/// Aufrufe kommen vom Main-Thread, Rueckmeldungen gehen immer auf dem Main-Thread an den Listener.
final class StoreKitBridge: NSObject, StoreBridge {
    private let unlockID: String
    private let trialID: String
    private var listener: StoreListener?
    private var updates: Task<Void, Never>?

    init(bundleID: String) {
        unlockID = bundleID + ".full_unlock"
        trialID = bundleID + ".trial"
        super.init()
    }

    func start(listener: StoreListener) {
        self.listener = listener
        updates?.cancel()
        // Kaeufe ausserhalb der App (Kaufanfrage bestaetigt, anderes Geraet, Erstattung)
        updates = Task.detached { [weak self] in
            for await result in Transaction.updates {
                // jede Transaktion abschliessen; freigeschaltet wird nur, was verifiziert ist (refreshNow)
                await result.unsafePayloadValue.finish()
                await self?.refreshNow()
            }
        }
    }

    func refresh() {
        Task { await self.refreshNow() }
    }

    func loadProducts() {
        Task {
            let products = (try? await Product.products(for: [self.unlockID, self.trialID])) ?? []
            let price = products.first(where: { $0.id == self.unlockID })?.displayPrice
            let trial = products.contains(where: { $0.id == self.trialID })
            await MainActor.run {
                self.listener?.onProducts(unlockPrice: price, trialAvailable: trial)
            }
        }
    }

    func purchase(trial: Bool) {
        let id = trial ? trialID : unlockID
        Task {
            do {
                guard let product = try await Product.products(for: [id]).first else {
                    await MainActor.run { self.listener?.onPurchaseFailed() }
                    return
                }
                let result = try await product.purchase()
                switch result {
                case .success(let verification):
                    await verification.unsafePayloadValue.finish()
                    await self.refreshNow()
                case .pending:
                    await MainActor.run { self.listener?.onPurchasePending() }
                case .userCancelled:
                    await MainActor.run { self.listener?.onPurchaseCancelled() }
                @unknown default:
                    await MainActor.run { self.listener?.onPurchaseCancelled() }
                }
            } catch {
                await MainActor.run { self.listener?.onPurchaseFailed() }
            }
        }
    }

    // "Kauf wiederherstellen" (Apple-Pflicht): mit dem App Store abgleichen, dann neu abfragen
    func restore() {
        Task {
            do {
                try await AppStore.sync()
                await self.refreshNow()
            } catch {
                await MainActor.run { self.listener?.onRestoreFailed() }
            }
        }
    }

    func stop() {
        updates?.cancel()
        updates = nil
        listener = nil
    }

    // Transaction.currentEntitlements: nur verifizierte, nicht erstattete Kaeufe zaehlen
    private func refreshNow() async {
        var unlocked = false
        var trialStart: Int64 = -1
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result, transaction.revocationDate == nil else { continue }
            if transaction.productID == unlockID {
                unlocked = true
            } else if transaction.productID == trialID {
                trialStart = Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000)
            }
        }
        // Ohne App-Store-Konto ist "nichts gefunden" keine verlaessliche Antwort: dann gilt der lokale Cache
        let storefront = await Storefront.current
        let failed = !unlocked && trialStart < 0 && storefront == nil
        await MainActor.run {
            if failed {
                self.listener?.onQueryFailed()
            } else {
                self.listener?.onEntitlements(unlocked: unlocked, trialStartMillis: trialStart)
            }
        }
    }
}
