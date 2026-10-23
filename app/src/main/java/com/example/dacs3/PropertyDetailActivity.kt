package com.example.dacs3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityPropertyDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding
    private lateinit var adapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()
    private lateinit var firestore: FirebaseFirestore
    private val auth = FirebaseAuth.getInstance()

    // Biến toàn cục lưu trữ thông tin phòng trọ nhận được
    private var currentProperty: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        // 1. NHẬN OBJECT PROPERTY TRUYỀN SANG
        currentProperty = intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property

        // Lấy các biến lẻ (Duy trì để hiển thị dữ liệu)
        val propertyId = currentProperty?.id ?: intent.getStringExtra("propertyId") ?: ""
        val hostId = currentProperty?.hostId ?: intent.getStringExtra("hostId") ?: ""
        val title = currentProperty?.title ?: intent.getStringExtra("title") ?: "Phòng trọ"
        val price = currentProperty?.price ?: intent.getStringExtra("price")
        val location = currentProperty?.location ?: intent.getStringExtra("location")
        val area = currentProperty?.area ?: intent.getStringExtra("area")
        val image = currentProperty?.imageUrl ?: intent.getStringExtra("image")
        val rating = currentProperty?.rating ?: intent.getFloatExtra("rating", 4f)

        // 2. SET DATA LÊN GIAO DIỆN
        binding.txtTitle.text = title
        binding.txtPrice.text = price
        binding.txtLocation.text = location
        binding.txtArea.text = area
        binding.ratingBar.rating = rating

        Glide.with(this)
            .load(image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(R.drawable.ic_house)
            .into(binding.imgProperty)

        binding.btnBack.setOnClickListener { finish() }

        // 3. XỬ LÝ ẨN/HIỆN PHÂN QUYỀN NÚT BẤM THEO VAI TRÒ (CHỦ NHÀ vs NGƯỜI THUÊ)
        val currentUid = auth.currentUser?.uid
        if (currentUid != null && currentUid == hostId) {
            binding.btnRentNow.visibility = View.GONE       // Chủ nhà: Ẩn nút Thuê ngay
            binding.btnCreateBill.visibility = View.VISIBLE // Chủ nhà: Hiện nút Tạo hóa đơn
        } else {
            binding.btnRentNow.visibility = View.VISIBLE    // Khách: Hiện nút Thuê ngay
            binding.btnCreateBill.visibility = View.GONE    // Khách: Ẩn nút Tạo hóa đơn
        }

        // 4. SỰ KIỆN: BẤM NÚT TẠO HÓA ĐƠN
        binding.btnCreateBill.setOnClickListener {
            if (currentProperty != null) {
                val intent = Intent(this, AddBillActivity::class.java)
                intent.putExtra("CHOSEN_PROPERTY", currentProperty) // Truyền Object sang AddBillActivity
                startActivity(intent)
            } else {
                Toast.makeText(this, "Không tìm thấy thông tin phòng trọ để tạo hóa đơn!", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. SỰ KIỆN: THUÊ PHÒNG NGAY
        binding.btnRentNow.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thuê phòng!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Xác nhận thuê phòng")
                .setMessage("Bạn có chắc chắn muốn gửi yêu cầu thuê căn phòng này không?")
                .setPositiveButton("Thuê ngay") { _, _ ->
                    firestore.collection("Users").document(currentUser.uid).get()
                        .addOnSuccessListener { userDoc ->
                            val tenantName = userDoc.getString("fullName") ?: "Khách thuê"

                            val handoverData = hashMapOf(
                                "roomName" to title,
                                "tenantName" to tenantName,
                                "assetCheck" to "Chờ kiểm tra (Diện tích: $area)",
                                "completed" to false
                            )

                            firestore.collection("handovers").add(handoverData)
                                .addOnSuccessListener {
                                    // Cập nhật trạng thái phòng trọ và gắn thêm tenantUid vào phòng
                                    if (propertyId.isNotEmpty()) {
                                        val updateMap = hashMapOf(
                                            "status" to "Đã thuê",
                                            "tenantUid" to currentUser.uid
                                        )
                                        firestore.collection("properties").document(propertyId)
                                            .update(updateMap as Map<String, Any>)
                                    }

                                    Toast.makeText(this, "Yêu cầu thuê thành công! Vui lòng vào mục Nhận Bàn Giao ở trang chủ.", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                        }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // CÁC NÚT TƯƠNG TÁC KHÁC
        binding.btnFavorite.setOnClickListener { binding.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on) }
        binding.btnChat.setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }

        setupCommentRecycler()
        loadComments()
        binding.btnSendComment.setOnClickListener { sendComment() }
    }

    private fun setupCommentRecycler() {
        adapter = CommentAdapter(commentList)
        binding.recyclerComment.layoutManager = LinearLayoutManager(this)
        binding.recyclerComment.adapter = adapter
    }

    private fun sendComment() {
        val text = binding.edtComment.text.toString()
        if (text.isEmpty()) return
        val comment = hashMapOf("username" to "Guest", "content" to text)
        firestore.collection("comments").add(comment)
        binding.edtComment.text.clear()
    }

    private fun loadComments() {
        firestore.collection("comments").addSnapshotListener { value, _ ->
            commentList.clear()
            value?.documents?.forEach {
                val comment = it.toObject(Comment::class.java)
                if (comment != null) commentList.add(comment)
            }
            adapter.notifyDataSetChanged()
        }
    }
}