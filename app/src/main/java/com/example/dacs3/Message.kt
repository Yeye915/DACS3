package com.example.dacs3

data class Message(
    val senderId: String = "",
    val text: String = "",       // Đổi từ message thành text
    val senderName: String = "", // THÊM TRƯỜNG NÀY để hiển thị tên người gửi
    val time: Long = 0
)