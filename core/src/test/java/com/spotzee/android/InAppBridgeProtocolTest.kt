package com.spotzee.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InAppBridgeProtocolTest {
    @Test
    fun rejectsSubframeMessagesAndAllFrameJavascriptInterfaces() {
        val message = decodeInAppBridgeMessage(
            rawMessage = """{"action":"custom","payload":{"screen":"billing"}}""",
            isMainFrame = false,
        )

        assertNull(message)

        val source = sequenceOf(
            File("core/src/main/java/com/spotzee/android/InAppDialogFragment.kt"),
            File("src/main/java/com/spotzee/android/InAppDialogFragment.kt"),
        ).first(File::isFile).readText()
        assertFalse(source.contains("addJavascriptInterface"))
        assertTrue(source.contains("postWebMessage"))
        assertTrue(source.contains("if (!request.isForMainFrame) return true"))
        assertTrue(source.contains("if (!request.isForMainFrame) return false"))
    }

    @Test
    fun acceptsMainFrameActionsWithObjectPayloads() {
        val dismiss = decodeInAppBridgeMessage(
            rawMessage = """{"action":"dismiss"}""",
            isMainFrame = true,
        )
        val custom = decodeInAppBridgeMessage(
            rawMessage = """{"action":"custom","payload":{"screen":"billing","step":2}}""",
            isMainFrame = true,
        )

        assertEquals(InAppAction.DISMISS, dismiss?.action)
        assertTrue(dismiss?.context?.isEmpty() == true)
        assertNotNull(custom)
        assertEquals(InAppAction.CUSTOM, custom?.action)
        assertEquals("billing", custom?.context?.get("screen"))
        assertEquals(2.0, custom?.context?.get("step"))
    }

    @Test
    fun rejectsMalformedUnknownAndNonObjectCustomMessages() {
        assertNull(decodeInAppBridgeMessage("not-json", isMainFrame = true))
        assertNull(
            decodeInAppBridgeMessage(
                """{"action":"unknown","payload":{}}""",
                isMainFrame = true,
            ),
        )
        assertNull(
            decodeInAppBridgeMessage(
                """{"action":"custom","payload":"billing"}""",
                isMainFrame = true,
            ),
        )
    }
}
