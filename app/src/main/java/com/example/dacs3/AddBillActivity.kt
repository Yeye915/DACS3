package com.example.dacs3

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.example.dacs3.databinding.ActivityAddBillBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class AddBillActivity : HouseActivity() {
    private lateinit var binding: ActivityAddBillBinding
    private var room: Property?=null
    private lateinit var total: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityAddBillBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        total=TextView(this).apply { textSize=20f;setPadding(0,24,0,16) }
        val form=binding.btnSubmitBill.parent as LinearLayout;form.addView(total,form.indexOfChild(binding.btnSubmitBill))
        binding.edtBillRoomName.isEnabled=true;binding.edtBillRoomName.isFocusable=false;binding.edtBillAddress.isFocusable=false
        binding.edtBillMonth.setText(YearMonth.now().format(DateTimeFormatter.ofPattern("MM/yyyy")))
        binding.edtBillElectricity.setText("0");binding.edtBillWater.setText("0")
        listOf(binding.edtBillRoomPrice,binding.edtBillElectricity,binding.edtBillWater).forEach { it.doOnTextChanged { _,_,_,_ -> updateTotal() } }
        binding.btnSubmitBill.isEnabled=false
        val chosen=intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property
        if(chosen!=null) select(chosen) else chooseProperty(true,true,::select)
        binding.edtBillRoomName.setOnClickListener { chooseProperty(true,true,::select) }
        binding.btnSubmitBill.setOnClickListener {
            val p=room ?: return@setOnClickListener
            val month=RentalValidation.month(binding.edtBillMonth.text.toString())
            val amounts=listOf(binding.edtBillRoomPrice,binding.edtBillElectricity,binding.edtBillWater).map { RentalValidation.money(it.text.toString()) }
            if(month==null) { binding.edtBillMonth.error="Tháng theo dạng MM/YYYY";return@setOnClickListener }
            if(amounts.any { it==null } || RentalValidation.total(*amounts.filterNotNull().toLongArray())==null) { notice("Nhập số tiền hợp lệ, không để trống");return@setOnClickListener }
            binding.btnSubmitBill.isEnabled=false
            RentalStore.createBill(p.id,month,amounts[0]!!,amounts[1]!!,amounts[2]!!)
                .addOnSuccessListener { notice("Đã tạo hóa đơn $month");finish() }.addOnFailureListener(::failure)
                .addOnCompleteListener { binding.btnSubmitBill.isEnabled=true }
        }
    }
    private fun select(p: Property) {
        if(p.hostId!=Backend.auth.currentUser?.uid || p.tenantUid.isBlank()) { notice("Chọn phòng của bạn đang có người thuê");return }
        room=p;binding.edtBillRoomName.setText(p.title);binding.edtBillAddress.setText(p.address);binding.edtBillRoomPrice.setText(p.rent.toString());binding.btnSubmitBill.isEnabled=true
    }
    private fun updateTotal() {
        val amounts=listOf(binding.edtBillRoomPrice,binding.edtBillElectricity,binding.edtBillWater).map { RentalValidation.money(it.text.toString()) }
        total.text=if(amounts.any { it==null }) "Tổng: —" else "Tổng: ${RentalValidation.total(*amounts.filterNotNull().toLongArray())?.let(RentalValidation::format) ?: "Vượt giới hạn"}"
    }
}
