package com.marketplacebatch

class ReplyPolicy(private val db:RobotDb,private val config:BatchConfig){
    fun handle(accountId:String,sender:String,text:String,marketplace:Boolean,send:(String)->Boolean,onReplyAttempt:()->Unit={}):Boolean{
        db.purgeExpiredReservations()
        db.markIncoming(accountId,sender)
        if(config.marketplaceRequired&&!marketplace){db.log("DEBUG","Non-marketplace ignored: $sender");return false}
        if(db.hasReplied(accountId,sender)){db.log("INFO","Already replied: $accountId/$sender");return false}
        val maxReplies=db.setting("max_replies_per_account",config.maxRepliesPerAccount.toString()).toIntOrNull()?:config.maxRepliesPerAccount
        if(db.accountReplyCount(accountId)>=maxReplies){db.log("INFO","Reply limit reached: $accountId");return false}
        val cooldown=db.setting("reply_cooldown_ms",config.replyDelayMinMs.toString()).toLongOrNull()?:config.replyDelayMinMs
        if(System.currentTimeMillis()-db.lastReplyAt(accountId)<cooldown){db.log("INFO","Cooldown active: $accountId");return false}
        if(!db.reserveReply(accountId,sender)){db.log("INFO","Reply already reserved: $accountId/$sender");return false}
        val template=db.setting("template")
        if(template.isBlank()){db.releaseReply(accountId,sender);db.log("WARN","Empty template");return false}
        val reply=template.replace("{{whatsapp}}",db.setting("whatsapp"))
        onReplyAttempt()
        if(config.dryRun){db.releaseReply(accountId,sender);db.log("DRY","$accountId/$sender => $reply");return true}
        val ok=runCatching{send(reply)}.getOrDefault(false)
        if(ok){db.markReplied(accountId,sender);db.log("INFO","Sent once: $accountId/$sender")}
        else{db.releaseReply(accountId,sender);db.log("WARN","Send failed: $accountId/$sender")}
        return ok
    }
}
