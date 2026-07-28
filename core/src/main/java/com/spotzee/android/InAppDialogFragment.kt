package com.spotzee.android

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InAppDialogFragment : DialogFragment() {

    private var webView: WebView? = null
    private var bridgePort: WebMessagePort? = null

    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url ?: return false
            Log.d(DIALOG_TAG, "WebView trying to load URL: $url")

            if (url.scheme == Constants.SPOTZEE_KEY) {
                if (!request.isForMainFrame) return true

                when (url.host) { // e.g., spotzee://dismiss, spotzee://custom
                    "dismiss" -> processAction(InAppAction.DISMISS)
                    "custom" -> {
                        val params = mutableMapOf<String, Any>("url" to url.toString())
                        url.queryParameterNames.forEach { key ->
                            url.getQueryParameter(key)?.let { value -> params[key] = value }
                        }
                        processAction(InAppAction.CUSTOM, params)
                    }
                    else -> processAction(InAppAction.CUSTOM, mapOf("url" to url.toString()))
                }
                return true
            }

            if (!request.isForMainFrame) return false

            // For any other URLs, open them in an external browser
            // This prevents the WebView from navigating away from your in-app message content.
            try {
                val intent = Intent(Intent.ACTION_VIEW, url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return true
            } catch (e: Exception) {
                delegate?.onError(e)
                return false
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            configureBridge(view)
            setThemeJs()

            notification?.let { delegate?.onNotificationShown(it) }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (error != null) {
                delegate?.onError(Exception("WebView error: ${error.description} on URL ${request?.url}"))
            }
        }
    }

    private var notification: SpotzeeNotification? = null
    private var delegate: InAppDelegate? = null

    override fun onStart() {
        super.onStart()

        setWindowTransitions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notification = arguments?.parcelable(ARG_NOTIFICATION)
        if (notification == null) {
            dismissAllowingStateLoss()
            return
        }

        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.dialog_in_app_container, container, false)
        val webViewContainer = rootView.findViewById<ViewGroup>(R.id.webview_container)
        webView = WebView(requireContext()).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
        }

        webViewContainer.addView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        webView?.webViewClient = webViewClient
        when (val content = notification?.content) {
            is HtmlNotification -> {
                webView?.loadDataWithBaseURL(
                    IN_APP_BRIDGE_BASE_URL,
                    content.html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
            else -> {
                Log.e(DIALOG_TAG, "Notification content is not HTML. Cannot display in WebView.")
                // Potentially dismiss or show a fallback UI
                dismissAllowingStateLoss()
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            window?.apply {
                setGravity(Gravity.BOTTOM)
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                lifecycleScope.launch {
                    delay(10)
                    setWindowTransitions()
                }
            }
        }
    }

    private fun setWindowTransitions() {
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    private fun setThemeJs() {
        val useDarkMode = delegate?.useDarkMode == true
        val function = if (useDarkMode) {
            "document.documentElement.classList.add('darkMode');"
        } else {
            "document.documentElement.classList.remove('darkMode');"
        }
        webView?.evaluateJavascript("javascript: $function", null)
    }

    private fun configureBridge(view: WebView) {
        bridgePort?.close()
        val ports = view.createWebMessageChannel()
        bridgePort = ports[0].apply {
            setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
                override fun onMessage(port: WebMessagePort, message: WebMessage?) {
                    val bridgeMessage = decodeInAppBridgeMessage(
                        rawMessage = message?.data,
                        isMainFrame = true,
                    )
                    if (bridgeMessage == null) {
                        Log.w(DIALOG_TAG, "Rejected malformed in-app bridge message")
                        return
                    }
                    activity?.runOnUiThread {
                        processAction(bridgeMessage.action, bridgeMessage.context)
                    }
                }
            })
        }

        val installBridge = """
            (function () {
                window.addEventListener('message', function (event) {
                    if (event.data !== '$IN_APP_BRIDGE_INIT' || !event.ports || event.ports.length !== 1) return;
                    var port = event.ports[0];
                    window.dismiss = function () {
                        port.postMessage(JSON.stringify({ action: 'dismiss' }));
                    };
                    window.trigger = function (obj) {
                        port.postMessage(JSON.stringify({ action: 'custom', payload: obj }));
                    };
                }, { once: true });
            })();
        """.trimIndent()
        view.evaluateJavascript(installBridge) {
            view.postWebMessage(
                WebMessage(IN_APP_BRIDGE_INIT, arrayOf(ports[1])),
                Uri.parse(IN_APP_BRIDGE_ORIGIN),
            )
        }
    }

    private fun processAction(action: InAppAction, body: Map<String, Any> = emptyMap()) {
        Log.d(DIALOG_TAG, "Processing action: $action with body: $body")
        val notification = notification
        if (notification != null) {
            delegate?.handle(action, body, notification)
        }
        dismissAllowingStateLoss()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)

        val notification = notification
        if (isAdded && notification != null) {
            delegate?.handle(
                action = InAppAction.DISMISS,
                context = emptyMap(),
                notification = notification
            )
        }
    }

    override fun onDestroyView() {
        bridgePort?.close()
        bridgePort = null
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    companion object {
        const val DIALOG_TAG = "SpotzeeInAppDialog"
        private const val ARG_NOTIFICATION = "arg_notification"

        fun newInstance(
            notification: SpotzeeNotification,
            delegate: InAppDelegate
        ): InAppDialogFragment = InAppDialogFragment().apply {
            arguments = bundleOf(ARG_NOTIFICATION to notification)
            this.delegate = delegate
        }
    }
}
