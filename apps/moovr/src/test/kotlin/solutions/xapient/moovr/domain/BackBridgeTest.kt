package solutions.xapient.moovr.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackBridgeTest {

    @Test
    fun handledKeepsApp() {
        assertFalse(BackBridge.shouldExit("\"handled\""))
        assertFalse(BackBridge.shouldExit(" \"handled\" "))
    }

    @Test
    fun everythingElseExits() {
        listOf("\"exit\"", "null", "", null, "\"\"", "undefined", "\"HANDLED\"").forEach {
            assertTrue("$it", BackBridge.shouldExit(it))
        }
    }
}
