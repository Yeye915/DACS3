package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
class NotificationListActivity : FlowActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen("Thông báo","Cập nhật yêu cầu thuê, hóa đơn và sửa chữa trong ứng dụng.")
        if(!requireAccount()) { finish();return }
        val uid=Backend.auth.currentUser!!.uid
        if(!getSharedPreferences("AppPrefs",MODE_PRIVATE).getBoolean("notifications_$uid",true)) {
            state.text="Bạn đã tắt hiển thị thông báo trong ứng dụng."
            action("Mở cài đặt") { startActivity(Intent(this,SettingsActivity::class.java)) };return
        }
        Backend.db.collection("Users").document(uid).get().addOnSuccessListener { profile ->
            val host=profile.getString("role")=="host";val field=if(host) "hostId" else "tenantUid"
            fun group(collection: String,title: String,target: Class<out android.app.Activity>,display: (com.google.firebase.firestore.DocumentSnapshot)->String) {
                label(title,20f);val section=LinearLayout(this).apply { orientation=1;body.addView(this) }
                observe(Backend.db.collection(collection).whereEqualTo(field,uid)) { data ->
                    state.text="";section.removeAllViews();if(data.isEmpty) label("Chưa có cập nhật",14f,section)
                    data.documents.takeLast(20).forEach { doc -> action(display(doc),section) { startActivity(Intent(this,target).putExtra("HOST_MODE",host)) } }
                }
            }
            group("rental_requests","Yêu cầu thuê",RentalRequestsActivity::class.java) { "${it.getString("roomName")} • ${when(it.getString("status")) { "pending"->"Chờ duyệt";"accepted"->"Đã chấp thuận";"cancelled"->"Đã hủy";else->"Đã từ chối" }}" }
            group("bills","Hóa đơn",if(host) HostWalletActivity::class.java else TenantBillActivity::class.java) { "${it.getString("roomName")} • ${it.getString("month")} • ${it.getString("status")}" }
            group("repairs","Sửa chữa",RepairManagementActivity::class.java) { "${it.getString("propertyTitle")} • ${it.getString("status")}" }
        }.addOnFailureListener(::failure)
    }
}
