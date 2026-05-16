package com.example.dacs3

import android.content.Intent
import android.os.Bundle
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

        setupRecyclerView()
        setupTopNavigation() // Tách riêng phần xử lý menu trên cùng cho sạch code
    }

    private fun setupTopNavigation() {
        // Fix lỗi: Gọi đúng ID của từng mục thay vì binding.root
        binding.tvTopBlog.setOnClickListener {
            Toast.makeText(this, "Mở trang Blog House", Toast.LENGTH_SHORT).show()
        }

        binding.tvTopLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.tvTopRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Chỉ giữ lại các tính năng quản lý cốt lõi trong Grid
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