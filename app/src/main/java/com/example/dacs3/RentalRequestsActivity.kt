package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class RentalRequestsActivity : FlowActivity() {
    private var section: android.widget.LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host=intent.getBooleanExtra("HOST_MODE",false)
        screen(if(host) "Duyệt yêu cầu thuê" else "Yêu cầu thuê của tôi","Gửi yêu cầu → chủ nhà chấp thuận → kiểm tra và nhận bàn giao")
        if(!requireAccount()) { finish();return }
        val uid=Backend.auth.currentUser!!.uid
        observe(Backend.db.collection("rental_requests").whereEqualTo(if(host) "hostId" else "tenantUid",uid)) { snapshot ->
            section?.let { body.removeView(it) };section=android.widget.LinearLayout(this).apply { orientation=1;body.addView(this) }
            snapshot.documents.sortedByDescending { it.getTimestamp("createdAt") }.forEach { doc ->
                val c=card(section!!)
                val status=doc.getString("status").orEmpty()
                label(doc.getString("roomName").orEmpty(),20f,c)
                if(host) label("Người gửi: ${doc.getString("tenantName").orEmpty().ifBlank { "Tài khoản người thuê" }}",14f,c)
                label(when(status) { "pending"->"Đang chờ chủ nhà duyệt";"accepted"->"Đã chấp thuận • Kiểm tra và nhận bàn giao";"cancelled"->"Người thuê đã hủy yêu cầu";else->"Chủ nhà đã từ chối yêu cầu" },14f,c)
                action("Xem phòng và vị trí",c) { openRoom(doc.getString("propertyId").orEmpty()) }
                action("Liên hệ ${if(host) "người thuê" else "chủ nhà"}",c) {
                    Backend.db.collection("properties").document(doc.getString("propertyId")!!).get().addOnSuccessListener { room ->
                        if(room.exists()) startActivity(Intent(this,ChatActivity::class.java).putExtra("CHOSEN_PROPERTY",Property.from(room)).putExtra("PEER_UID",doc.getString(if(host) "tenantUid" else "hostId")))
                    }.addOnFailureListener(::failure)
                }
                if(status=="pending") {
                    if(host) {
                        action("Chấp thuận thuê phòng",c) {
                            AlertDialog.Builder(this).setTitle("Chấp thuận yêu cầu?")
                                .setMessage("Phòng sẽ được gắn với người thuê này. Các yêu cầu khác không thể được chấp thuận khi phòng đang có người thuê.")
                                .setNegativeButton("Hủy",null).setPositiveButton("Chấp thuận") { _,_ -> RentalStore.decideRequest(doc.id,true).addOnSuccessListener { notice("Đã chấp thuận và tạo biên bản bàn giao") }.addOnFailureListener(::failure) }.show()
                        }
                        action("Từ chối yêu cầu",c) { AlertDialog.Builder(this).setTitle("Từ chối yêu cầu này?").setNegativeButton("Hủy",null).setPositiveButton("Từ chối") { _,_ -> RentalStore.decideRequest(doc.id,false).addOnFailureListener(::failure) }.show() }
                    } else action("Hủy yêu cầu thuê",c) {
                        AlertDialog.Builder(this).setTitle("Hủy yêu cầu thuê?").setNegativeButton("Giữ lại",null).setPositiveButton("Hủy yêu cầu") { _,_ ->
                            val ref=doc.reference
                            Backend.db.runTransaction { tx -> require(tx.get(ref).getString("status")=="pending") { "Yêu cầu đã được xử lý" };tx.update(ref,"status","cancelled");null }.addOnFailureListener(::failure)
                        }.show()
                    }
                }
                if(status=="accepted") action(if(host) "Theo dõi bàn giao" else "Kiểm tra và nhận bàn giao",c) { startActivity(Intent(this,if(host) HandoverManagementActivity::class.java else TenantHandoverActivity::class.java)) }
            }
        }
    }
    private fun openRoom(id: String) { Backend.db.collection("properties").document(id).get().addOnSuccessListener { if(it.exists()) startActivity(Intent(this,PropertyDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",Property.from(it))) else notice("Phòng không còn tồn tại") }.addOnFailureListener(::failure) }
}
