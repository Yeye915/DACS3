package com.example.dacs3

// File: MainActivity.kt
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Khai báo ViewBinding an toàn (tránh NullPointer)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // 1. Chuẩn bị dữ liệu mô phỏng lấy từ trang chủ ITRO
        // (Sử dụng icon mặc định của Android để bạn không cần tải ảnh ngoài vào)
        val featureList = listOf(
            Feature(1, "Quản lý nhà/phòng", android.R.drawable.ic_dialog_map),
            Feature(2, "Quản lý khách thuê", android.R.drawable.ic_menu_myplaces),
            Feature(3, "Quản lý tài chính", android.R.drawable.ic_menu_sort_by_size),
            Feature(4, "Quản lý tài sản", android.R.drawable.ic_menu_agenda),
            Feature(5, "Quản lý sự cố", android.R.drawable.ic_dialog_alert),
            Feature(6, "Cài đặt & Thống kê", android.R.drawable.ic_menu_preferences)
        )

        // 2. Khởi tạo Adapter và truyền Lambda lắng nghe sự kiện Click
        val adapter = FeatureAdapter(featureList) { selectedFeature ->
            // Khi sinh viên bấm vào 1 ô, nó sẽ hiển thị Toast thông báo
            Toast.makeText(this, "Bạn chọn: ${selectedFeature.title}", Toast.LENGTH_SHORT).show()
        }

        // 3. Gắn Adapter vào RecyclerView và thiết lập Layout hiển thị dạng Grid (2 cột)
        binding.rvFeatures.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatures.adapter = adapter
    }
}