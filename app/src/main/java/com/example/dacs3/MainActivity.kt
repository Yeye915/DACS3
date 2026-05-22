package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Xử lý các sự kiện click cơ bản
        setupClickListeners()

        // 2. Cài đặt Banner (ViewPager2)
        setupBanner()

        // 3. Cài đặt danh sách tính năng (RecyclerView)
        setupRecyclerView()
    }

    private fun setupClickListeners() {
        binding.tvTopRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvTopLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_blog -> {
                    startActivity(Intent(this, BlogActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_home -> true
                else -> false
            }
        }
    }

    private fun setupBanner() {
        // Danh sách ảnh banner
        val bannerImages = listOf(
            R.drawable.background,
            R.drawable.background1,
            R.drawable.background2,
            R.drawable.background3
        )

        val adapter = BannerAdapter(bannerImages)
        binding.viewPagerBanner.adapter = adapter

        // Tự động chuyển ảnh sau 3 giây
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

    private fun setupRecyclerView() {
        val featureList = listOf(
            Feature(1, "Quản lý nhà/phòng", android.R.drawable.ic_dialog_map),
            Feature(2, "Quản lý khách thuê", android.R.drawable.ic_menu_myplaces),
            Feature(3, "Quản lý tài chính", android.R.drawable.ic_menu_sort_by_size),
            Feature(4, "Quản lý tài sản", android.R.drawable.ic_menu_agenda),
            Feature(5, "Quản lý sự cố", android.R.drawable.ic_dialog_alert),
            Feature(6, "Cài đặt & Thống kê", android.R.drawable.ic_menu_preferences)
        )

        val adapter = FeatureAdapter(featureList) { selectedFeature ->
            Toast.makeText(this, "Bạn chọn: ${selectedFeature.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatures.adapter = adapter
    }
}