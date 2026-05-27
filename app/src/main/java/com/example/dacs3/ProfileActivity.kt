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
        binding.btnPersonalInfo.setOnClickListener {
            // Lệnh chuyển sang trang EditProfileActivity
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
        // Kết nối nút hỗ trợ với trang AI mới
        binding.btnSupport.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            startActivity(intent)
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

        binding.bottomNavProfile.selectedItemId = R.id.nav_profile

        binding.bottomNavProfile.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_rooms -> {
                    // Nếu Nhi đã có PropertyActivity thì mở nó ra nhé
                    startActivity(Intent(this, PropertyActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish() // Đóng MainActivity để giải phóng bộ nhớ
                    true
                }
                R.id.nav_blog -> {
                    startActivity(Intent(this, NewsActivity::class.java))
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
        binding.toggleRoleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val editor = sharedPref.edit()

                when (checkedId) {
                    R.id.btnSeekerMode -> {
                        binding.layoutSeekerMenu.visibility = View.VISIBLE
                        binding.layoutHostMenu.visibility = View.GONE
                        // Lưu lại là đang ở chế độ người thuê
                        editor.putString("user_role", "seeker").apply()
                    }
                    R.id.btnHostMode -> {
                        binding.layoutSeekerMenu.visibility = View.GONE
                        binding.layoutHostMenu.visibility = View.VISIBLE
                        // Lưu lại là đang ở chế độ chủ nhà
                        editor.putString("user_role", "host").apply()
                    }
                }
            }
        }
    }
}