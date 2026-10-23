package com.example.dacs3

data class Property(
    var id: String = "",            // THÊM: ID của phòng trọ trên Firestore
    val hostId: String = "",        // THÊM: UID của chủ căn nhà này để tạo hóa đơn
    val tenantUid: String = "",   // ID người đang thuê phòng này để gửi đúng hóa đơn
    val title: String = "",
    val rental: String = "",      // Địa chỉ
    val price: String = "",
    val location: String = "",
    val bedroom: Int = 0,
    val bathroom: Int = 0,
    val area: String = "",
    val imageUrl: String = "",
    val rating: Float = 4f,
    val status: String = "Trống"    // THÊM: "Trống" hoặc "Đã thuê" để đồng bộ với bộ đếm trang chủ
) : java.io.Serializable // THÊM DÒNG NÀY ĐỂ TRUYỀN DỮ LIỆU SANG ADDBILLACTIVITY ĐƯỢC NHA