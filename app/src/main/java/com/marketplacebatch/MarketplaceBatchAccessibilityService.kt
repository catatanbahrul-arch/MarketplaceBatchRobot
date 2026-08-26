package com.marketplacebatch

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

object MarketplaceBatchServiceRegistry { @Volatile var service: MarketplaceBatchAccessibilityService? = null }

class MarketplaceBatchAccessibilityService : AccessibilityService() {
    private lateinit var db: RobotDb
    private lateinit var policy: ReplyPolicy
    private lateinit var batch: BatchCoordinator
    private val handler = Handler(Looper.getMainLooper())
    private var lastAccountId = ""
    private val visitedRows = linkedSetOf<String>()
    private var rowClicks = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        MarketplaceBatchServiceRegistry.service = this
        db = RobotDb(this)
        policy = ReplyPolicy(db, currentConfig())
        batch = BatchCoordinator(this, db, currentConfig()) { state, msg -> db.log(state.name, msg) }
        db.log("INFO", "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::batch.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        batch.onScreenActivity(pkg)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ scan(pkg) }, 400L)
    }

    private fun currentConfig() = BatchConfig(
        scanIdleMs = db.setting("scan_idle_ms", "9000").toLongOrNull() ?: 9000L,
        uiOpenWaitMs = db.setting("ui_open_wait_ms", "2500").toLongOrNull() ?: 2500L,
        replyDelayMinMs = db.setting("reply_cooldown_ms", "3500").toLongOrNull() ?: 3500L,
        replyDelayMaxMs = (db.setting("reply_cooldown_ms", "3500").toLongOrNull() ?: 3500L) + 2500L,
        maxRepliesPerAccount = db.setting("max_replies_per_account", "100").toIntOrNull() ?: 100,
        dryRun = db.setting("dry_run", "1") == "1",
        marketplaceRequired = db.setting("marketplace_required", "1") == "1",
        maxVisibleRowsPerSweep = 30
    )

    private fun scan(pkg: String) {
        if (!::db.isInitialized || !::batch.isInitialized) return
        val account = db.accounts().firstOrNull { it.packageName == pkg && it.enabled } ?: return
        if (lastAccountId != account.id) {
            lastAccountId = account.id
            visitedRows.clear()
            rowClicks = 0
        }

        val root = rootInActiveWindow ?: return
        val texts = collectText(root).map(String::trim).filter(String::isNotBlank).distinct()

        if (hasLoginOrChallenge(texts)) {
            batch.onAccountBlocked("Login/CAPTCHA/verification terdeteksi")
            return
        }

        val chatView = findEditable(root) != null
        if (chatView) {
            processCurrentConversation(account, root, texts)
            return
        }

        if (!isMarketplaceContext(texts)) {
            batch.scheduleNextSweep()
            return
        }

        val next = findNextMarketplaceRow(root)
        if (next != null) {
            rowClicks++
            next.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ scan(pkg) }, 500L)
            return
        }

        batch.scheduleNextSweep()
    }

    private fun processCurrentConversation(account: BatchAccount, root: AccessibilityNodeInfo, texts: List<String>) {
        if (!isMarketplaceContext(texts)) {
            batch.scheduleNextSweep()
            return
        }
        val sender = extractSender(texts)
        val message = extractMessage(texts)
        if (sender.isNullOrBlank() || message.isNullOrBlank()) {
            db.log("WARN", "Konversasi Marketplace tidak cukup jelas; tidak membalas")
            batch.scheduleNextSweep()
            return
        }

        val sent = policy.handle(account.id, sender, message, true, { text -> reply(root, text) }) {
            batch.noteReplyActivity()
        }

        if (sent || db.hasReplied(account.id, sender)) {
            handler.postDelayed({ performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }, 700L)
        }
        batch.scheduleNextSweep()
    }

    private fun findNextMarketplaceRow(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (rowClicks >= 30) return null
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectClickableRows(root, candidates)
        for (node in candidates) {
            val key = normalize(rowText(node))
            if (key.length < 6) continue
            if (!isMarketplaceText(key)) continue
            if (visitedRows.add(key)) return node
        }
        return null
    }

    private fun collectClickableRows(node: AccessibilityNodeInfo, output: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) {
            val text = normalize(rowText(node))
            if (text.isNotBlank() && isMarketplaceText(text)) output += node
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectClickableRows(it, output) }
    }

    private fun rowText(node: AccessibilityNodeInfo): String {
        val values = mutableListOf<String>()
        node.text?.toString()?.takeIf(String::isNotBlank)?.let(values::add)
        node.contentDescription?.toString()?.takeIf(String::isNotBlank)?.let(values::add)
        for (i in 0 until node.childCount) node.getChild(i)?.let { values += rowText(it) }
        return values.joinToString(" | ")
    }

    private fun normalize(value: String) = value.replace("\\s+".toRegex(), " ").trim().take(240).lowercase()

    private fun isMarketplaceText(text: String): Boolean {
        val words = listOf("marketplace", "listing", "lihat barang", "view item", "item details")
        return words.any { text.contains(it, ignoreCase = true) }
    }

    private fun isMarketplaceContext(texts: List<String>) = texts.any { isMarketplaceText(normalize(it)) }

    private fun hasLoginOrChallenge(texts: List<String>): Boolean {
        val terms = listOf("log in", "login", "masuk", "continue with facebook", "verifikasi", "verify", "captcha", "security check")
        return texts.any { text -> terms.any { term -> text.equals(term, true) || text.contains(term, true) } }
    }

    private fun extractSender(texts: List<String>): String? {
        texts.firstOrNull { it.startsWith("sender:", true) }
            ?.substringAfter(":")?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        return texts.firstOrNull { it.length in 2..80 && !isUiLabel(it) && !isMarketplaceText(normalize(it)) }
    }

    private fun extractMessage(texts: List<String>): String? {
        texts.firstOrNull { it.startsWith("message:", true) }
            ?.substringAfter(":")?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        return texts.lastOrNull { it.length in 2..1000 && !isUiLabel(it) && !isMarketplaceText(normalize(it)) }
    }

    private fun isUiLabel(value: String) = listOf(
        "send", "kirim", "back", "more", "menu", "marketplace", "search", "cari", "settings", "pengaturan"
    ).any { value.equals(it, true) }

    private fun collectText(node: AccessibilityNodeInfo): List<String> {
        val output = mutableListOf<String>()
        node.text?.toString()?.takeIf(String::isNotBlank)?.let(output::add)
        node.contentDescription?.toString()?.takeIf(String::isNotBlank)?.let(output::add)
        for (i in 0 until node.childCount) node.getChild(i)?.let { output += collectText(it) }
        return output
    }

    private fun reply(root: AccessibilityNodeInfo, text: String): Boolean {
        val input = findEditable(root) ?: return false
        if (!input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        if (!input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        val send = findByText(root, "Send") ?: findByText(root, "Kirim") ?: return false
        return send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) findEditable(node.getChild(i))?.let { return it }
        return null
    }

    private fun findByText(node: AccessibilityNodeInfo?, needle: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.trim()?.equals(needle, true) == true ||
            node.contentDescription?.toString()?.trim()?.equals(needle, true) == true) return node
        for (i in 0 until node.childCount) findByText(node.getChild(i), needle)?.let { return it }
        return null
    }

    fun startBatch(): Boolean {
        if (!::batch.isInitialized) return false
        batch.start()
        return true
    }

    fun stopBatch() {
        if (::batch.isInitialized) batch.stop()
    }

    override fun onInterrupt() {
        if (::db.isInitialized) db.log("WARN", "Accessibility interrupted")
    }

    override fun onDestroy() {
        MarketplaceBatchServiceRegistry.service = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
