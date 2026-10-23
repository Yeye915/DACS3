package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RepairAdapter(
    private val list: List<RepairRequest>,
    private val onLongClick: (RepairRequest) -> Unit
) : RecyclerView.Adapter<RepairAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtProperty: TextView = view.findViewById(R.id.txtRepairProperty)
        val txtDesc: TextView = view.findViewById(R.id.txtRepairDesc)
        val txtStatus: TextView = view.findViewById(R.id.txtRepairStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_repair_request, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtProperty.text = item.propertyTitle
        holder.txtDesc.text = item.description
        holder.txtStatus.text = item.status

        // Thay đổi màu sắc tag trạng thái theo tiến độ
        when (item.status) {
            "Đã xong" -> holder.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2ECC71.toInt()) // Xanh
            "Đang sửa" -> holder.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF3498DB.toInt()) // Xanh biển
            else -> holder.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt()) // Cam
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = list.size
}