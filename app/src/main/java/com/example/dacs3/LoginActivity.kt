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
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sử dụng Firebase Auth để đăng nhập thật
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    // Trong LoginActivity.kt -> phần auth.signInWithEmailAndPassword().addOnCompleteListener
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            // Lấy role từ Firestore về để lưu tạm vào máy máy
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("Users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "seeker"

                                    // Lưu vào SharedPreferences để MainActivity đọc được
                                    val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    sharedPref.edit().putString("user_role", role).apply()

                                    // Chuyển màn hình
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    // Nếu lỗi mạng không lấy được dữ liệu, vẫn cho vào màn hình chính với quyền mặc định
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    }
                }
        }

        binding.tvGoToRegister.setOnClickListener {
            // Mở màn hình đăng ký
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}