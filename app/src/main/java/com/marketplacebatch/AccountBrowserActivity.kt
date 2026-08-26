package com.marketplacebatch

import android.app.AlertDialog
import android.webkit.WebResourceRequest
import android.net.Uri
import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class AccountBrowserActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var db: RobotDb

    private val handler = Handler(Looper.getMainLooper())

    private val facebookOrigins = listOf(
        "https://www.facebook.com/",
        "https://m.facebook.com/",
        "https://facebook.com/",
        "https://web.facebook.com/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = RobotDb(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
        }

        status = TextView(this).apply {
            text = "Browser Facebook • memeriksa login..."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val refresh = Button(this).apply {
            text = "↻"
            setOnClickListener {
                webView.reload()
            }
        }

        val save = Button(this).apply {
            text = "Simpan Akun"
            setOnClickListener {
                saveAccount()
            }
        }

        top.addView(status)
        top.addView(refresh)
        top.addView(save)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return handleFacebookNavigation(view, url)
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    return handleFacebookNavigation(view, url ?: return false)
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    cm.flush()

                    handler.postDelayed(
                        { updateStatus() },
                        700
                    )
                }
            }

            webChromeClient = WebChromeClient()
        }

        root.addView(top)
        root.addView(webView)

        setContentView(root)

        webView.loadUrl("https://www.facebook.com/")
        updateStatus()
    }

    private fun handleFacebookNavigation(
        view: WebView?,
        url: String
    ): Boolean {
        val lower = url.lowercase()

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun cookieText(): String {
        val cm = CookieManager.getInstance()

        return facebookOrigins
            .mapNotNull { origin ->
                try {
                    cm.getCookie(origin)
                } catch (_: Exception) {
                    null
                }
            }
            .joinToString("; ")
    }

    private fun facebookId(): String? {
        val cookies = cookieText()

        val match = Regex(
            "(^|;\\s*)c_user=([^;]+)"
        ).find(cookies)

        return match
            ?.groupValues
            ?.getOrNull(2)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun loggedIn(): Boolean {
        return facebookId() != null
    }

    private fun updateStatus() {
        status.text = if (loggedIn()) {
            "Browser Facebook • LOGIN TERDETEKSI • ID ${facebookId()}"
        } else {
            "Browser Facebook • belum login"
        }
    }

    private fun saveAccount() {
        val id = facebookId()

        if (id == null) {
            Toast.makeText(
                this,
                "Login Facebook dulu di browser ini. Setelah halaman Facebook selesai dimuat, tekan ↻ lalu coba Simpan Akun.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val name = EditText(this).apply {
            hint = "Nama / alias akun"
        }

        val info = TextView(this).apply {
            text = "ID Facebook terdeteksi: $id"
            setPadding(16, 8, 16, 8)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(info)
            addView(name)
        }

        AlertDialog.Builder(this)
            .setTitle("Simpan akun Facebook")
            .setView(box)
            .setPositiveButton("Simpan") { _, _ ->

                val alias = name.text
                    .toString()
                    .trim()
                    .ifBlank { "Facebook $id" }

                val existing = db.accounts()
                    .indexOfFirst { it.id == id }

                val position = if (existing >= 0) {
                    db.accounts()[existing].position
                } else {
                    db.accounts().size
                }

                db.upsertAccount(
                    id = id,
                    name = alias,
                    packageName = "internal-webview",
                    position = position
                )

                Toast.makeText(
                    this,
                    "Akun Facebook berhasil disimpan.",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
