package com.spotzee.example

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.spotzee.android.InAppAction
import com.spotzee.android.InAppDelegate
import com.spotzee.android.InAppDisplayState
import com.spotzee.android.Spotzee
import com.spotzee.android.SpotzeeNotification

class MainApplication : Application(), InAppDelegate {

    override fun onCreate() {
        super.onCreate()

        // TODO: Enter API Key and URL
        val apiKey = "" // like: pk_fdfbi282ec65-4a4f-b9ef-6f6979905523
        val urlEndpoint = "" // like: https://spotzee.company.com/api
        analytics = Spotzee.initialize(
            app = this,
            apiKey = apiKey,
            urlEndpoint = urlEndpoint,
            isDebug = true,
            inAppDelegate = this
        )
    }

    override val autoShow: Boolean = true

    override val useDarkMode: Boolean = false

    override fun onNew(notification: SpotzeeNotification): InAppDisplayState {
        Log.d(LOG_TAG, "onNew: $notification")
        return InAppDisplayState.SHOW
    }

    override fun handle(
        action: InAppAction,
        context: Map<String, Any>,
        notification: SpotzeeNotification
    ) {
        Log.d(LOG_TAG, "handle: $action, context: $context, notification: $notification")
        Toast.makeText(this, "Action: $action", Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: Throwable) {
        Log.e(LOG_TAG, "onError: $error")
        super.onError(error)
    }

    companion object {
        private const val LOG_TAG = "MainApplication"

        lateinit var analytics: Spotzee
    }
}