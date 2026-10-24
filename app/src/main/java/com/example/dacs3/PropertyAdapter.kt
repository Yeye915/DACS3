package com.example.dacs3

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ItemPropertyBinding

class PropertyAdapter(private val propertyList: List<Property>) : RecyclerView.Adapter<PropertyAdapter.Holder>() {
    var favoriteIds: Set<String> = emptySet()
    class Holder(val binding: ItemPropertyBinding): RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup,type: Int)=Holder(ItemPropertyBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(holder: Holder,position: Int) {
        val p=propertyList[position];val b=holder.binding;val context=holder.itemView.context
        b.txtPrice.text=if(p.rent>0) "${RentalValidation.format(p.rent)}/tháng" else p.price.ifBlank { "Liên hệ giá" }
        b.txtTitle.text=p.title;b.txtLocation.text="${p.address} • ${p.status}";b.txtBedroom.text="${p.bedroom} phòng ngủ";b.txtBathroom.text="${p.bathroom} phòng tắm";b.txtArea.text=p.area;b.ratingBar.rating=p.rating
        Glide.with(context).load(p.imageUrl).placeholder(R.drawable.illustration_home).error(R.drawable.illustration_home).into(b.imgProperty)
        holder.itemView.setOnClickListener { context.startActivity(Intent(context,PropertyDetailActivity::class.java).putExtra("CHOSEN_PROPERTY",p)) }
        b.btnChat.setOnClickListener { holder.itemView.performClick() }
        val saved=p.id in favoriteIds
        b.imgFavorite.setColorFilter(ContextCompat.getColor(context,if(saved) R.color.error else R.color.primary))
        b.imgFavorite.contentDescription=if(saved) "Bỏ lưu phòng ${p.title}" else "Lưu phòng ${p.title}"
        b.imgFavorite.setOnClickListener {
            val activity=context as? HouseActivity ?: return@setOnClickListener
            if(!activity.requireAccount()) return@setOnClickListener
            b.imgFavorite.isEnabled=false
            Favorites.toggle(p.id).addOnSuccessListener { added ->
                favoriteIds=if(added) favoriteIds+p.id else favoriteIds-p.id
                if(holder.bindingAdapterPosition!=RecyclerView.NO_POSITION) notifyItemChanged(holder.bindingAdapterPosition)
            }.addOnFailureListener(activity::failure).addOnCompleteListener { b.imgFavorite.isEnabled=true }
        }
    }
    override fun getItemCount()=propertyList.size
}
