package com.marketplacebatch

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class BatchCoordinator(
    private val context:Context,
    private val db:RobotDb,
    private val config:BatchConfig,
    private val onState:(BatchState,String)->Unit
){
    private val main=Handler(Looper.getMainLooper())
    private var accounts:List<BatchAccount> = emptyList()
    private var index=0
    private var current:BatchAccount?=null
    private var stopped=true
    private var accountIdleToken=0L
    private var advancingAccount=false
    var state:BatchState=BatchState.IDLE;private set
    fun start(){
        if(state==BatchState.RUNNING||state==BatchState.STARTING||state==BatchState.SCANNING)return
        accounts=BatchPlanner().ordered(db.accounts())
        index=0;stopped=false;accountIdleToken++
        if(accounts.isEmpty()){set(BatchState.ERROR,"Tidak ada akun aktif");return}
        db.setSetting("enabled","1")
        set(BatchState.STARTING,"Memulai batch: ${accounts.size} akun")
        nextAccount()
    }
    fun stop(){stopped=true;advancingAccount=false;accountIdleToken++;main.removeCallbacksAndMessages(null);db.setSetting("enabled","0");set(BatchState.STOPPED,"Batch dihentikan")}
    fun onScreenActivity(packageName:String){
        if(stopped)return
        val a=current?:return
        if(packageName!=a.packageName)return
        if(state==BatchState.WAITING_UI||state==BatchState.STARTING||state==BatchState.SCANNING)set(BatchState.RUNNING,"Akun aktif: ${a.displayName}")
        set(BatchState.SCANNING,"Memindai ${a.displayName}")
        armIdleTimer()
    }
    fun onAccountBlocked(reason:String){
        if(stopped || advancingAccount)return
        advancingAccount=true
        db.log("WARN","${current?.id}: $reason")
        set(BatchState.SKIPPING,"${current?.displayName}: $reason")
        index++
        main.postDelayed({
            advancingAccount=false
            nextAccount()
        },500)
    }
    fun noteReplyActivity(){if(stopped)return;set(BatchState.REPLYING,"Membalas pesan ${current?.displayName}");armIdleTimer()}
    fun scheduleNextSweep(){armIdleTimer()}
    fun randomReplyDelay():Long=if(config.replyDelayMaxMs<=config.replyDelayMinMs)config.replyDelayMinMs else kotlin.random.Random.nextLong(config.replyDelayMinMs,config.replyDelayMaxMs+1)
    private fun armIdleTimer(){
        accountIdleToken++
        val token=accountIdleToken
        main.removeCallbacksAndMessages(null)
        main.postDelayed({if(!stopped&&token==accountIdleToken)accountComplete()},config.scanIdleMs)
    }
    private fun accountComplete(){
        if(stopped)return
        val done=current?.displayName?:("Akun ${index+1}")
        set(BatchState.ACCOUNT_DONE,"Selesai memindai: $done")
        index++;main.postDelayed({nextAccount()},700)
    }
    private fun nextAccount(){
        if(stopped)return
        if(index>=accounts.size){current=null;set(BatchState.COMPLETE,"Semua ${accounts.size} akun selesai");db.setSetting("enabled","0");return}
        current=accounts[index]
        val a=current!!
        set(BatchState.STARTING,"Akun ${index+1}/${accounts.size}: ${a.displayName}")
        val launch=context.packageManager.getLaunchIntentForPackage(a.packageName)
        if(launch==null){onAccountBlocked("Package tidak ditemukan: ${a.packageName}");return}
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching{context.startActivity(launch)}.onFailure{onAccountBlocked("Gagal membuka aplikasi: ${it.message}");return}
        set(BatchState.WAITING_UI,"Menunggu UI ${a.displayName}")
        main.postDelayed({if(!stopped&&current?.id==a.id){set(BatchState.SCANNING,"Mulai sweep ${a.displayName}")}},config.uiOpenWaitMs)
    }
    private fun set(s:BatchState,msg:String){state=s;onState(s,msg)}
}
