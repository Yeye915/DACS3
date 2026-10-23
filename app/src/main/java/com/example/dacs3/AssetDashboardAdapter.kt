package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AssetDashboardAdapter(
    // SỬA: Chuyển sang ArrayList để có thể xóa và thêm phần tử trực tiếp trong bộ nhớ adapter
    private val list: ArrayList<AssetItem>
) : RecyclerView.Adapter<AssetDashboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(android.R.id.text1)
        val txtSubTitle: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Hiển thị tên tài sản và số lượng chữ đậm
        holder.txtTitle.text = "${item.assetName} (Số lượng: ${item.quantity})"
        holder.txtTitle.paint.isFakeBoldText = true

        // Hiển thị vị trí phòng và tình trạng vật chất
        holder.txtSubTitle.text = "Vị trí: ${item.roomName} | Tình trạng: ${item.condition}"
    }

    override fun getItemCount(): Int = list.size

    // SỬA HÀM CẬP NHẬT: Xóa sạch danh sách cũ trong RAM và nạp dữ liệu mới
    fun updateList(newList: List<AssetItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged() // Ép RecyclerView vẽ lại giao diện ngay lập tức
    }
}