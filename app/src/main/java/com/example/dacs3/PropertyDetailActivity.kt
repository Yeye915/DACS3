package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityPropertyDetailBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue

class PropertyDetailActivity : HouseActivity() {
    private lateinit var binding: ActivityPropertyDetailBinding
    private var property=Property()
    private val comments=mutableListOf<Comment>()
    private val commentAdapter=CommentAdapter(comments)
    private var roomListener: ListenerRegistration?=null
    private var commentListener: ListenerRegistration?=null
    private var favoriteListener: ListenerRegistration?=null
    private var requestListener: ListenerRegistration?=null
    private lateinit var status: TextView
    private lateinit var edit: MaterialButton
    private var pending=false
    private var savingRequest=false
    private var name="Người dùng"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPropertyDetailBinding.inflate(layoutInflater);setContentView(binding.root)
        property=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property ?: Property(id=intent.getStringExtra("propertyId").orEmpty())
        if(property.id.isBlank()) { notice("Không tìm thấy mã phòng");finish();return }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerComment.layoutManager=LinearLayoutManager(this);binding.recyclerComment.adapter=commentAdapter
        val info=binding.txtTitle.parent as LinearLayout
        status=TextView(this).apply { textSize=14f;setPadding(0,16,0,8);info.addView(this) }
        MaterialButton(this).apply { id=R.id.btnViewMap; text="Xem vị trí trên bản đồ";isAllCaps=false;info.addView(this)
            setOnClickListener { startActivity(Intent(this@PropertyDetailActivity,MapActivity::class.java).putExtra("CHOSEN_PROPERTY",property)) } }
        edit=MaterialButton(this).apply { text="Chỉnh sửa phòng";isAllCaps=false;info.addView(this)
            setOnClickListener { startActivity(Intent(this@PropertyDetailActivity,AddPropertyActivity::class.java).putExtra("CHOSEN_PROPERTY",property)) } }
        binding.btnRentNow.setOnClickListener { requestRent() }
        binding.btnCreateBill.setOnClickListener { startActivity(Intent(this,AddBillActivity::class.java).putExtra("CHOSEN_PROPERTY",property)) }
        binding.btnFavorite.setOnClickListener {
            if(requireAccount()) { binding.btnFavorite.isEnabled=false;Favorites.toggle(property.id).addOnFailureListener(::failure).addOnCompleteListener { binding.btnFavorite.isEnabled=true } }
        }
        binding.btnChat.setOnClickListener {
            if(requireAccount()) {
                val uid=Backend.auth.currentUser!!.uid
                val peer=if(uid==property.hostId) property.tenantUid else property.hostId
                if(peer.isBlank() || peer==uid) { notice("Phòng chưa có người liên hệ hợp lệ");return@setOnClickListener }
                startActivity(Intent(this,ChatActivity::class.java).putExtra("CHOSEN_PROPERTY",property).putExtra("PEER_UID",peer))
            }
        }
        binding.btnSendComment.setOnClickListener { sendComment() }
        render()
    }
    override fun onStart() {
        super.onStart();if(property.id.isBlank()) return
        roomListener=Backend.db.collection("properties").document(property.id).addSnapshotListener { doc,error ->
            if(error!=null) { failure(error);return@addSnapshotListener }
            if(doc!=null && doc.exists()) { property=Property.from(doc);render() }
            else if(doc!=null) { notice("Phòng này không còn tồn tại");finish() }
        }
        commentListener=Backend.db.collection("comments").whereEqualTo("propertyId",property.id).addSnapshotListener { data,error ->
            if(error!=null) { failure(error);return@addSnapshotListener }
            comments.clear();data?.documents?.sortedBy { it.getTimestamp("createdAt") }?.forEach { comments.add(it.toObject(Comment::class.java)!!) };commentAdapter.notifyDataSetChanged()
        }
        val uid=Backend.auth.currentUser?.uid;pending=false
        if(uid!=null) {
            name=Backend.auth.currentUser?.displayName.orEmpty().ifBlank { "Người dùng" }
            Backend.db.collection("Users").document(uid).get().addOnSuccessListener { name=it.getString("fullName").orEmpty().ifBlank { name } }
            favoriteListener=Backend.db.collection("Users").document(uid).collection("favorites").document(property.id).addSnapshotListener { doc,error ->
                if(error!=null) failure(error)
                val saved=doc?.exists()==true
                binding.btnFavorite.alpha=if(saved) 1f else 0.5f
                binding.btnFavorite.contentDescription=if(saved) "Bỏ lưu phòng" else "Lưu phòng yêu thích"
            }
            requestListener=Backend.db.collection("rental_requests").document(RentalValidation.requestId(property.id,uid)).addSnapshotListener { doc,error ->
                // A missing request is expected before the first submission.
                if(error==null) { pending=doc?.getString("status")=="pending";render() }
            }
        }
        render()
    }
    private fun render() {
        if(!::status.isInitialized) return
        val uid=Backend.auth.currentUser?.uid;val owned=uid!=null && uid==property.hostId
        binding.txtTitle.text=property.title;binding.txtPrice.text=if(property.rent>0) "${RentalValidation.format(property.rent)}/tháng" else property.price
        binding.txtLocation.text=property.address;binding.txtArea.text="${property.area} m²";binding.ratingBar.rating=property.rating;binding.ratingBar.isVisible=property.rating>0
        Glide.with(this).load(property.imageUrl).placeholder(R.drawable.illustration_home).error(R.drawable.illustration_home).into(binding.imgProperty)
        binding.txtPropertyStatus.text=property.status
        status.text=when { property.hostId.isBlank() -> "Tin cũ chưa có UID chủ nhà. Chưa thể gửi yêu cầu thuê hoặc liên hệ."
            uid!=null && uid==property.tenantUid -> "Bạn đang thuê phòng này"
            pending -> "Bạn đã gửi yêu cầu. Chủ nhà sẽ duyệt trước khi bàn giao."
            else -> "Trạng thái: ${property.status}" }
        binding.btnRentNow.isVisible=!owned
        binding.btnRentNow.text=when { pending -> "Xem yêu cầu";uid!=null && uid==property.tenantUid -> "Phòng đang thuê";property.status!="Trống" -> "Phòng chưa sẵn sàng";else -> "Gửi yêu cầu thuê" }
        binding.btnRentNow.isEnabled=!savingRequest && (pending || property.hostId.isNotBlank() && property.status=="Trống" && property.tenantUid.isBlank())
        binding.btnCreateBill.isVisible=owned;binding.btnCreateBill.isEnabled=property.tenantUid.isNotBlank()
        edit.isVisible=owned;binding.btnChat.isEnabled=property.hostId.isNotBlank() && (!owned || property.tenantUid.isNotBlank())
    }
    private fun requestRent() {
        if(!requireAccount()) return
        if(pending) { startActivity(Intent(this,RentalRequestsActivity::class.java));return }
        AlertDialog.Builder(this).setTitle("Gửi yêu cầu thuê phòng")
            .setMessage("${property.title}\n${RentalValidation.format(property.rent)}/tháng\n\nChủ nhà sẽ duyệt yêu cầu. Bạn cần kiểm tra phòng, thỏa thuận hợp đồng và tiền cọc trực tiếp trước khi xác nhận bàn giao.")
            .setNegativeButton("Hủy",null).setPositiveButton("Gửi yêu cầu") { _,_ ->
                savingRequest=true;render()
                RentalStore.request(property).addOnSuccessListener { notice("Đã gửi yêu cầu thuê. Theo dõi trong Yêu cầu thuê của tôi.") }
                    .addOnFailureListener(::failure).addOnCompleteListener { savingRequest=false;render() }
            }.show()
    }
    private fun sendComment() {
        val text=binding.edtComment.text.toString().trim()
        if(text.isBlank()) return
        if(text.length>2000) { binding.edtComment.error="Tối đa 2.000 ký tự";return }
        if(!requireAccount()) return
        binding.btnSendComment.isEnabled=false
        Backend.db.collection("comments").add(mapOf("propertyId" to property.id,"userUid" to Backend.auth.currentUser!!.uid,
            "username" to name,"content" to text,"createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { if(binding.edtComment.text.toString().trim()==text) binding.edtComment.text.clear() }
            .addOnFailureListener(::failure).addOnCompleteListener { binding.btnSendComment.isEnabled=true }
    }
    override fun onStop() { roomListener?.remove();commentListener?.remove();favoriteListener?.remove();requestListener?.remove();super.onStop() }
}
