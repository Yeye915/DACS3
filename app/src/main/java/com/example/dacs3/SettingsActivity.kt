package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import com.example.dacs3.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : HouseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nút quay lại màn hình Profile
        binding.btnBack.setOnClickListener { finish() }

        // Bật tắt nhận thông báo đơn giản
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Đã bật nhận thông báo hệ thống", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Đã tắt nhận thông báo hệ thống", Toast.LENGTH_SHORT).show()
            }
        }

        // Click đổi mật khẩu bằng cơ chế gửi link về Email của Firebase Auth cực kỳ an toàn
        binding.btnChangePassword.setOnClickListener {
            val userEmail = auth.currentUser?.email
            if (userEmail != null) {
                auth.sendPasswordResetEmail(userEmail)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã gửi liên kết đổi mật khẩu vào Email của bạn!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Không tìm thấy thông tin email tài khoản!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}