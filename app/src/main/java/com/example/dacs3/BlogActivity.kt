package com.example.dacs3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityBlogBinding

class BlogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. có thể lấy từ Firebase Firestore
        val listBlog = listOf(
            Blog("1", "Cách chọn phòng trọ an toàn", "Bạn nên kiểm tra hệ thống điện nước...", "Admin", "10/05/2026"),
            Blog("2", "Nội quy phòng trọ mới nhất", "Giờ giấc ra vào và quy định rác thải...", "Chủ trọ", "12/05/2026"),
            Blog("3", "Mẹo trang trí phòng trọ nhỏ", "Sử dụng gương và đèn led để tạo không gian...", "Nhi Trần", "15/05/2026")
        )

        // 2. Thiết lập RecyclerView
        binding.rvBlogList.layoutManager = LinearLayoutManager(this)
        binding.rvBlogList.adapter = BlogAdapter(listBlog)
    }
}