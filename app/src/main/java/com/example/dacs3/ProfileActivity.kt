package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Sửa import thành Firestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Gọi hàm thiết lập Bottom Nav
        setupBottomNavigation()

        loadProfileData()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNavigation() {
        // Để icon "Tôi" được sáng lên khi đang ở trang này
        binding.bottomNav.selectedItemId = R.id.nav_profile

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0) // Giúp chuyển cảnh mượt mà không bị giật
                    finish()
                    true
                }
                R.id.nav_blog -> {
                    startActivity(Intent(this, BlogActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_rooms -> {
                    // Nếu Nhi có trang danh sách phòng thì mở ở đây
                    true
                }
                R.id.nav_profile -> true // Đang ở đây rồi nên không làm gì
                else -> false
            }
        }
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Đã sửa lỗi: Sử dụng đúng FirebaseFirestore
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("Users").document(currentUser.uid)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Lấy dữ liệu từ Firestore
                    val name = document.getString("fullName") ?: "N/A"
                    val email = document.getString("email") ?: "N/A"
                    val phone = document.getString("phone") ?: "N/A"
                    val role = document.getString("role") ?: "User"

                    // Hiển thị lên giao diện
                    binding.tvProfileName.text = name
                    binding.tvProfileEmail.text = "Email: $email"
                    binding.tvProfilePhone.text = "Số điện thoại: $phone"
                    binding.tvProfileRole.text = "Vai trò: $role"
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}