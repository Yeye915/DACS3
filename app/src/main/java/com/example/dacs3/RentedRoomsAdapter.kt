package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RentedRoomsAdapter(private val list: List<HandoverRequest>) :
    RecyclerView.Adapter<RentedRoomsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtRoomName: TextView = view.findViewById(R.id.txtRentedRoomName)
        val txtRoomInfo: TextView = view.findViewById(R.id.txtRentedRoomInfo)

        val hostName: TextView = view.findViewById(R.id.txtHostName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rented_room, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtRoomName.text = item.roomName
        holder.txtRoomInfo.text = item.assetCheck
        holder.hostName.text = "Người cho thuê: ${item.hostName}"
    }

    override fun getItemCount(): Int = list.size
}