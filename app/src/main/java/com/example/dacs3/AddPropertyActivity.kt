package com.example.dacs3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityAddPropertyBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue

class AddPropertyActivity : HouseActivity() {
    private lateinit var binding: ActivityAddPropertyBinding
    private var selectedImage: Uri?=null
    private var original: Property?=null
    private var latitude: Double?=null
    private var longitude: Double?=null
    private lateinit var pinStatus: TextView
    private lateinit var paused: CheckBox
    private var pinnedAddress=""
    private var posting=false
    private var hostRole=false
    private var documentId=""
    private var uploadedImage: String?=null
    private val pickImage=registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) { selectedImage=uri;uploadedImage=null;binding.imgSelected.setImageURI(uri) }
    }
    private val pickLocation=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode==RESULT_OK) {
            pinnedAddress=binding.edtLocation.text.toString().trim()
            latitude=result.data?.getDoubleExtra("LATITUDE",Double.NaN);longitude=result.data?.getDoubleExtra("LONGITUDE",Double.NaN);showPin()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityAddPropertyBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        original=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        documentId=savedInstanceState?.getString("documentId") ?: original?.id ?: Backend.db.collection("properties").document().id
        original?.let { p ->
            if(p.hostId!=Backend.auth.currentUser!!.uid) { notice("Bạn không quản lý phòng này");finish();return }
            binding.edtTitle.setText(p.title);binding.edtPrice.setText(p.rent.takeIf { it>0 }?.toString().orEmpty())
            binding.edtLocation.setText(p.address);binding.edtBedroom.setText(p.bedroom.toString());binding.edtBathroom.setText(p.bathroom.toString());binding.edtArea.setText(p.area)
            Glide.with(this).load(p.imageUrl).placeholder(R.drawable.illustration_home).into(binding.imgSelected)
            binding.btnAddProperty.text="Lưu thay đổi"
        }
        latitude=savedInstanceState?.getDouble("lat",Double.NaN) ?: original?.latitude
        longitude=savedInstanceState?.getDouble("lon",Double.NaN) ?: original?.longitude
        uploadedImage=savedInstanceState?.getString("uploadedImage")
        pinnedAddress=savedInstanceState?.getString("pinnedAddress") ?: original?.address.orEmpty()
        selectedImage=savedInstanceState?.getString("selectedImage")?.let(Uri::parse)
        selectedImage?.let { runCatching { binding.imgSelected.setImageURI(it) }.onFailure { selectedImage=null;notice("Hãy chọn lại ảnh trước khi lưu") } }
        val form=binding.edtLocation.parent as LinearLayout
        val locationIndex=form.indexOfChild(binding.edtLocation)
        val pinButton=MaterialButton(this).apply { text="Ghim vị trí trên bản đồ";isAllCaps=false
            setOnClickListener {
                pickLocation.launch(Intent(this@AddPropertyActivity,MapActivity::class.java).putExtra("PICK_LOCATION",true)
                    .putExtra("ADDRESS",binding.edtLocation.text.toString()).apply {
                        latitude?.let { putExtra("LATITUDE",it) };longitude?.let { putExtra("LONGITUDE",it) }
                    })
            } }
        form.addView(pinButton,locationIndex+1)
        pinStatus=TextView(this).apply { textSize=14f;setPadding(0,8,0,12) };form.addView(pinStatus,locationIndex+2)
        paused=CheckBox(this).apply { text="Tạm dừng nhận yêu cầu thuê";isChecked=savedInstanceState?.getBoolean("paused") ?: (original?.status=="Đang bảo trì")
            isEnabled=original?.tenantUid.isNullOrBlank() };form.addView(paused,form.indexOfChild(binding.btnAddProperty))
        showPin()
        binding.edtLocation.doOnTextChanged { text,_,_,_ -> if(text.toString().trim()!=pinnedAddress) { latitude=null;longitude=null;showPin() } }
        binding.btnSelectImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnAddProperty.isEnabled=false
        Backend.db.collection("Users").document(Backend.auth.currentUser!!.uid).get().addOnSuccessListener {
            hostRole=it.getString("role")=="host"
            binding.btnAddProperty.isEnabled=hostRole
            if(!hostRole) notice("Chỉ tài khoản chủ nhà có thể đăng phòng. Vai trò được chọn khi đăng ký.")
        }.addOnFailureListener { notice("Chưa kiểm tra được tài khoản chủ nhà. Hãy quay lại và thử lại.");failure(it) }
        binding.btnAddProperty.setOnClickListener { save() }
    }
    private fun showPin() { pinStatus.text=if(RentalValidation.coordinates(latitude,longitude)) "Đã ghim: %.6f, %.6f".format(java.util.Locale.US,latitude,longitude) else "Chưa có vị trí. Ghim đúng phòng để người thuê xem bản đồ." }
    private fun save() {
        if(posting || !hostRole) return
        val title=binding.edtTitle.text.toString().trim();val location=binding.edtLocation.text.toString().trim()
        val price=RentalValidation.money(binding.edtPrice.text.toString());val beds=binding.edtBedroom.text.toString().toIntOrNull();val baths=binding.edtBathroom.text.toString().toIntOrNull()
        val area=binding.edtArea.text.toString().replace(",",".").toDoubleOrNull()
        binding.edtTitle.error=if(title.length !in 3..150) "Tên phòng từ 3 đến 150 ký tự" else null
        binding.edtLocation.error=if(location.length !in 5..300) "Nhập địa chỉ chi tiết từ 5 đến 300 ký tự" else null
        binding.edtPrice.error=if(price==null || price<=0) "Nhập giá thuê hợp lệ lớn hơn 0" else null
        binding.edtBedroom.error=if(beds==null || beds !in 0..50) "Từ 0 đến 50" else null
        binding.edtBathroom.error=if(baths==null || baths !in 0..50) "Từ 0 đến 50" else null
        binding.edtArea.error=if(area==null || !area.isFinite() || area<=0 || area>100000) "Diện tích hợp lệ lớn hơn 0" else null
        if(listOf(binding.edtTitle,binding.edtLocation,binding.edtPrice,binding.edtBedroom,binding.edtBathroom,binding.edtArea).any { it.error!=null }) return
        if(!RentalValidation.coordinates(latitude,longitude)) { notice("Hãy ghim vị trí phòng trên bản đồ trước khi lưu");return }
        posting=true;binding.btnAddProperty.isEnabled=false;binding.btnSelectImage.isEnabled=false;binding.btnAddProperty.text="Đang lưu…"
        fun write(image: String) {
            val uid=Backend.auth.currentUser?.uid ?: run { restore();return }
            val ref=Backend.db.collection("properties").document(documentId)
            Backend.db.runTransaction { tx ->
                val current=tx.get(ref)
                if(original!=null) require(current.exists() && Property.from(current).hostId==uid) { "Phòng đã thay đổi hoặc bạn không còn quyền sửa" }
                else require(!current.exists()) { "Phòng đã được lưu trước đó" }
                val values=mutableMapOf<String,Any>("title" to title,"location" to location,"rental" to location,
                    "price" to "${RentalValidation.format(price!!)}/tháng","monthlyPrice" to price,"bedroom" to beds!!,"bathroom" to baths!!,
                    "area" to area!!.toString(),"imageUrl" to image,"latitude" to latitude!!,"longitude" to longitude!!,"hostId" to uid,"hostUid" to uid,"updatedAt" to FieldValue.serverTimestamp())
                if(original==null) values.putAll(mapOf("tenantUid" to "","status" to if(paused.isChecked) "Đang bảo trì" else "Trống","rating" to 0,"createdAt" to FieldValue.serverTimestamp()))
                else if(Property.from(current).tenantUid.isBlank()) values["status"]=if(paused.isChecked) "Đang bảo trì" else "Trống"
                if(original==null) tx.set(ref,values) else tx.update(ref,values)
                null
            }.addOnSuccessListener { notice("Đã lưu thông tin phòng");finish() }.addOnFailureListener { failure(it);restore() }
        }
        if(selectedImage!=null && uploadedImage==null) ImageUpload.upload(this,selectedImage!!).addOnSuccessListener { uploadedImage=it;write(it) }.addOnFailureListener { failure(it);restore() }
        else write(uploadedImage ?: original?.imageUrl.orEmpty())
    }
    private fun restore() { posting=false;binding.btnAddProperty.isEnabled=hostRole;binding.btnSelectImage.isEnabled=true;binding.btnAddProperty.text=if(original==null) "Đăng phòng" else "Lưu thay đổi" }
    override fun onSaveInstanceState(out: Bundle) { out.putString("pinnedAddress",pinnedAddress);out.putString("selectedImage",selectedImage?.toString());out.putString("documentId",documentId);out.putString("uploadedImage",uploadedImage);latitude?.let { out.putDouble("lat",it) };longitude?.let { out.putDouble("lon",it) };out.putBoolean("paused",paused.isChecked);super.onSaveInstanceState(out) }
}
