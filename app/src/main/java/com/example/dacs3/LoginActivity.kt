package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityLoginBinding

// LoginActivity.kt
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim() // .trim() để xóa khoảng trắng thừa
            val password = binding.etPassword.text.toString().trim()

            // 1. Kiểm tra không được để trống
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Logic phân quyền dựa trên Email
            val role: String
            val intent: Intent

            if (email == "chutro@gmail.com") {
                role = "Chủ trọ"
                // Chuyển đến màn hình Dashboard của Chủ trọ (MainActivity bạn đã làm)
                intent = Intent(this, MainActivity::class.java)
            } else {
                role = "Khách thuê"
                // Chuyển đến màn hình dành cho Khách thuê
                // (Nếu chưa có, bạn có thể tạm thời dùng MainActivity hoặc tạo mới TenantActivity)
                intent = Intent(this, MainActivity::class.java)
                // Sau này bạn thay MainActivity ở trên thành TenantActivity::class.java nhé
            }

            // 3. Thông báo và chuyển màn hình
            Toast.makeText(this, "Đăng nhập thành công với quyền $role", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish() // Đóng màn hình Login để người dùng không quay lại được bằng nút Back
        }

        binding.tvGoToRegister.setOnClickListener {
            // Mở màn hình đăng ký
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}