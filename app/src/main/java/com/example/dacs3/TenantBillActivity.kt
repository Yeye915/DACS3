package com.example.dacs3

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityTenantBillBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TenantBillActivity : HouseActivity() {

    private lateinit var binding: ActivityTenantBillBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bills = mutableListOf<Bill>()
    private lateinit var adapter: TenantBillAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTenantBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadTenantBills()
    }

    private fun setupRecyclerView() {
        adapter = TenantBillAdapter(bills)
        binding.rvTenantBills.layoutManager = LinearLayoutManager(this)
        binding.rvTenantBills.adapter = adapter
    }

    private fun loadTenantBills() {
        val currentUid = auth.currentUser?.uid ?: return

        // Kết nối Firestore quét đúng hóa đơn của người dùng đang đăng nhập
        db.collection("bills")
            .whereEqualTo("tenantUid", currentUid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    bills.clear()
                    for (doc in snapshots) {
                        val bill = doc.toObject(Bill::class.java)
                        bill.id = doc.id
                        bills.add(bill)
                    }

                    if (bills.isEmpty()) {
                        binding.tvEmptyBill.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyBill.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}