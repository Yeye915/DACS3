package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityPropertyBinding
import com.google.firebase.firestore.ListenerRegistration

class PropertyActivity : HouseActivity() {
    private lateinit var binding: ActivityPropertyBinding
    private val properties=mutableListOf<Property>()
    private val filtered=mutableListOf<Property>()
    private val adapter=PropertyAdapter(filtered)
    private val savedIds=mutableSetOf<String>()
    private var listener: ListenerRegistration?=null
    private var favoritesListener: ListenerRegistration?=null
    private var accountUid: String?=null
    private var loading=true
    private var loadFailed=false
    private var favoritesOnly=false
    private var filters=RoomFilters()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPropertyBinding.inflate(layoutInflater);setContentView(binding.root)
        filters=savedInstanceState?.getSerializable("filters") as? RoomFilters ?: RoomFilters()
        favoritesOnly=savedInstanceState?.getBoolean("favoritesOnly") ?: intent.getBooleanExtra("FAVORITES_ONLY",false)
        binding.recyclerProperty.layoutManager=GridLayoutManager(this,if(resources.configuration.screenWidthDp>=600) 2 else 1)
        binding.recyclerProperty.adapter=adapter
        binding.edtSearch.setText(filters.query)
        binding.edtSearch.doOnTextChanged { text,_,_,_ -> filters=filters.copy(query=text.toString());filterProperties() }
        binding.btnAddProperty.setOnClickListener { if(requireAccount()) startActivity(Intent(this,AddPropertyActivity::class.java)) }
        binding.btnMapRooms.setOnClickListener { startActivity(Intent(this,MapActivity::class.java).putExtra("ROOM_FILTERS",filters)
            .putExtra("FAVORITE_IDS",if(favoritesOnly) ArrayList(savedIds) else null)) }
        binding.btnFilterRooms.setOnClickListener { showFilters() }
        binding.btnSavedRooms.setOnClickListener { if(requireAccount()) { favoritesOnly=!favoritesOnly; filterProperties() } }
        binding.btnRetry.setOnClickListener {
            if(loadFailed) loadProperties() else { filters=RoomFilters();favoritesOnly=false;binding.edtSearch.text.clear();filterProperties() }
        }
        binding.bottomNav.selectedItemId=R.id.nav_rooms
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target=when(item.itemId) { R.id.nav_home -> MainActivity::class.java; R.id.nav_blog -> NewsActivity::class.java; R.id.nav_profile -> ProfileActivity::class.java; else -> null }
            if(target!=null) { startActivity(Intent(this,target));overridePendingTransition(0,0);finish() };true
        }
        loadProperties()
    }
    override fun onStart() {
        super.onStart()
        val uid=Backend.auth.currentUser?.uid
        if(uid!=accountUid || favoritesListener==null && uid!=null) {
            accountUid=uid;favoritesListener?.remove();savedIds.clear()
            if(uid!=null) favoritesListener=Backend.db.collection("Users").document(uid).collection("favorites").addSnapshotListener { result,error ->
                if(error!=null) failure(error)
                if(result!=null) { savedIds.clear();savedIds.addAll(result.documents.map { it.id });adapter.favoriteIds=savedIds.toSet();filterProperties() }
            }
            filterProperties()
        }
        if(uid==null) { adapter.favoriteIds=emptySet();filterProperties() }
    }
    private fun loadProperties() {
        loading=true;loadFailed=false;renderState();listener?.remove()
        listener=Backend.db.collection("properties").addSnapshotListener { results,error ->
            loading=false;loadFailed=error!=null
            if(results!=null) { properties.clear();properties.addAll(results.documents.map(Property::from)) }
            filterProperties()
        }
    }
    private fun filterProperties() {
        filtered.clear();filtered.addAll(filters.apply(properties,Backend.auth.currentUser?.uid).filter { !favoritesOnly || it.id in savedIds })
        adapter.notifyDataSetChanged();renderState()
    }
    private fun renderState() {
        binding.btnSavedRooms.text=if(favoritesOnly) "♥ Đã lưu" else "♡ Đã lưu"
        binding.btnFilterRooms.text=if(filters.copy(query="")!=RoomFilters()) "Lọc •" else "Bộ lọc"
        binding.loadingIndicator.isVisible=loading && properties.isEmpty()
        binding.emptyState.isVisible=!loading && filtered.isEmpty();binding.recyclerProperty.isVisible=filtered.isNotEmpty()
        binding.emptyTitle.text=when { loadFailed -> "Chưa kết nối được";favoritesOnly -> "Chưa có phòng đã lưu phù hợp"; else -> "Chưa tìm thấy phòng phù hợp" }
        binding.emptyDescription.text=if(loadFailed) "Kiểm tra kết nối mạng rồi thử lại." else "Đổi từ khóa, điều chỉnh bộ lọc hoặc lưu phòng bằng biểu tượng trái tim."
        binding.btnRetry.text=if(loadFailed) "Thử lại" else "Xóa bộ lọc"
    }
    private fun showFilters() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(32,8,32,8) }
        fun field(hint: String,value: String)=EditText(this).apply { this.hint=hint;inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText(value);box.addView(this) }
        val min=field("Giá tối thiểu (đ)",filters.minPrice.takeIf { it>0 }?.toString().orEmpty())
        val max=field("Giá tối đa (đ)",filters.maxPrice.takeIf { it<RentalValidation.MAX_MONEY }?.toString().orEmpty())
        val area=field("Diện tích tối thiểu (m²)",filters.minArea.takeIf { it>0 }?.toString().orEmpty()).apply { inputType=8194 }
        val vacant=CheckBox(this).apply { text="Chỉ phòng còn trống";isChecked=this@PropertyActivity.filters.vacantOnly;box.addView(this) }
        val owned=CheckBox(this).apply { text="Phòng tôi đăng";isChecked=this@PropertyActivity.filters.ownedOnly;isEnabled=Backend.auth.currentUser!=null;box.addView(this) }
        val sort=Spinner(this).apply { adapter=ArrayAdapter(this@PropertyActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Theo tên phòng","Giá thấp đến cao","Giá cao đến thấp"));setSelection(filters.sort);box.addView(this) }
        val dialog=AlertDialog.Builder(this).setTitle("Lọc phòng").setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Áp dụng",null).setNeutralButton("Đặt lại") { _,_ -> filters=RoomFilters(query=filters.query);filterProperties() }.setNegativeButton("Hủy",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val low=if(min.text.isBlank()) 0L else RentalValidation.money(min.text.toString())
            val high=if(max.text.isBlank()) RentalValidation.MAX_MONEY else RentalValidation.money(max.text.toString())
            val size=if(area.text.isBlank()) 0.0 else area.text.toString().replace(",",".").toDoubleOrNull()
            if(low==null || high==null || low>high || size==null || !size.isFinite() || size<0) { notice("Kiểm tra khoảng giá và diện tích");return@setOnClickListener }
            filters=RoomFilters(filters.query,low,high,size,vacant.isChecked,sort.selectedItemPosition,owned.isChecked);filterProperties();dialog.dismiss()
        } };dialog.show()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putSerializable("filters",filters);out.putBoolean("favoritesOnly",favoritesOnly);super.onSaveInstanceState(out) }
    override fun onDestroy() { listener?.remove();favoritesListener?.remove();super.onDestroy() }
}
