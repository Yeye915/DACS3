package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import com.example.dacs3.databinding.ActivitySendRepairBinding
import com.google.android.material.button.MaterialButton
class SendRepairActivity : HouseActivity() {
    private lateinit var binding: ActivitySendRepairBinding
    private var room: Property?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivitySendRepairBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        binding.btnBack.setOnClickListener { finish() };binding.edtRepairRoom.isFocusable=false;binding.edtRepairRoom.hint="Chạm để chọn phòng đang thuê"
        fun select() { chooseProperty(false,false) { room=it;binding.edtRepairRoom.setText(it.title) } }
        binding.edtRepairRoom.setOnClickListener { select() };select()
        MaterialButton(this).apply { text="Theo dõi yêu cầu sửa chữa";(binding.btnSubmitRepair.parent as android.widget.LinearLayout).addView(this)
            setOnClickListener { startActivity(Intent(this@SendRepairActivity,RepairManagementActivity::class.java).putExtra("HOST_MODE",false)) } }
        binding.btnSubmitRepair.setOnClickListener {
            val chosen=room;val text=binding.edtRepairDescription.text.toString().trim()
            if(chosen==null) { notice("Hãy chọn phòng đang thuê");return@setOnClickListener }
            if(text.length !in 1..2000) { binding.edtRepairDescription.error="Mô tả từ 1 đến 2.000 ký tự";return@setOnClickListener }
            binding.btnSubmitRepair.isEnabled=false
            Backend.db.collection("repairs").add(mapOf("propertyId" to chosen.id,"propertyTitle" to chosen.title,"hostId" to chosen.hostId,"tenantUid" to Backend.auth.currentUser!!.uid,"description" to text,"status" to "Chờ xử lý","timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener { notice("Đã gửi báo cáo sự cố");finish() }.addOnFailureListener(::failure).addOnCompleteListener { binding.btnSubmitRepair.isEnabled=true }
        }
    }
}
