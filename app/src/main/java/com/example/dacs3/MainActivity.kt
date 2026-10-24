package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : HouseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Xử lý logic điều hướng Top Bar và Bottom Nav
        setupNavigationAndClicks()
    }

    override fun onStart() {
        super.onStart()
        val cachedRole = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getString("user_role", "seeker") ?: "seeker"
        setupFeaturesLayout(cachedRole)
        fetchUserRoleAndSetupFeatures()
    }

    private fun updateStatistics(role: String) {
        if (!::binding.isInitialized || binding.cardStatistics == null) return
        val currentUserUid = auth.currentUser?.uid

        if (role == "host") {
            binding.txtStatLabel1.text = "Đã thuê"
            binding.txtStatLabel2.text = "Chưa thuê"

            if (currentUserUid != null) {
                // Đếm số phòng ĐÃ THUÊ của chính chủ nhà này
                db.collection("properties")
                    .whereEqualTo("hostUid", currentUserUid)
                    .whereEqualTo("status", "Đã thuê")
                    .addSnapshotListener(this) { snapshots, _ ->
                        binding.txtStatValue1.text = (snapshots?.size() ?: 0).toString()
                    }

                // Đếm số phòng TRỐNG của chính chủ nhà này
                db.collection("properties")
                    .whereEqualTo("hostUid", currentUserUid)
                    .whereEqualTo("status", "Trống")
                    .addSnapshotListener(this) { snapshots, _ ->
                        binding.txtStatValue2.text = (snapshots?.size() ?: 0).toString()
                    }
            }
        } else {
            // VAI TRÒ NGƯỜI THUÊ
            binding.txtStatLabel1.text = "Phòng trống"
            binding.txtStatLabel2.text = "Khu vực"

            // Đếm tổng số phòng trống trên toàn bộ hệ thống
            db.collection("properties")
                .whereEqualTo("status", "Trống")
                .addSnapshotListener(this) { snapshots, _ ->
                    binding.txtStatValue1.text = (snapshots?.size() ?: 0).toString()
                }

            // GIẢI PHÁP AN TOÀN: Lấy toàn bộ phòng về và đếm xem có bao nhiêu phòng ở Đà Nẵng
            db.collection("properties")
                .addSnapshotListener(this) { snapshots, _ ->
                    var danangCount = 0
                    if (snapshots != null) {
                        for (doc in snapshots) {
                            val rentalAddress = doc.getString("rental") ?: ""
                            val cityField = doc.getString("city") ?: ""
                            // Nếu địa chỉ chứa chữ "Lê Độ" hoặc "Đà Nẵng" thì tự động tính vào khu vực Đà Nẵng
                            if (cityField == "Đà Nẵng" || rentalAddress.lowercase().contains("lê độ")) {
                                danangCount++
                            }
                        }
                    }
                    binding.txtStatValue2.text = "Đà Nẵng ($danangCount)"
                }
        }
    }

    private fun setupNavigationAndClicks() {
        binding.btnExploreRooms.setOnClickListener {
            startActivity(Intent(this, PropertyActivity::class.java))
        }
        binding.btnHomeSupport.setOnClickListener {
            startActivity(Intent(this, ChatAIActivity::class.java))
        }
        binding.bottomNav.selectedItemId = R.id.nav_home

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_rooms -> {
                    startActivity(Intent(this, PropertyActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_blog -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.tvTopRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvTopLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun fetchUserRoleAndSetupFeatures() {
        val currentUser = auth.currentUser
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        if (currentUser != null) {
            binding.tvTopLogin.visibility = View.GONE
            binding.tvTopRegister.visibility = View.GONE
            binding.tvUserNameTop.visibility = View.VISIBLE

            // ĐỌC ONLINE: Đảm bảo đăng nhập nick nào là nhảy giao diện tiện ích nick đó ngay lập tức
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("fullName") ?: "Người dùng"
                        binding.tvUserNameTop.text = " $userName"

                        val role = document.getString("role") ?: "seeker"
                        sharedPref.edit().putString("user_role", role).apply()

                        setupFeaturesLayout(role)
                        updateStatistics(role)
                    }
                }
                .addOnFailureListener {
                    val localRole = sharedPref.getString("user_role", "seeker") ?: "seeker"
                    setupFeaturesLayout(localRole)
                    updateStatistics(localRole)
                }
        } else {
            binding.tvTopLogin.visibility = View.VISIBLE
            // Keep the brand and login reachable on narrow screens with large text.
            // Registration is also available from the login screen.
            val compactHeader = resources.configuration.fontScale > 1.15f &&
                resources.configuration.screenWidthDp < 360
            binding.tvTopRegister.visibility = if (compactHeader) View.GONE else View.VISIBLE
            binding.tvUserNameTop.visibility = View.GONE

            val localRole = sharedPref.getString("user_role", "seeker") ?: "seeker"
            setupFeaturesLayout(localRole)
            updateStatistics(localRole)
        }
    }

    private fun setupFeaturesLayout(role: String) {
        val features = if (role == "host") {
            listOf(
                Feature(1, "Tạo hóa đơn", R.drawable.ic_ui_receipt),
                Feature(2, "Quản lý tài sản", R.drawable.ic_ui_assets),
                Feature(3, "Danh sách phòng", R.drawable.ic_ui_home),
                Feature(4, "Yêu cầu sửa chữa", R.drawable.ic_ui_repair),
                Feature(5, "Bàn giao phòng", R.drawable.ic_ui_key)
            )
        } else {
            listOf(
                Feature(1, "Hóa đơn của tôi", R.drawable.ic_ui_receipt),
                Feature(2, "Phòng đang thuê", R.drawable.ic_ui_home),
                Feature(3, "Khám phá phòng", R.drawable.ic_ui_explore),
                Feature(4, "Báo cáo sự cố", R.drawable.ic_ui_repair),
                Feature(5, "Nhận bàn giao", R.drawable.ic_ui_key)
            )
        }
        renderRecyclerView(features, role)
    }

    private fun renderRecyclerView(features: List<Feature>, role: String) {
        val adapter = FeatureAdapter(features) { selectedFeature ->
            when (selectedFeature.id) {
                1 -> {
                    if (role == "host") {
                        startActivity(Intent(this, AddBillActivity::class.java))
                    } else {
                        // NGƯỜI THUÊ: Kích hoạt mở trang xem hóa đơn thực tế
                        startActivity(Intent(this, TenantBillActivity::class.java))
                    }
                }
                2 -> {
                    if (role == "host") {
                        // THAY THẾ DIALOG CŨ: Mở thẳng màn hình quản lý tổng thể tài sản sang trọng
                        startActivity(Intent(this, AssetDashboardActivity::class.java))
                    } else {
                        // NGƯỜI THUÊ: Mở trang xem danh sách phòng trọ đã thuê thực tế
                        startActivity(Intent(this, RentedRoomsActivity::class.java))
                    }
                }

                3 -> {
                    startActivity(Intent(this, PropertyActivity::class.java))
                }
                4 -> {
                    if (role == "host") {
                        startActivity(Intent(this, RepairManagementActivity::class.java))
                    } else {
                        // NGƯỜI THUÊ: Kích hoạt mở trang gửi báo cáo sự cố lên Firebase thực tế
                        startActivity(Intent(this, SendRepairActivity::class.java))
                    }
                }
                5 -> {
                    if (role == "host") {
                        startActivity(Intent(this, HandoverManagementActivity::class.java))
                    } else {
                        // NGƯỜI THUÊ: Mở trang nhận bàn giao phòng online thực tế
                        startActivity(Intent(this, TenantHandoverActivity::class.java))
                    }
                }
                else -> {
                    Toast.makeText(this, "Tính năng: ${selectedFeature.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (features.size % 2 == 1 && position == features.lastIndex) 2 else 1
            }
        }
        binding.rvFeatures.adapter = adapter
    }

}
