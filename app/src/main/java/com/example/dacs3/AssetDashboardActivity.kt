package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityAssetDashboardBinding
import com.google.firebase.firestore.FirebaseFirestore

class AssetDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssetDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val assetList = mutableListOf<AssetItem>()
    // SỬA DÒNG NÀY: Chuyển từ mutableListOf() sang arrayListOf() cho đồng bộ với Adapter
    private val filteredList = arrayListOf<AssetItem>()
    // Đổi từ AssetAdapter sang AssetDashboardAdapter
    private lateinit var adapter: AssetDashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssetDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Nút FAB mở màn hình thêm tài sản mới của bạn
        binding.fabAddAsset.setOnClickListener {
            startActivity(Intent(this, PropertyHomeDetailActivity::class.java))
        }

        setupRecyclerView()
        loadAssetsData()
        setupSearchLogic()
    }

    private fun setupRecyclerView() {
        adapter = AssetDashboardAdapter(filteredList)
        binding.rvAssets.layoutManager = LinearLayoutManager(this)
        binding.rvAssets.adapter = adapter
    }

    private fun loadAssetsData() {
        db.collection("assets")
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) {
                    assetList.clear()
                    for (doc in snapshots) {
                        val asset = doc.toObject(AssetItem::class.java)
                        asset.id = doc.id
                        assetList.add(asset)
                    }

                    // GỌI HÀM LỌC: Để nạp toàn bộ dữ liệu từ assetList sang filteredList
                    performFilter(binding.edtSearchAsset.text.toString())
                }
            }
    }

    private fun setupSearchLogic() {
        binding.edtSearchAsset.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performFilter(keyword: String) {
        filteredList.clear() // Xóa dữ liệu cũ của danh sách bộ lọc công khai

        if (keyword.isEmpty()) {
            filteredList.addAll(assetList) // Nạp toàn bộ tài sản nếu không gõ từ khóa
        } else {
            for (item in assetList) {
                if (item.assetName.lowercase().contains(keyword.lowercase()) ||
                    item.roomName.lowercase().contains(keyword.lowercase())) {
                    filteredList.add(item)
                }
            }
        }

        // Kiểm tra để ẩn/hiển thị dòng chữ thông báo trống
        if (filteredList.isEmpty()) {
            binding.tvEmptyAsset.visibility = View.VISIBLE
        } else {
            binding.tvEmptyAsset.visibility = View.GONE
        }

        // Đẩy danh sách ArrayList đã nạp đầy đủ sang cho Adapter vẽ lên màn hình
        adapter.updateList(filteredList)
    }
}