package solutions.xapient.aapps.core.store

import platform.Foundation.NSUserDefaults

// Cache in den UserDefaults der App (Required-Reason-API, in PrivacyInfo.xcprivacy deklariert)
class UserDefaultsStoreCache(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : StoreCache {
    override var unlocked: Boolean
        get() = defaults.boolForKey(KEY_UNLOCKED)
        set(v) = defaults.setBool(v, KEY_UNLOCKED)

    override var trialStart: Long?
        get() = if (defaults.objectForKey(KEY_TRIAL) == null) null else defaults.doubleForKey(KEY_TRIAL).toLong()
        set(v) = if (v == null) defaults.removeObjectForKey(KEY_TRIAL) else defaults.setDouble(v.toDouble(), KEY_TRIAL)

    private companion object {
        const val KEY_UNLOCKED = "aapps_store_unlocked"
        const val KEY_TRIAL = "aapps_store_trial_start"
    }
}
