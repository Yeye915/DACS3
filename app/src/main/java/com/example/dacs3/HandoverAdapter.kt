package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HandoverAdapter(
    private val list: List<HandoverRequest>,
    private val onLongClick: (HandoverRequest) -> Unit
) : RecyclerView.Adapter<HandoverAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtRoom: TextView = view.findViewById(R.id.txtHandoverRoom)
        val txtTenant: TextView = view.findViewById(R.id.txtHandoverTenant)
        val txtAsset: TextView = view.findViewById(R.id.txtHandoverAsset)
        val txtStatus: TextView = view.findViewById(R.id.txtHandoverStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_handover_request, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtRoom.text = item.roomName
        holder.txtTenant.text = "Khách thuê: ${item.tenantName}"
        holder.txtAsset.text = "Ghi chú: ${item.assetCheck}"

        if (item.completed) {
            holder.txtStatus.text = "Đã hoàn thành"
            holder.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2ECC71.toInt()) // Màu xanh lá
        } else {
            holder.txtStatus.text = "Chưa hoàn thành"
            holder.txtStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt()) // Màu cam
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = list.size
}