package com.example.dacs3

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityAddPropertyDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class PropertyHomeDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityAddPropertyDetailBinding

    // ĐỔI SANG dùng AssetItem để đồng bộ cấu trúc với Dashboard
    private val assetList = mutableListOf<AssetItem>()
    private lateinit var adapter: AssetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupSpinner()
        setupAssetList()

        // Bắt sự kiện bấm nút lưu tổng thể
        binding.btnSaveAll.setOnClickListener {
            saveDataToFirestore()
        }
        // Xử lý thêm đồ dùng tùy chỉnh vào danh sách bên dưới
        binding.btnAddCustomAsset.setOnClickListener {
            val customName = binding.edtCustomAssetName.text.toString().trim()
            if (customName.isEmpty()) {
                Toast.makeText(this, "Vui lòng gõ tên đồ dùng cần thêm!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tạo object mới add vào list tài sản tạm thời
            val newItem = AssetItem(
                id = "",
                assetName = customName,
                roomName = binding.edtRoomCode.text.toString().trim(), // Gán luôn mã phòng hiện tại
                quantity = 1,
                condition = "Tốt"
            )

            assetList.add(newItem)

            // Dùng lệnh này để cập nhật đồng bộ lại toàn bộ Adapter hiển thị lên màn hình hiện tại
            adapter.notifyDataSetChanged()

            binding.edtCustomAssetName.text.clear() // Xóa trống ô nhập chữ
        }
    }

    private fun setupSpinner() {
        val statuses = listOf("Trống", "Đã thuê", "Đang bảo trì")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        binding.spinnerStatus.adapter = adapterSpinner
    }

    private fun setupAssetList() {
        // Danh sách các tên vật tư mặc định của Nhi
        val defaultNames = listOf("Giường", "Tủ lạnh", "Máy lạnh", "Máy giặt", "Tủ quần áo", "Bàn ghế", "Quạt")

        assetList.clear()
        defaultNames.forEach { name ->
            // Ánh xạ chuẩn đét theo Model AssetItem mới
            assetList.add(AssetItem(
                id = "",
                assetName = name,
                roomName = "", // Sẽ được cập nhật khi điền tên phòng
                quantity = 1,
                condition = "Tốt"
            ))
        }

        // Nếu file AssetAdapter cũ của Nhi báo đỏ, hãy đổi kiểu dữ liệu nhận vào của Adapter thành List<AssetItem> nhé
        adapter = AssetAdapter(assetList)
        binding.rvAssetList.layoutManager = LinearLayoutManager(this)
        binding.rvAssetList.adapter = adapter
    }

    private fun saveDataToFirestore() {
        // 1. Lấy trạng thái phòng trọ từ Spinner
        val roomStatus = binding.spinnerStatus.selectedItem.toString()

        // 2. ĐỔI TỪ edtNote SANG edtRoomCode ĐỂ KHỚP VỚI XML CỦA NHI
        val inputRoomName = binding.edtRoomCode.text.toString().trim()

        // Kiểm tra nếu chủ nhà quên chưa nhập mã phòng
        if (inputRoomName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Mã phòng (VD: P101) để quản lý tài sản!", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Khởi tạo tiến trình lưu đồng bộ (Batch Write) ghi dữ liệu lên Firebase
        val batch = db.batch()

        assetList.forEach { asset ->
            // Tạo một document ID ngẫu nhiên mới trong collection "assets" để hiển thị lên Dashboard
            val docRef = db.collection("assets").document()

            val assetData = hashMapOf(
                "assetName" to asset.assetName,  // Phải là "assetName", không được viết là "name" hay "AssetName"
                "roomName" to inputRoomName,    // Phải là "roomName"
                "quantity" to asset.quantity,    // Phải là "quantity"
                "condition" to asset.condition   // Phải là "condition"
            )
            batch.set(docRef, assetData)
        }

        // Thực thi lệnh gửi dữ liệu lên Firebase
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã đồng bộ dữ liệu vào kho tài sản tổng!", Toast.LENGTH_SHORT).show()
                finish() // Đóng màn hình quay về Dashboard tài sản
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi kết nối Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}