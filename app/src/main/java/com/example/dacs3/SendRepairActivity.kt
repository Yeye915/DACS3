package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import com.example.dacs3.databinding.ActivitySendRepairBinding
import com.google.firebase.firestore.FirebaseFirestore

class SendRepairActivity : HouseActivity() {

    private lateinit var binding: ActivitySendRepairBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendRepairBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nút quay lại
        binding.btnBack.setOnClickListener { finish() }

        // Sự kiện gửi báo cáo sự cố
        binding.btnSubmitRepair.setOnClickListener {
            val roomName = binding.edtRepairRoom.text.toString().trim()
            val description = binding.edtRepairDescription.text.toString().trim()

            if (roomName.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin sự cố!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Đóng gói dữ liệu dạng Map đẩy lên collection "repairs" trùng khớp cấu trúc của Chủ nhà
            val repairData = hashMapOf(
                "propertyTitle" to roomName,
                "description" to description,
                "status" to "Chờ xử lý",
                "timestamp" to System.currentTimeMillis()
            )

            binding.btnSubmitRepair.isEnabled = false // Khóa nút tránh gửi trùng lặp

            db.collection("repairs").add(repairData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Đã gửi báo cáo thành công! Chủ nhà sẽ xử lý sớm.", Toast.LENGTH_LONG).show()
                    finish() // Đóng màn hình quay về trang chủ
                }
                .addOnFailureListener { e ->
                    binding.btnSubmitRepair.isEnabled = true
                    Toast.makeText(this, "Lỗi khi gửi báo cáo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}