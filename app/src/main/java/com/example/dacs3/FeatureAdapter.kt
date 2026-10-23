package com.example.dacs3

// File: FeatureAdapter.kt
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemFeatureBinding

class FeatureAdapter(
    private val features: List<Feature>,
    private val onItemClick: (Feature) -> Unit // Truyền sự kiện click qua Lambda
) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    // Lớp ViewHolder kết nối với file item_feature.xml thông qua ViewBinding
    inner class FeatureViewHolder(private val binding: ItemFeatureBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(feature: Feature) {
            binding.txtTitle.text = feature.title
            binding.imgIcon.setImageResource(feature.iconResId)

            // THÊM DÒNG NÀY ĐỂ ĐỔI TOÀN BỘ MÀU ICON SANG MÀU XANH ITRO:
            binding.imgIcon.setColorFilter(
                ContextCompat.getColor(itemView.context, R.color.itro_green)
            )

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
        val feature = features[position]
        holder.bind(feature)
    }

    override fun getItemCount(): Int = features.size
}
