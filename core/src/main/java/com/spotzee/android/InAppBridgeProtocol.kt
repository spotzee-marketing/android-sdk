package com.spotzee.android

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

internal const val IN_APP_BRIDGE_ORIGIN = "https://inapp.spotzee.invalid"
internal const val IN_APP_BRIDGE_BASE_URL = "$IN_APP_BRIDGE_ORIGIN/"
internal const val IN_APP_BRIDGE_INIT = "spotzee-in-app-bridge"

internal data class InAppBridgeMessage(
    val action: InAppAction,
    val context: Map<String, Any>,
)

private val bridgeGson = Gson()
private val bridgeContextType = object : TypeToken<Map<String, Any>>() {}.type

internal fun decodeInAppBridgeMessage(
    rawMessage: String?,
    isMainFrame: Boolean,
): InAppBridgeMessage? {
    if (!isMainFrame || rawMessage.isNullOrBlank()) return null

    return try {
        val envelope = JsonParser.parseString(rawMessage)
        if (!envelope.isJsonObject) return null

        val actionName = envelope.asJsonObject.get("action")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            ?: return null
        val action = InAppAction.entries.firstOrNull {
            it.name.equals(actionName, ignoreCase = true)
        } ?: return null

        when (action) {
            InAppAction.DISMISS -> InAppBridgeMessage(action, emptyMap())
            InAppAction.CUSTOM -> {
                val payload = envelope.asJsonObject.get("payload")
                val context = when {
                    payload == null || payload.isJsonNull -> emptyMap()
                    payload.isJsonObject -> bridgeGson.fromJson<Map<String, Any>>(
                        payload,
                        bridgeContextType,
                    ) ?: emptyMap()
                    else -> return null
                }
                InAppBridgeMessage(action, context)
            }
        }
    } catch (_: Exception) {
        null
    }
}
