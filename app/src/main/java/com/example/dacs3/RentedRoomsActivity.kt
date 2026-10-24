package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
class RentedRoomsActivity : FlowActivity() {
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen("Phòng đang thuê","Xem chi tiết, bản đồ và liên hệ chủ nhà.")
        action("Yêu cầu thuê của tôi") { startActivity(Intent(this,RentalRequestsActivity::class.java)) }
        if(!requireAccount()) { finish();return }
        observe(Backend.db.collection("properties").whereEqualTo("tenantUid",Backend.auth.currentUser!!.uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            result.documents.map(Property::from).forEach { room ->
                val c=card(section!!);label(room.title,20f,c);label("${room.address}\n${RentalValidation.format(room.rent)}/tháng",14f,c)
                action("Xem chi tiết phòng",c) { startActivity(Intent(this,PropertyDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",room)) }
                action("Xem bản đồ",c) { startActivity(Intent(this,MapActivity::class.java).putExtra("CHOSEN_PROPERTY",room)) }
                action("Tài sản bàn giao",c) { startActivity(Intent(this,ViewAssetActivity::class.java).putExtra("CHOSEN_PROPERTY",room)) }
            }
        }
    }
}
