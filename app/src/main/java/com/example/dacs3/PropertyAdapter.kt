package com.example.dacs3

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ItemPropertyBinding

class PropertyAdapter(
    private val propertyList: List<Property>
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(
        val binding: ItemPropertyBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PropertyViewHolder {

        val binding = ItemPropertyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PropertyViewHolder,
        position: Int
    ) {

        val property = propertyList[position]

        // Price
        holder.binding.txtPrice.text =
            property.price

        // Title
        holder.binding.txtTitle.text =
            property.title

        // Location
        holder.binding.txtLocation.text =
            property.location

        // Bedroom
        holder.binding.txtBedroom.text =
            "${property.bedroom} Beds"

        // Bathroom
        holder.binding.txtBathroom.text =
            "${property.bathroom} Baths"

        // Area
        holder.binding.txtArea.text =
            property.area

        // Rating
        holder.binding.ratingBar.rating =
            property.rating

        // Load Image
        Glide.with(holder.itemView.context)
            .load(property.imageUrl)

            .placeholder(
                android.R.drawable.ic_menu_gallery
            )

            .error(
                R.drawable.ic_house
            )

            .into(holder.binding.imgProperty)

        // Open Detail
        holder.itemView.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,
                PropertyDetailActivity::class.java
            )

            intent.putExtra(
                "title",
                property.title
            )

            intent.putExtra(
                "price",
                property.price
            )

            intent.putExtra(
                "location",
                property.location
            )

            intent.putExtra(
                "area",
                property.area
            )

            intent.putExtra(
                "image",
                property.imageUrl
            )

            intent.putExtra(
                "rating",
                property.rating
            )

            holder.itemView.context.startActivity(
                intent
            )
        }

        // Favorite
        holder.binding.imgFavorite
            .setOnClickListener {

                Toast.makeText(
                    holder.itemView.context,
                    "Added to Favorite ❤️",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // Open Chat
        holder.binding.btnChat.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,
                ChatActivity::class.java
            )

            holder.itemView.context.startActivity(
                intent
            )
        }
    }

    override fun getItemCount(): Int {

        return propertyList.size
    }
}