package com.example.dacs3

data class News(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val likes: Int = 0,
    val comments: Int = 0 // Thêm "= 0" vào đây
)