package solutions.xapient.aapps.core.billing

import android.content.pm.PackageManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BundleKeyTest {
    private val hex = "AB:".repeat(31) + "CD"
    private val bytes = ByteArray(32) { if (it == 31) 0xCD.toByte() else 0xAB.toByte() }

    private fun check(sdk: Int = 28, cert: String = hex, installer: String? = "com.android.vending", signed: Boolean = true) =
        BundleKey.check(sdk, cert, { installer }) { it.contentEquals(bytes) && signed }

    @Test fun parsesConsoleFormatWithAndWithoutColons() {
        assertArrayEquals(bytes, BundleKey.parseHex(hex))
        assertArrayEquals(bytes, BundleKey.parseHex(hex.replace(":", "").lowercase()))
        assertNull(BundleKey.parseHex(""))
        assertNull(BundleKey.parseHex("AB:CD"))
        assertNull(BundleKey.parseHex("ZZ".repeat(32)))
    }

    @Test fun playInstallWithRightKeyUnlocks() = assertTrue(check())

    @Test fun wrongKeyOrInstallerOrOldAndroidLocks() {
        assertFalse(check(signed = false))
        assertFalse(check(installer = "com.example.mirror"))
        assertFalse(check(installer = null))
        assertFalse(check(sdk = 27))
        assertFalse(check(cert = ""))
    }

    @Test fun notInstalledThrowsAndLocks() {
        assertFalse(BundleKey.check(30, hex, { throw PackageManager.NameNotFoundException() }) { true })
    }

    @Test fun placeholderNeverUnlocks() {
        // solange der Hash fehlt, darf nichts frei werden
        if (BundleKey.BUNDLE_CERT_SHA256.isEmpty()) assertFalse(BundleKey.installed(RuntimeEnvironment.getApplication()))
        assertEquals("solutions.xapient.bundle", BundleKey.PACKAGE)
    }
}
