package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

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

        // 1. Cài đặt các thành phần giao diện
        setupBanner()
        setupHouseRecyclerView()

        // 2. Xử lý logic người dùng và Navigation
        fetchUserRoleAndSetupFeatures()
        setupNavigationAndClicks()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "seeker")

        if (role == "host") {
            binding.btnContractWallet.text = "Ví hợp đồng (Chủ nhà)"
            binding.btnContractWallet.setOnClickListener {
                // Chuyển sang trang ví của chủ nhà
                val intent = Intent(this, HostWalletActivity::class.java)
                startActivity(intent)
            }
        } else {
            binding.btnContractWallet.text = "Ví hợp đồng (Người thuê)"
            binding.btnContractWallet.setOnClickListener {
                // Chuyển sang trang ví của người thuê
                val intent = Intent(this, TenantWalletActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupNavigationAndClicks() {
        // Mặc định mục Trang chủ sáng đèn
        binding.bottomNav.selectedItemId = R.id.nav_home

        // Xử lý sự kiện Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Đang ở Home rồi, không làm gì cả
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

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true}
                else -> false
            }
        }

        // Click listeners cho Top Bar
        binding.tvTopRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


    }

    private fun fetchUserRoleAndSetupFeatures() {
        val currentUser = auth.currentUser

        val allFeatures = listOf(
            Feature(1, "Hóa Đơn", android.R.drawable.ic_dialog_map),
            Feature(2, "Quản Lý Tài Sản", android.R.drawable.ic_menu_myplaces),
            Feature(3, "Thống Kê", android.R.drawable.ic_menu_sort_by_size),
            Feature(4, "Sự Cố Và Sửa Chữa", android.R.drawable.ic_menu_agenda),
            Feature(5, "Bàn Giao Tài Sản Số", android.R.drawable.ic_dialog_alert),
            Feature(6, "Ví Hợp Đồng", android.R.drawable.ic_menu_preferences)
        )

        if (currentUser != null) {
            binding.tvTopLogin.visibility = View.GONE
            binding.tvTopRegister.visibility = View.GONE
            binding.tvUserNameTop.visibility = View.VISIBLE

            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("fullName") ?: "Người dùng"
                        binding.tvUserNameTop.text = " $userName"
                    }
                    renderRecyclerView(allFeatures)
                }
                .addOnFailureListener {
                    renderRecyclerView(allFeatures)
                }
        } else {
            binding.tvTopLogin.visibility = View.VISIBLE
            binding.tvTopRegister.visibility = View.VISIBLE
            binding.tvUserNameTop.visibility = View.GONE
            renderRecyclerView(allFeatures)
        }
    }

    private fun setupHouseRecyclerView() {
        val sampleHouses = listOf(
            House(1, "Phòng trọ gác xép mới xây", "Gần đại học VKU, Đà Nẵng", "1.500.000 VNĐ/tháng", R.drawable.background),
            House(2, "Căn hộ mini Full nội thất", "Đường Trần Đại Nghĩa, Đà Nẵng", "2.800.000 VNĐ/tháng", R.drawable.background1),
            House(3, "Phòng trọ giá rẻ cho sinh viên", "Khu dân cư Nam cầu Cẩm Lệ", "1.200.000 VNĐ/tháng", R.drawable.background2)
        )

        val adapter = HouseAdapter(sampleHouses) { selectedHouse ->
            Toast.makeText(this, "Bạn đang xem: ${selectedHouse.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvHouses.layoutManager = LinearLayoutManager(this)
        binding.rvHouses.adapter = adapter
    }

    private fun renderRecyclerView(features: List<Feature>) {
        val adapter = FeatureAdapter(features) { selectedFeature ->
            when (selectedFeature.id) {
                6 -> { // ID của Ví Hợp Đồng là 6
                    val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    val role = sharedPref.getString("user_role", "seeker")

                    if (role == "host") {
                        startActivity(Intent(this, HostWalletActivity::class.java))
                    } else {
                        startActivity(Intent(this, TenantWalletActivity::class.java))
                    }
                }
                else -> {
                    Toast.makeText(this, "Bạn chọn: ${selectedFeature.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatures.adapter = adapter
    }

    private fun setupBanner() {
        val bannerImages = listOf(R.drawable.background, R.drawable.background1, R.drawable.background2, R.drawable.background3)
        binding.viewPagerBanner.adapter = BannerAdapter(bannerImages)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = binding.viewPagerBanner.currentItem
                val nextItem = if (currentItem == bannerImages.size - 1) 0 else currentItem + 1
                binding.viewPagerBanner.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)
    }
}