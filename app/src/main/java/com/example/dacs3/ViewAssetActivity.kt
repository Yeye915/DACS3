package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityViewAssetBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ViewAssetActivity : HouseActivity() {

    private lateinit var binding: ActivityViewAssetBinding
    private lateinit var db: FirebaseFirestore
    private val savedRoomList = mutableListOf<RoomAssetGroup>()
    private lateinit var groupAdapter: RoomAssetGroupAdapter // Đổi sang Group Adapter mới

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAssetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Thiết lập RecyclerView hiển thị theo nhóm phòng
        groupAdapter = RoomAssetGroupAdapter(savedRoomList)
        binding.rvViewAssetList.layoutManager = LinearLayoutManager(this)
        binding.rvViewAssetList.adapter = groupAdapter

        loadAssetsFromFirestore()
    }

    private fun loadAssetsFromFirestore() {
        db.collection("PropertyAssets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                savedRoomList.clear()
                for (document in documents) {
                    // Đọc mã phòng (nếu lúc lưu Nhi đặt tên field là roomCode hoặc mã phòng tương tự)
                    // Ở đây mình tạm lấy trường "roomCode", nếu trong hashmap lúc lưu Nhi đặt tên khác thì sửa lại chuỗi này nhé
                    val roomCode = document.getString("roomCode") ?: document.id // Nếu không tìm thấy field thì lấy tạm ID của Document làm tên phòng
                    val status = document.getString("status") ?: "Trống"

                    val assetsMap = document.get("assets") as? List<Map<String, Any>>
                    val assetsOfThisRoom = mutableListOf<Asset>()

                    assetsMap?.forEach { map ->
                        val asset = Asset(
                            name = map["name"] as? String ?: "",
                            quantity = (map["quantity"] as? Long)?.toInt() ?: 0,
                            status = map["status"] as? String ?: "",
                            buyDate = map["buyDate"] as? String ?: "",
                            value = map["value"] as? String ?: "",
                            note = map["note"] as? String ?: "",
                            quality = map["quality"] as? String ?: ""
                        )
                        assetsOfThisRoom.add(asset)
                    }

                    // Thêm cả cụm phòng và danh sách tài sản của nó vào list hiển thị
                    savedRoomList.add(RoomAssetGroup(roomCode, status, assetsOfThisRoom))
                }
                groupAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}