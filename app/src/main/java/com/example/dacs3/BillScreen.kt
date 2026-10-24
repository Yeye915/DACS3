package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
abstract class BillScreen : FlowActivity() {
    abstract val host: Boolean
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen(if(host) "Thu tiền phòng" else "Hóa đơn của tôi","Thanh toán trực tiếp hoặc chuyển khoản theo thỏa thuận. Ứng dụng ghi nhận xác nhận của hai bên.")
        if(!requireAccount()) { finish();return }
        if(host) action("Tạo hóa đơn mới") { startActivity(Intent(this,AddBillActivity::class.java)) }
        val uid=Backend.auth.currentUser!!.uid
        observe(Backend.db.collection("bills").whereEqualTo(if(host) "hostId" else "tenantUid",uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            val paid=result.documents.filter { it.getString("status")=="Đã thanh toán" }.sumOf { it.getLong("totalAmount") ?: 0L }
            val unpaid=result.documents.filter { it.getString("status")!="Đã thanh toán" }.sumOf { it.getLong("totalAmount") ?: 0L }
            label("Đã thanh toán: ${RentalValidation.format(paid)}\nCòn phải ${if(host) "thu" else "trả"}: ${RentalValidation.format(unpaid)}",18f,section!!)
            result.documents.sortedByDescending { it.getTimestamp("createdAt") }.forEach { doc ->
                val c=card(section!!);val status=doc.getString("status").orEmpty()
                label("${doc.getString("roomName")} • ${doc.getString("month")}",20f,c)
                label("Tiền phòng: ${RentalValidation.format(doc.getLong("roomFee") ?: 0)}\nĐiện: ${RentalValidation.format(doc.getLong("electricityFee") ?: 0)}\nNước: ${RentalValidation.format(doc.getLong("waterFee") ?: 0)}",14f,c)
                label("Tổng: ${RentalValidation.format(doc.getLong("totalAmount") ?: 0)}\n$status",18f,c)
                if(host && status=="Chờ xác nhận" || !host && status=="Chưa thanh toán") action(if(host) "Xác nhận đã nhận tiền" else "Báo đã thanh toán",c) {
                    AlertDialog.Builder(this).setTitle(if(host) "Bạn đã nhận đủ tiền?" else "Bạn đã thanh toán?")
                        .setMessage(if(host) "Chỉ xác nhận sau khi kiểm tra đã nhận đủ tiền thực tế. Thao tác này đánh dấu hóa đơn đã thanh toán." else "Thao tác này gửi báo thanh toán cho chủ nhà kiểm tra, không thực hiện chuyển tiền.")
                        .setNegativeButton("Hủy",null).setPositiveButton("Xác nhận") { _,_ -> RentalStore.payment(doc.id,host).addOnSuccessListener { notice("Đã cập nhật hóa đơn") }.addOnFailureListener(::failure) }.show()
                }
            }
        }
    }
}
