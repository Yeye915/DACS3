package com.example.dacs3

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth // 1. Import thư viện Firebase Auth

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth // 2. Khai báo biến Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Khởi tạo đối tượng Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.btnRegisterSubmit.setOnClickListener {
            performRegistration()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val pass = binding.etPassReg.text.toString().trim()
        val confirmPass = binding.etConfirmPass.text.toString().trim()

        // Kiểm tra điều kiện bỏ trống
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra định dạng Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email không đúng định dạng"
            return
        }

        // Kiểm tra mật khẩu
        if (pass != confirmPass) {
            binding.etConfirmPass.error = "Mật khẩu xác nhận không khớp"
            return
        }

        if (pass.length < 6) {
            binding.etPassReg.error = "Mật khẩu phải từ 6 ký tự trở lên"
            return
        }

        // --- 4. TIẾN HÀNH ĐĂNG KÝ VỚI FIREBASE ---
        // Hiển thị thông báo đang xử lý để người dùng chờ
        Toast.makeText(this, "Đang xử lý đăng ký...", Toast.LENGTH_SHORT).show()

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                    // Tạo đối tượng dữ liệu để lưu
                    val userMap = hashMapOf(
                        "fullName" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to "User" // Mặc định là User
                    )

                    // Lưu vào Collection "Users" với ID là userId vừa tạo
                    if (userId != null) {
                        db.collection("Users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đăng ký và lưu dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Lỗi lưu dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}