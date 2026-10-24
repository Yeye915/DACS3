package com.example.dacs3

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FieldValue

abstract class HandoverScreen : FlowActivity() {
    abstract val host: Boolean
    private var section: LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);screen(if(host) "Theo dõi bàn giao" else "Nhận bàn giao phòng","Chỉ người thuê được chỉ định có thể xác nhận đã nhận phòng.")
        if(!requireAccount()) { finish();return }
        val uid=Backend.auth.currentUser!!.uid
        observe(Backend.db.collection("handovers").whereEqualTo(if(host) "hostId" else "tenantUid",uid)) { result ->
            section?.let { body.removeView(it) };section=LinearLayout(this).apply { orientation=1;body.addView(this) }
            result.documents.forEach { doc ->
                val c=card(section!!);label(doc.getString("roomName").orEmpty(),20f,c);label(doc.getString("assetCheck").orEmpty(),14f,c)
                action("Xem tài sản bàn giao",c) {
                    val room=Property(id=doc.getString("propertyId").orEmpty(),title=doc.getString("roomName").orEmpty())
                    if(room.id.isNotBlank()) startActivity(android.content.Intent(this,ViewAssetActivity::class.java).putExtra("CHOSEN_PROPERTY",room))
                }
                val completed=doc.getBoolean("completed")==true;label(if(completed) "Người thuê đã ký nhận" else "Chờ người thuê kiểm tra và ký nhận",14f,c)
                if(!host && !completed) action("Xác nhận đã nhận phòng",c) {
                    AlertDialog.Builder(this).setTitle("Xác nhận bàn giao?").setMessage("Chỉ xác nhận sau khi đã kiểm tra thực tế phòng, danh sách tài sản và thỏa thuận với chủ nhà.")
                        .setNegativeButton("Chưa nhận",null).setPositiveButton("Đã kiểm tra và nhận phòng") { _,_ ->
                            Backend.db.runTransaction { tx ->
                                val current=tx.get(doc.reference);require(current.getString("tenantUid")==uid && current.getBoolean("completed")!=true)
                                tx.update(doc.reference,mapOf("completed" to true,"signedAt" to FieldValue.serverTimestamp()));null
                            }.addOnSuccessListener { notice("Đã xác nhận nhận phòng") }.addOnFailureListener(::failure)
                        }.show()
                }
            }
        }
    }
}
