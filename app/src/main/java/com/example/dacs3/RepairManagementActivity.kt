package com.example.dacs3

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
class RepairManagementActivity : FlowActivity() {
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);val host=intent.getBooleanExtra("HOST_MODE",true)
        screen(if(host) "Xử lý sự cố" else "Tiến độ sửa chữa")
        if(!requireAccount()) { finish();return }
        observe(Backend.db.collection("repairs").whereEqualTo(if(host) "hostId" else "tenantUid",Backend.auth.currentUser!!.uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            result.documents.sortedByDescending { it.getLong("timestamp") }.forEach { doc ->
                val c=card(section!!);label(doc.getString("propertyTitle").orEmpty(),20f,c);label(doc.getString("description").orEmpty(),14f,c);label(doc.getString("status").orEmpty(),16f,c)
                if(host) action("Cập nhật tiến độ",c) { val statuses=arrayOf("Chờ xử lý","Đang sửa","Đã xong")
                    AlertDialog.Builder(this).setTitle("Trạng thái sửa chữa").setItems(statuses) { _,i -> doc.reference.update("status",statuses[i]).addOnFailureListener(::failure) }.show()
                }
            }
        }
    }
}
