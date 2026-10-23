package com.example.dacs3

data class NotificationItem(
    var id: String = "",
    val title: String = "",       // Tiêu đề (vđ: Biên bản bàn giao mới)
    val content: String = "",     // Nội dung chi tiết
    val timestamp: Long = 0,      // Thời gian gửi thông báo
    val userUid: String = ""      // UID của người nhận (để lọc đúng thông báo của từng người)
)