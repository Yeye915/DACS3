package com.example.dacs3

import android.os.Bundle
class ViewAssetActivity : FlowActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen("Tài sản bàn giao","Đối chiếu danh sách này với tài sản thực tế trước khi ký nhận.")
        if(!requireAccount()) { finish();return }
        val room=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        if(room!=null) load(room) else chooseProperty(false,false,::load)
    }
    private fun load(room: Property) {
        label(room.title,20f)
        observe(Backend.db.collection("assets").whereEqualTo("propertyId",room.id)) { result ->
            val old=body.findViewWithTag<android.view.View>("assets");if(old!=null) body.removeView(old)
            val c=card().apply { (parent as android.view.View).tag="assets" }
            result.documents.forEach { label("${it.getString("assetName")} • ${it.getLong("quantity")} • ${it.getString("condition")}",16f,c) }
        }
    }
}
