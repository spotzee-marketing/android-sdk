package com.spotzee.android.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.spotzee.android.AlertNotification
import com.spotzee.android.BannerNotification
import com.spotzee.android.HtmlNotification
import com.spotzee.android.NotificationContent
import com.spotzee.android.NotificationType
import com.spotzee.android.SpotzeeNotification
import java.lang.reflect.Type
import java.util.Date

class SpotzeeNotificationDeserializer : JsonDeserializer<SpotzeeNotification> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SpotzeeNotification {
        val jsonObject = json.asJsonObject
        val contentTypeString = jsonObject.get("content_type").asString
        val contentType = try {
            NotificationType.valueOf(contentTypeString.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            throw JsonParseException("Unknown notification content type: $contentTypeString", e)
        }

        val contentJson = jsonObject.getAsJsonObject("content")
        val notificationContent: NotificationContent = context.deserialize(
            contentJson,
            NotificationContent::class.java
        )
        return SpotzeeNotification(
            id = jsonObject.get("id")?.asLong ?: 0L,
            contentType = contentType,
            content = notificationContent,
            readAt = context.deserialize<Date>(jsonObject.get("read_at"), Date::class.java),
            expiresAt = context.deserialize<Date>(jsonObject.get("expires_at"), Date::class.java),
        )
    }
}
