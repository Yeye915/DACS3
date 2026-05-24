package com.example.dacs3

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupBottomNavigation()
        setupRoleToggle()

        binding.btnEditAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.tvProfileName.text = doc.getString("fullName") ?: "Nhi"
                }
            }
    }

    private fun setupBottomNavigation() {
        // ĐÃ SỬA: Luôn bắt đầu sáng đèn ở mục Trang chủ
        binding.bottomNavProfile.selectedItemId = R.id.nav_home

        binding.bottomNavProfile.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_rooms -> {
                    Toast.makeText(this, "Tính năng Nhà/Phòng", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_blog -> {
                    startActivity(Intent(this, BlogActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                binding.ivAvatar.setImageURI(imageUri)
                Toast.makeText(this, "Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupRoleToggle() {
        // Lắng nghe sự kiện chuyển đổi nút
        binding.toggleRoleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSeekerMode -> {
                        // Hiện menu người thuê, ẩn menu chủ nhà
                        binding.layoutSeekerMenu.visibility = View.VISIBLE
                        binding.layoutHostMenu.visibility = View.GONE
                    }
                    R.id.btnHostMode -> {
                        // Hiện menu chủ nhà, ẩn menu người thuê
                        binding.layoutSeekerMenu.visibility = View.GONE
                        binding.layoutHostMenu.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}