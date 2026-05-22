package com.example.dacs3

data class Blog(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val author: String = "",
    val date: String = "",
    val imageUrl: String = "house1.jpg" // Nếu bạn muốn hiển thị ảnh minh họa
)