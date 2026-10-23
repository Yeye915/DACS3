package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bannerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Cài đặt thành phần giao diện cố định
        setupBanner()

        // 2. Xử lý logic điều hướng Top Bar và Bottom Nav
        setupNavigationAndClicks()
    }

    override fun onStart() {
        super.onStart()
        // Đọc Online trực tiếp từ Firestore để cập nhật chính xác Tiện ích và Ô thống kê số lượng phòng
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
                    .addSnapshotListener { snapshots, _ ->
                        binding.txtStatValue1.text = (snapshots?.size() ?: 0).toString()
                    }

                // Đếm số phòng TRỐNG của chính chủ nhà này
                db.collection("properties")
                    .whereEqualTo("hostUid", currentUserUid)
                    .whereEqualTo("status", "Trống")
                    .addSnapshotListener { snapshots, _ ->
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
                .addSnapshotListener { snapshots, _ ->
                    binding.txtStatValue1.text = (snapshots?.size() ?: 0).toString()
                }

            // GIẢI PHÁP AN TOÀN: Lấy toàn bộ phòng về và đếm xem có bao nhiêu phòng ở Đà Nẵng
            db.collection("properties")
                .addSnapshotListener { snapshots, _ ->
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
            binding.tvTopRegister.visibility = View.VISIBLE
            binding.tvUserNameTop.visibility = View.GONE

            val localRole = sharedPref.getString("user_role", "seeker") ?: "seeker"
            setupFeaturesLayout(localRole)
            updateStatistics(localRole)
        }
    }

    private fun setupFeaturesLayout(role: String) {
        val features = if (role == "host") {
            listOf(
                Feature(1, "Tạo Hóa Đơn", android.R.drawable.ic_menu_add),
                Feature(2, "Quản Lý Tài Sản", android.R.drawable.ic_menu_myplaces),
                Feature(3, "Xem Phòng", android.R.drawable.ic_menu_sort_by_size),
                Feature(4, "Yêu Cầu Sửa Chữa", android.R.drawable.ic_menu_agenda),
                Feature(5, "Bàn Giao Phòng", android.R.drawable.ic_dialog_alert)
            )
        } else {
            listOf(
                Feature(1, "Hóa Đơn Của Tôi", android.R.drawable.ic_dialog_email),
                Feature(2, "Xem Phòng Đã Thuê", android.R.drawable.ic_menu_myplaces),
                Feature(3, "Khám Phá Phòng Trống", android.R.drawable.ic_menu_sort_by_size),
                Feature(4, "Báo Cáo Sự Cố", android.R.drawable.ic_menu_agenda),
                Feature(5, "Nhận Bàn Giao", android.R.drawable.ic_dialog_alert)
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
        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatures.adapter = adapter
    }

    private fun setupBanner() {
        val bannerImages = listOf(R.drawable.background, R.drawable.background1, R.drawable.background2, R.drawable.background3)
        binding.viewPagerBanner.adapter = BannerAdapter(bannerImages)

        bannerRunnable = object : Runnable {
            override fun run() {
                if (::binding.isInitialized && binding.viewPagerBanner != null) {
                    val currentItem = binding.viewPagerBanner.currentItem
                    val nextItem = if (currentItem == bannerImages.size - 1) 0 else currentItem + 1
                    binding.viewPagerBanner.setCurrentItem(nextItem, true)
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.postDelayed(bannerRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bannerRunnable.isInitialized) {
            handler.removeCallbacks(bannerRunnable)
        }
    }
}