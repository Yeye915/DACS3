package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegisterSubmit.setOnClickListener {
            performRegistration()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish() // Đóng màn hình đăng ký để quay về màn hình Login trước đó
        }
    }

    private fun performRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val pass = binding.etPassReg.text.toString().trim()
        val confirmPass = binding.etConfirmPass.text.toString().trim()
        val role = if (binding.rbLandlordReg.isChecked) "Chủ trọ" else "Khách thuê"

        // Kiểm tra điều kiện
        if (name.isEmpty() || phone.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPass) {
            binding.etConfirmPass.error = "Mật khẩu xác nhận không khớp"
            return
        }

        if (pass.length < 6) {
            binding.etPassReg.error = "Mật khẩu phải từ 6 ký tự trở lên"
            return
        }

        // Nếu mọi thứ ok, thông báo thành công
        val message = "Đăng ký thành công tài khoản $role: $name"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Sau khi đăng ký xong, thường sẽ chuyển về màn hình Login
        finish()
    }
}