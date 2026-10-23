package com.example.dacs3

data class HandoverRequest(
    var id: String = "",
    val roomName: String = "",       // Tên phòng cần bàn giao
    val tenantName: String = "",     // Tên khách thuê nhận bàn giao
    val assetCheck: String = "",     // Ghi chú trạng thái nội thất
    // Bên trong file HandoverRequest.kt của Nhi, hãy đảm bảo có trường này:
    val hostName: String = "",

    // ĐỔI TÊN BIẾN: Sửa từ isCompleted thành completed để tránh lỗi mapping của Firebase
    @field:JvmField
    var completed: Boolean = false
)