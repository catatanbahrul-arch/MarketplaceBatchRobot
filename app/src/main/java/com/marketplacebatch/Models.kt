package com.marketplacebatch

enum class AccountState { READY, RUNNING, NEED_LOGIN, UNAVAILABLE, DONE, ERROR }

data class BatchAccount(val id:String,val displayName:String,val packageName:String,val enabled:Boolean=true,val position:Int=0)

data class BatchConfig(
    val scanIdleMs:Long=9000L,
    val uiOpenWaitMs:Long=2500L,
    val replyDelayMinMs:Long=3000L,
    val replyDelayMaxMs:Long=6000L,
    val maxRepliesPerAccount:Int=100,
    val dryRun:Boolean=true,
    val marketplaceRequired:Boolean=true,
    val maxVisibleRowsPerSweep:Int=30
)
