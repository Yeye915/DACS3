package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoomAssetGroupAdapter(private val roomList: List<RoomAssetGroup>) :
    RecyclerView.Adapter<RoomAssetGroupAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomCode: TextView = view.findViewById(R.id.tvRoomCode)
        val tvRoomStatus: TextView = view.findViewById(R.id.tvRoomStatus)
        val tvAssetsSummary: TextView = view.findViewById(R.id.tvAssetsSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_asset_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = roomList[position]
        holder.tvRoomCode.text = "Mã phòng: ${item.roomCode}"
        holder.tvRoomStatus.text = item.status

        // Tạo chuỗi tóm tắt tài sản có định dạng xuống dòng cho dễ nhìn
        val summary = StringBuilder()
        item.assets.forEachIndexed { index, asset ->
            summary.append("• ${asset.name} (${asset.quantity}) - Tình trạng: ${asset.quality}")
            if (index < item.assets.size - 1) {
                summary.append("\n")
            }
        }
        holder.tvAssetsSummary.text = if (summary.isNotEmpty()) summary.toString() else "Không có tài sản"
    }

    override fun getItemCount(): Int = roomList.size
}