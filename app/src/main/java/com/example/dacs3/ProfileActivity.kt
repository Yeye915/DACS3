package com.example.dacs3

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.dacs3.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : HouseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = Backend.auth
    private val db = Backend.db
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val signedIn = auth.currentUser != null
        binding.toggleRoleGroup.visibility = if (signedIn) View.VISIBLE else View.GONE
        binding.btnEditAvatar.visibility = if (signedIn) View.VISIBLE else View.GONE
        binding.tvProfileSubtitle.setText(if (signedIn) R.string.profile_description else R.string.profile_guest_description)
        binding.btnGuestLogin.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.btnLogout.visibility = if (signedIn) View.VISIBLE else View.GONE
        binding.btnGuestLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        com.google.android.material.button.MaterialButton(this).apply {
            text="Phòng đã lưu";isAllCaps=false;binding.layoutSeekerMenu.addView(this)
            setOnClickListener { if(requireAccount()) startActivity(Intent(this@ProfileActivity,PropertyActivity::class.java).putExtra("FAVORITES_ONLY",true)) }
        }
        com.google.android.material.button.MaterialButton(this).apply {
            text="Yêu cầu thuê của tôi";isAllCaps=false;binding.layoutSeekerMenu.addView(this)
            setOnClickListener { if(requireAccount()) startActivity(Intent(this@ProfileActivity,RentalRequestsActivity::class.java)) }
        }
        com.google.android.material.button.MaterialButton(this).apply {
            text="Duyệt yêu cầu thuê";isAllCaps=false;binding.layoutHostMenu.addView(this)
            setOnClickListener { if(requireAccount()) startActivity(Intent(this@ProfileActivity,RentalRequestsActivity::class.java).putExtra("HOST_MODE",true)) }
        }
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
            getSharedPreferences("AppPrefs",MODE_PRIVATE).edit().remove("user_role").remove("role_uid").apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        binding.btnPersonalInfo.setOnClickListener {
            // Lệnh chuyển sang trang EditProfileActivity
            val intent = Intent(this, if (auth.currentUser == null) LoginActivity::class.java else EditProfileActivity::class.java)
            startActivity(intent)
        }
        binding.btnMessages.setOnClickListener {
            startActivity(Intent(this, if (auth.currentUser == null) LoginActivity::class.java else ConversationListActivity::class.java))
        }
        // Kết nối nút hỗ trợ với trang AI mới
        binding.btnSupport.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            startActivity(intent)
        }
        binding.btnNotifications.setOnClickListener {
            val intent = Intent(this, NotificationListActivity::class.java)
            startActivity(intent)
        }
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        // Đọc lại role đã lưu để hiển thị đúng nút khi vừa vào trang
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "seeker").takeIf { sharedPref.getString("role_uid",null)==auth.currentUser?.uid && auth.currentUser!=null } ?: "seeker"

        if (role == "host") {
            binding.toggleRoleGroup.check(R.id.btnHostMode)
            binding.layoutHostMenu.visibility = View.VISIBLE
            binding.layoutSeekerMenu.visibility = View.GONE
        } else {
            binding.toggleRoleGroup.check(R.id.btnSeekerMode)
            binding.layoutSeekerMenu.visibility = View.VISIBLE
            binding.layoutHostMenu.visibility = View.GONE
        }


    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.tvProfileName.text = doc.getString("fullName") ?: getString(R.string.profile_title)
                    val host=doc.getString("role")=="host"
                    binding.toggleRoleGroup.check(if(host) R.id.btnHostMode else R.id.btnSeekerMode)
                    binding.layoutHostMenu.visibility=if(host) View.VISIBLE else View.GONE
                    binding.layoutSeekerMenu.visibility=if(host) View.GONE else View.VISIBLE
                    com.bumptech.glide.Glide.with(this).load(doc.getString("avatarUrl")).placeholder(R.drawable.ic_ui_person).error(R.drawable.ic_ui_person).into(binding.ivAvatar)
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
                binding.btnEditAvatar.isEnabled=false
                ImageUpload.upload(this,imageUri).continueWithTask { upload ->
                    db.collection("Users").document(auth.currentUser!!.uid).update(mapOf("avatarUrl" to upload.result,"password" to com.google.firebase.firestore.FieldValue.delete()))
                }.addOnSuccessListener { loadUserData();notice("Đã lưu ảnh đại diện") }.addOnFailureListener(::failure)
                    .addOnCompleteListener { binding.btnEditAvatar.isEnabled=true }
            }
        }
    }

    override fun onResume() {
        super.onResume();if(!::binding.isInitialized) return
        val signed=auth.currentUser!=null
        binding.toggleRoleGroup.visibility=if(signed) View.VISIBLE else View.GONE
        binding.btnEditAvatar.visibility=if(signed) View.VISIBLE else View.GONE
        binding.btnGuestLogin.visibility=if(signed) View.GONE else View.VISIBLE
        binding.btnLogout.visibility=if(signed) View.VISIBLE else View.GONE
        binding.tvProfileSubtitle.setText(if(signed) R.string.profile_description else R.string.profile_guest_description)
        loadUserData()
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