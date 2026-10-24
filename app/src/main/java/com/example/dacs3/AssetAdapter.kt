package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AssetAdapter(
    private val list: MutableList<AssetItem> // Đổi thành MutableList để cho phép thêm/xóa phần tử
) : RecyclerView.Adapter<AssetAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtAssetName)
        val txtQty: TextView = view.findViewById(R.id.txtQuantity)
        val btnDecrease: Button = view.findViewById(R.id.btnDecrease)
        val btnIncrease: Button = view.findViewById(R.id.btnIncrease)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteAsset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_asset_edit, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtName.text = item.assetName
        holder.txtQty.text = item.quantity.toString()

        // Nút giảm số lượng (tối thiểu là 1)
        holder.btnDecrease.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                holder.txtQty.text = item.quantity.toString()
            }
        }

        // Nút tăng số lượng
        holder.btnIncrease.setOnClickListener {
            if(item.quantity >= 1000) return@setOnClickListener
            item.quantity++
            holder.txtQty.text = item.quantity.toString()
        }

        // Nút xóa món đồ ra khỏi danh sách tạm thời
        holder.btnDelete.setOnClickListener {
            val current=holder.bindingAdapterPosition
            if(current==RecyclerView.NO_POSITION) return@setOnClickListener
            list.removeAt(current)
            notifyItemRemoved(current)
            notifyItemRangeChanged(current,list.size-current)
        }
    }

    override fun getItemCount(): Int = list.size
    fun updateList(newList: List<AssetItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}