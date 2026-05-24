package com.example.dacs3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemHouseBinding

class HouseAdapter(
    private val houseList: List<House>,
    private val onItemClick: (House) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    inner class HouseViewHolder(val binding: ItemHouseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(house: House) {
            binding.tvHouseTitle.text = house.title
            binding.tvHouseAddress.text = house.address
            binding.tvHousePrice.text = house.price
            // Gán ảnh mẫu
            binding.ivHouseImage.setImageResource(house.imageResId)

            // Lắng nghe sự kiện click vào cả cái thẻ (Card)
            binding.root.setOnClickListener {
                onItemClick(house)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val binding = ItemHouseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HouseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(houseList[position])
    }

    override fun getItemCount(): Int = houseList.size
}