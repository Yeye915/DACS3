package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TenantHandoverAdapter(
    private val list: List<HandoverRequest>,
    private val onSignClick: (HandoverRequest) -> Unit
) : RecyclerView.Adapter<TenantHandoverAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtRoom: TextView = view.findViewById(R.id.txtTenantRoomName)
        val txtAsset: TextView = view.findViewById(R.id.txtTenantAssetCheck)
        val txtStatus: TextView = view.findViewById(R.id.txtTenantHandoverStatus)
        val btnSign: Button = view.findViewById(R.id.btnSignHandover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tenant_handover, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtRoom.text = item.roomName
        holder.txtAsset.text = "Tình trạng bàn giao: ${item.assetCheck}"

        if (item.completed) {
            holder.txtStatus.text = "Đã ký nhận bàn giao"
            holder.txtStatus.setTextColor(0xFF2ECC71.toInt()) // Màu xanh lá
            holder.btnSign.visibility = View.GONE // Ẩn nút ký nếu đã xong
        } else {
            holder.txtStatus.text = "Đang chờ bạn kiểm tra & ký nhận"
            holder.txtStatus.setTextColor(0xFFFF9800.toInt()) // Màu cam
            holder.btnSign.visibility = View.VISIBLE

            holder.btnSign.setOnClickListener { onSignClick(item) }
        }
    }

    override fun getItemCount(): Int = list.size
}