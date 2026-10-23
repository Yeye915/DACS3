package com.example.dacs3

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAddBillBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class AddBillActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBillBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val formatter = DecimalFormat("#,###")

    // Đối tượng nhận dữ liệu phòng trọ được truyền từ Adapter sang
    private var currentProperty: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận dữ liệu phòng trọ từ PropertyAdapter truyền sang
        currentProperty = intent.getSerializableExtra("CHOSEN_PROPERTY") as? Property

        // 2. Tự động điền dữ liệu vào đúng ID XML của Nhi
        currentProperty?.let { property ->
            binding.edtBillRoomName.setText(property.title)
            binding.edtBillAddress.setText(property.rental) // Hoặc property.location tùy thuộc Model của bạn

            // Làm sạch chuỗi tiền gốc trước khi đưa vào ô nhập
            val cleanPrice = property.price.replace(",", "").replace(".", "").replace("đ", "").trim()
            binding.edtBillRoomPrice.setText(cleanPrice)
        } ?: run {
            Toast.makeText(this, "Không tìm thấy dữ liệu nhà phòng trọ!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupAutoCalculate()

        // 3. Đúng ID nút bấm btnSubmitBill trong XML của bạn
        binding.btnSubmitBill.setOnClickListener {
            saveBillToFirestore()
        }
    }

    private fun setupAutoCalculate() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        // Lắng nghe sự thay đổi để tính toán tổng tiền động
        binding.edtBillRoomPrice.addTextChangedListener(watcher)
        binding.edtBillElectricity.addTextChangedListener(watcher)
        binding.edtBillWater.addTextChangedListener(watcher)
    }

    private fun calculateTotal(): Long {
        val roomPrice = binding.edtBillRoomPrice.text.toString().toLongOrNull() ?: 0L
        val elecBill = binding.edtBillElectricity.text.toString().toLongOrNull() ?: 0L
        val waterBill = binding.edtBillWater.text.toString().toLongOrNull() ?: 0L

        return roomPrice + elecBill + waterBill
    }

    private fun saveBillToFirestore() {
        val property = currentProperty ?: return
        val roomName = binding.edtBillRoomName.text.toString().trim()
        val month = binding.edtBillMonth.text.toString().trim()

        if (roomName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên phòng", Toast.LENGTH_SHORT).show()
            return
        }
        if (month.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tháng xuất hóa đơn", Toast.LENGTH_SHORT).show()
            return
        }

        val roomFee = binding.edtBillRoomPrice.text.toString().toLongOrNull() ?: 0L
        val electricityFee = binding.edtBillElectricity.text.toString().toLongOrNull() ?: 0L
        val waterFee = binding.edtBillWater.text.toString().toLongOrNull() ?: 0L

        // Đóng gói dữ liệu chuẩn chỉnh gửi lên tập hợp "bills"
        val billData = hashMapOf(
            "roomName" to roomName,
            "month" to month,
            "roomFee" to roomFee,
            "electricityFee" to electricityFee,
            "waterFee" to waterFee,
            "totalAmount" to calculateTotal(),
            "date" to System.currentTimeMillis(),
            "status" to "Chưa thanh toán",

            // Các trường liên kết ID cốt lõi
            "hostId" to property.hostId,
            "tenantUid" to property.tenantUid
        )

        firestore.collection("bills").add(billData)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xuất hóa đơn thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi lưu hóa đơn", Toast.LENGTH_SHORT).show()
            }
    }
}