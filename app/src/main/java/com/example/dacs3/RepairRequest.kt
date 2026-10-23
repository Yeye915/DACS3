package com.example.dacs3

data class RepairRequest(
    var id: String = "",
    val propertyTitle: String = "", // Tên phòng bị sự cố
    val description: String = "",   // Nội dung hư hỏng
    val status: String = "Chờ xử lý", // Trạng thái: Chờ xử lý, Đang sửa, Đã xong
    val timestamp: Long = 0
)