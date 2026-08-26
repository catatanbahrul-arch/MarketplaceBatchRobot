package com.marketplacebatch

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
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
            text = "Browser Facebook • belum login"
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
            setOnClickListener { webView.reload() }
        }

        val save = Button(this).apply {
            text = "Simpan Akun"
            setOnClickListener { saveAccount() }
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
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    updateStatus()
                }
            }

            webChromeClient = WebChromeClient()
        }

        root.addView(top)
        root.addView(webView)
        setContentView(root)

        webView.loadUrl("https://m.facebook.com/")
        updateStatus()
    }

    private fun cookies(): String =
        CookieManager.getInstance()
            .getCookie("https://m.facebook.com/")
            .orEmpty()

    private fun facebookId(): String? {
        val match = Regex("(^|;\\s*)c_user=([^;]+)").find(cookies())
        return match?.groupValues?.getOrNull(2)
    }

    private fun loggedIn(): Boolean = facebookId() != null

    private fun updateStatus() {
        status.text = if (loggedIn()) {
            "Browser Facebook • LOGIN TERDETEKSI"
        } else {
            "Browser Facebook • belum login"
        }
    }

    private fun saveAccount() {
        val id = facebookId()

        if (id == null) {
            Toast.makeText(
                this,
                "Login Facebook dulu di browser ini.",
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
                val alias = name.text.toString().trim()
                    .ifBlank { "Facebook $id" }

                db.upsertAccount(
                    id = id,
                    name = alias,
                    packageName = "internal-webview",
                    position = db.accounts().size
                )

                Toast.makeText(
                    this,
                    "Akun berhasil disimpan.",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
