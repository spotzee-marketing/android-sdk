package com.spotzee.android

import java.text.SimpleDateFormat
import java.util.*

class Constants {
    companion object {
        const val DEFAULT_URL_ENDPOINT: String = "https://apix.spotzee.com/api"
        const val SPOTZEE_KEY: String = "spotzee"
        const val IN_APP_CHECK_MESSAGE_KEY: String = "check_in_app_messages"

        /** Pinned Spotzee API version this SDK release targets. */
        const val SPOTZEE_API_VERSION: String = "2026-04-28"

        /** x-spotzee-client-type value sent on every request from this SDK. */
        const val CLIENT_TYPE: String = "sdk-android"

        val iso8601DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}