package com.marketplacebatch

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RobotDb(context: Context):SQLiteOpenHelper(context,"marketplace_batch_robot.db",null,3){
    override fun onCreate(db:SQLiteDatabase){
        schema(db)
        defaults(db)
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        if(oldVersion<2){ try{db.execSQL("ALTER TABLE senders ADD COLUMN reserved_at INTEGER")}catch(_:Exception){} }
        if(oldVersion<3){ try{db.execSQL("ALTER TABLE accounts ADD COLUMN position INTEGER NOT NULL DEFAULT 0")}catch(_:Exception){} }
        schemaIndexes(db)
    }
    private fun schema(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts(id TEXT PRIMARY KEY,name TEXT NOT NULL,package_name TEXT NOT NULL,enabled INTEGER NOT NULL DEFAULT 1,position INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS senders(account_id TEXT NOT NULL,sender_key TEXT NOT NULL,replied_at INTEGER,reserved_at INTEGER,message_count INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(account_id,sender_key))")
        db.execSQL("CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY AUTOINCREMENT,account_id TEXT NOT NULL,sender_key TEXT NOT NULL,type TEXT NOT NULL,payload TEXT NOT NULL,created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS logs(id INTEGER PRIMARY KEY AUTOINCREMENT,level TEXT NOT NULL,message TEXT NOT NULL,created_at INTEGER NOT NULL)")
        schemaIndexes(db)
    }
    private fun schemaIndexes(db:SQLiteDatabase){
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_position ON accounts(position)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_senders_reply ON senders(account_id,replied_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_account ON events(account_id,created_at)")
    }
    private fun defaults(db:SQLiteDatabase){
        val d=mapOf(
            "enabled" to "0","dry_run" to "1",
            "template" to "Terima kasih sudah menghubungi kami. Untuk informasi lebih lanjut silakan lanjut melalui WhatsApp: {{whatsapp}}",
            "whatsapp" to "","max_replies_per_account" to "100","reply_cooldown_ms" to "3500",
            "scan_idle_ms" to "9000","ui_open_wait_ms" to "2500","marketplace_required" to "1"
        )
        d.forEach{(k,v)->db.execSQL("INSERT OR IGNORE INTO settings(key,value) VALUES(?,?)",arrayOf(k,v))}
    }
    fun accounts():List<BatchAccount> =readableDatabase.rawQuery("SELECT id,name,package_name,enabled,position FROM accounts ORDER BY position,id",null).use{c->buildList{while(c.moveToNext())add(BatchAccount(c.getString(0),c.getString(1),c.getString(2),c.getInt(3)==1,c.getInt(4)))}}
    fun upsertAccount(id:String,name:String,packageName:String,position:Int){writableDatabase.execSQL("INSERT INTO accounts(id,name,package_name,enabled,position) VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,package_name=excluded.package_name,position=excluded.position",arrayOf(id,name,packageName,1,position))}
    fun setAccountEnabled(id:String,enabled:Boolean){writableDatabase.execSQL("UPDATE accounts SET enabled=? WHERE id=?",arrayOf(if(enabled)1 else 0,id))}
    fun setSetting(key:String,value:String){writableDatabase.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES(?,?)",arrayOf(key,value))}
    fun setting(key:String,default:String="")=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?",arrayOf(key)).use{if(it.moveToFirst())it.getString(0) else default}
    fun reserveReply(accountId:String,sender:String):Boolean{
        val db=writableDatabase
        db.beginTransaction()
        try{
            val now=System.currentTimeMillis()
            db.execSQL("INSERT OR IGNORE INTO senders(account_id,sender_key,message_count) VALUES(?,?,0)",arrayOf(accountId,sender))
            val changed=db.update("senders",ContentValues().apply{put("reserved_at",now)},"account_id=? AND sender_key=? AND replied_at IS NULL AND reserved_at IS NULL",arrayOf(accountId,sender))
            db.setTransactionSuccessful()
            return changed==1
        }finally{db.endTransaction()}
    }
    fun markIncoming(accountId:String,sender:String){writableDatabase.execSQL("INSERT INTO senders(account_id,sender_key,message_count) VALUES(?,?,1) ON CONFLICT(account_id,sender_key) DO UPDATE SET message_count=message_count+1",arrayOf(accountId,sender))}
    fun markReplied(accountId:String,sender:String){writableDatabase.execSQL("UPDATE senders SET replied_at=?,reserved_at=NULL WHERE account_id=? AND sender_key=? AND reserved_at IS NOT NULL",arrayOf(System.currentTimeMillis(),accountId,sender))}
    fun releaseReply(accountId:String,sender:String){writableDatabase.execSQL("UPDATE senders SET reserved_at=NULL WHERE account_id=? AND sender_key=? AND replied_at IS NULL",arrayOf(accountId,sender))}
    fun purgeExpiredReservations(maxAgeMs:Long=60_000L){writableDatabase.execSQL("UPDATE senders SET reserved_at=NULL WHERE reserved_at IS NOT NULL AND replied_at IS NULL AND ?-reserved_at>=?",arrayOf(System.currentTimeMillis(),maxAgeMs))}
    fun accountReplyCount(accountId:String):Int=readableDatabase.rawQuery("SELECT COUNT(*) FROM senders WHERE account_id=? AND replied_at IS NOT NULL",arrayOf(accountId)).use{if(it.moveToFirst())it.getInt(0) else 0}
    fun lastReplyAt(accountId:String):Long=readableDatabase.rawQuery("SELECT COALESCE(MAX(replied_at),0) FROM senders WHERE account_id=?",arrayOf(accountId)).use{if(it.moveToFirst())it.getLong(0) else 0}
    fun hasReplied(accountId:String,sender:String)=readableDatabase.rawQuery("SELECT replied_at FROM senders WHERE account_id=? AND sender_key=?",arrayOf(accountId,sender)).use{it.moveToFirst()&&!it.isNull(0)}
    fun metrics(accountId:String):Pair<Int,Int>{return readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(CASE WHEN replied_at IS NOT NULL THEN 1 ELSE 0 END),0) FROM senders WHERE account_id=?",arrayOf(accountId)).use{if(it.moveToFirst())it.getInt(0) to it.getInt(1) else 0 to 0}}
    fun log(level:String,msg:String){writableDatabase.execSQL("INSERT INTO logs(level,message,created_at) VALUES(?,?,?)",arrayOf(level,msg,System.currentTimeMillis()))}
}
