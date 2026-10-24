package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.inputmethod.EditorInfo
import com.example.dacs3.databinding.ActivityLoginBinding

// LoginActivity.kt
class LoginActivity : HouseActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.etUsername.error = if (email.isEmpty()) getString(R.string.auth_required_email) else null
            binding.etPassword.error = if (password.isEmpty()) getString(R.string.auth_required_password) else null
            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener
            binding.btnLogin.isEnabled = false
            binding.btnLogin.setText(R.string.auth_logging_in)

            // Sử dụng Firebase Auth để đăng nhập thật
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.setText(R.string.auth_login)
                    if (!task.isSuccessful) {
                        Toast.makeText(this, R.string.auth_login_failed, Toast.LENGTH_LONG).show()
                    }
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

        binding.etPassword.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnLogin.performClick()
                true
            } else false
        }
        binding.tvGoToRegister.setOnClickListener {
            // Mở màn hình đăng ký
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}