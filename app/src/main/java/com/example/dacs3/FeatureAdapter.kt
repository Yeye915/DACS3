package com.example.dacs3

// File: FeatureAdapter.kt
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemFeatureBinding

class FeatureAdapter(
    private val features: List<Feature>,
    private val onItemClick: (Feature) -> Unit // Truyền sự kiện click qua Lambda
) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    // Lớp ViewHolder kết nối với file item_feature.xml thông qua ViewBinding
    inner class FeatureViewHolder(private val binding: ItemFeatureBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(feature: Feature) {
            binding.tvFeatureTitle.text = feature.title
            binding.ivFeatureIcon.setImageResource(feature.iconResId)

            // Xử lý sự kiện click
            binding.root.setOnClickListener {
                onItemClick(feature)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding = ItemFeatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size
}