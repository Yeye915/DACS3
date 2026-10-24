package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
class AssetDashboardActivity : FlowActivity() {
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen("Tài sản của tôi","Tài sản được gắn với mã phòng và UID chủ nhà.")
        if(!requireAccount()) { finish();return }
        action("Chọn phòng để quản lý tài sản") { chooseProperty(true,false) { startActivity(Intent(this,PropertyHomeDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",it)) } }
        observe(Backend.db.collection("assets").whereEqualTo("hostId",Backend.auth.currentUser!!.uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            result.documents.groupBy { it.getString("propertyId").orEmpty() }.forEach { (id,items) ->
                val c=card(section!!);label(items.first().getString("roomName").orEmpty(),20f,c)
                items.forEach { label("${it.getString("assetName")} • ${it.getLong("quantity")} • ${it.getString("condition")}",14f,c) }
                action("Chỉnh sửa danh sách",c) { Backend.db.collection("properties").document(id).get().addOnSuccessListener { if(it.exists()) startActivity(Intent(this,PropertyHomeDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",Property.from(it))) }.addOnFailureListener(::failure) }
            }
        }
    }
}
