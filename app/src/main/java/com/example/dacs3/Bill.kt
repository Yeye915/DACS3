package com.example.dacs3

data class Bill(
    var id: String = "",
    val roomName: String = "",    // Tên phòng trọ (vđ: Phòng 202)
    val month: String = "",       // Tháng đóng tiền (vđ: 06/2026)
    val electricityFee: Long = 0, // Tiền điện
    val waterFee: Long = 0,       // Tiền nước
    val roomFee: Long = 0,        // Tiền phòng gốc
    val totalAmount: Long = 0,    // Tổng tiền phải đóng
    val status: String = "Chưa thanh toán", // Trạng thái: Chưa thanh toán / Đã thanh toán
    val tenantUid: String = ""    // ID người thuê để lọc đúng hóa đơn của họ
)