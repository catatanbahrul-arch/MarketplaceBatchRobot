package com.marketplacebatch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.ComponentActivity

class MainActivity:ComponentActivity(){
    private lateinit var db:RobotDb
    private lateinit var status:TextView
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);db=RobotDb(this);ui()}
    override fun onResume(){super.onResume();if(::db.isInitialized)refresh()}
    private fun ui(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,22,22,22)}
        root.addView(TextView(this).apply{text="🤖 MARKETPLACE BATCH ROBOT\nSatu klik • bergiliran";textSize=25f})
        status=TextView(this).apply{textSize=15f};root.addView(status)
        root.addView(Button(this).apply{text="👤 AKUN";setOnClickListener{accounts()}})
        root.addView(Button(this).apply{text="📝 TEMPLATE";setOnClickListener{template()}})
        root.addView(Button(this).apply{text="▶ MULAI BATCH";setOnClickListener{startBatch()}})
        root.addView(Button(this).apply{text="⏹ STOP";setOnClickListener{stopBatch()}})
        root.addView(Button(this).apply{text="♿ ACCESSIBILITY";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
        root.addView(Button(this).apply{text="🧪 DRY RUN ON/OFF";setOnClickListener{db.setSetting("dry_run",if(db.setting("dry_run","1")=="1")"0" else "1");refresh()}})
        root.addView(Button(this).apply{text="🔋 BATERAI";setOnClickListener{startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}})
        setContentView(root);refresh()
    }
    private fun refresh(){
        val accounts=db.accounts();val first=accounts.firstOrNull();val m=first?.let{db.metrics(it.id)}?:0 to 0
        status.text="Akun aktif: ${accounts.count{it.enabled}}\nDry run: ${db.setting("dry_run","1")}\nTemplate: ${if(db.setting("template").isBlank())"KOSONG" else "SIAP"}\nWhatsApp: ${db.setting("whatsapp").ifBlank{"belum diisi"}}\nContoh metrik akun pertama: ${m.first} sender / ${m.second} sudah dibalas\n\nUrutan:\n${accounts.joinToString("\n"){ "${it.position+1}. ${it.displayName} → ${it.packageName}" }}"
    }
    private fun accounts(){
        val a=db.accounts();android.app.AlertDialog.Builder(this).setTitle("Urutan akun").setItems(a.map{"${it.position+1}. ${it.displayName} • ${it.packageName}"}.toTypedArray(),null).setPositiveButton("Tambah"){_,_->addAccount()}.setNegativeButton("Tutup",null).show()
    }
    private fun addAccount(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val id=EditText(this).apply{hint="ID akun"};val name=EditText(this).apply{hint="Nama akun"};val pkg=EditText(this).apply{hint="Package Facebook instance";setText("com.facebook.katana")}
        box.addView(id);box.addView(name);box.addView(pkg)
        android.app.AlertDialog.Builder(this).setTitle("Tambah akun").setView(box).setPositiveButton("Simpan"){_,_->if(id.text.isNotBlank())db.upsertAccount(id.text.toString().trim(),if(name.text.isBlank())id.text.toString().trim() else name.text.toString().trim(),pkg.text.toString().trim(),db.accounts().size)}.setNegativeButton("Batal",null).show()
    }
    private fun template(){
        val input=EditText(this).apply{setText(db.setting("template"));minLines=5}
        android.app.AlertDialog.Builder(this).setTitle("Template").setMessage("Gunakan {{whatsapp}} untuk link WhatsApp.").setView(input).setPositiveButton("Simpan"){_,_->db.setSetting("template",input.text.toString());refresh()}.setNegativeButton("Batal",null).show()
    }
    private fun startBatch(){
        val s=MarketplaceBatchServiceRegistry.service
        if(s==null){Toast.makeText(this,"Aktifkan Accessibility dulu, lalu tekan Mulai Batch lagi",Toast.LENGTH_LONG).show();return}
        if(s.startBatch()) {
            db.setSetting("enabled","1")
        } else {
            Toast.makeText(this,"Accessibility Service belum siap. Buka pengaturan Accessibility lalu coba lagi.",Toast.LENGTH_LONG).show()
        }
    }
    private fun stopBatch(){MarketplaceBatchServiceRegistry.service?.stopBatch();db.setSetting("enabled","0");Toast.makeText(this,"Batch dihentikan",Toast.LENGTH_SHORT).show()}
}
