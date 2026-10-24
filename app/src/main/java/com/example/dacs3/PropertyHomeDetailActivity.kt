package com.example.dacs3

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityAddPropertyDetailBinding

class PropertyHomeDetailActivity : HouseActivity() {
    private lateinit var binding: ActivityAddPropertyDetailBinding
    private val assets=mutableListOf<AssetItem>()
    private lateinit var adapter: AssetAdapter
    private var property: Property?=null
    private var loadedIds=emptySet<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityAddPropertyDetailBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        adapter=AssetAdapter(assets);binding.rvAssetList.layoutManager=LinearLayoutManager(this);binding.rvAssetList.adapter=adapter
        binding.spinnerStatus.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Trống","Đang bảo trì","Đã thuê"))
        binding.edtRoomCode.isFocusable=false;binding.edtRoomCode.hint="Chạm để chọn phòng";binding.edtRoomCode.setOnClickListener { chooseProperty(true,false,::select) }
        binding.btnSaveAll.isEnabled=false
        val chosen=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        if(chosen!=null) select(chosen) else chooseProperty(true,false,::select)
        binding.btnAddCustomAsset.setOnClickListener {
            val name=binding.edtCustomAssetName.text.toString().trim()
            if(name.length !in 1..100 || assets.any { it.assetName.equals(name,true) } || assets.size>=50) { notice("Tên tài sản từ 1 đến 100 ký tự, không trùng; tối đa 50 loại mỗi phòng");return@setOnClickListener }
            assets.add(AssetItem(assetName=name));adapter.notifyItemInserted(assets.lastIndex);binding.edtCustomAssetName.text.clear()
        }
        binding.btnSaveAll.setOnClickListener { save() }
    }
    private fun select(room: Property) {
        if(room.hostId!=Backend.auth.currentUser?.uid) { notice("Bạn không quản lý phòng này");return }
        property=room;binding.edtRoomCode.setText(room.title);binding.edtPrice.setText(room.rent.toString());binding.edtMaxPeople.setText(room.maxPeople.toString())
        binding.spinnerStatus.setSelection(when(room.status) { "Đang bảo trì"->1;"Đã thuê"->2;else->0 });binding.spinnerStatus.isEnabled=room.tenantUid.isBlank()
        binding.btnSaveAll.isEnabled=false
        Backend.db.collection("assets").whereEqualTo("propertyId",room.id).get().addOnSuccessListener { data ->
            assets.clear();assets.addAll(data.documents.map { it.toObject(AssetItem::class.java)!!.copy(id=it.id) });loadedIds=data.documents.map { it.id }.toSet()
            adapter.notifyDataSetChanged();binding.btnSaveAll.isEnabled=true
        }.addOnFailureListener(::failure)
    }
    private fun save() {
        val room=property ?: return;val uid=Backend.auth.currentUser?.uid ?: return
        val price=RentalValidation.money(binding.edtPrice.text.toString());val people=binding.edtMaxPeople.text.toString().toIntOrNull()
        if(price==null || price<=0 || people==null || people !in 1..50 || assets.any { it.quantity !in 0..1000 }) { notice("Kiểm tra giá thuê, số người (1–50) và số lượng tài sản (0–1.000)");return }
        val status=binding.spinnerStatus.selectedItem.toString()
        if(room.tenantUid.isBlank() && status=="Đã thuê") { notice("Phòng chỉ được đánh dấu đã thuê qua quy trình chấp thuận yêu cầu thuê");return }
        binding.btnSaveAll.isEnabled=false
        val batch=Backend.db.batch()
        val retained=assets.map { if(it.id.isNotBlank()) it.id else RentalValidation.id(room.id,it.assetName.lowercase(java.util.Locale.ROOT)) }.toSet()
        (loadedIds-retained).forEach { batch.delete(Backend.db.collection("assets").document(it)) }
        assets.forEach { item ->
            val id=if(item.id.isNotBlank()) item.id else RentalValidation.id(room.id,item.assetName.lowercase(java.util.Locale.ROOT))
            batch.set(Backend.db.collection("assets").document(id),mapOf("propertyId" to room.id,"hostId" to uid,"roomName" to room.title,"assetName" to item.assetName,"quantity" to item.quantity,"condition" to item.condition))
        }
        // Room edits preserve tenant identity and never end a rental through a status spinner.
        batch.update(Backend.db.collection("properties").document(room.id),mapOf("monthlyPrice" to price,"price" to "${RentalValidation.format(price)}/tháng","maxPeople" to people,"status" to if(room.tenantUid.isNotBlank()) room.status else status))
        batch.commit().addOnSuccessListener { notice("Đã lưu tài sản của phòng");finish() }.addOnFailureListener(::failure).addOnCompleteListener { binding.btnSaveAll.isEnabled=true }
    }
}
