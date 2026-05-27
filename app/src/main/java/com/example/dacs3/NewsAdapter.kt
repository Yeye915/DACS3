package com.example.dacs3

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ItemNewsBinding

class NewsAdapter(
    private val newsList: List<News>
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(
        val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NewsViewHolder {

        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NewsViewHolder,
        position: Int
    ) {

        val news = newsList[position]

        holder.binding.txtCategory.text =
            news.category

        holder.binding.txtTitle.text =
            news.title

        holder.binding.txtDescription.text =
            news.description

        Glide.with(holder.itemView.context)
            .load(news.imageUrl)

            .placeholder(
                android.R.drawable.ic_menu_gallery
            )

            .into(holder.binding.imgNews)

        holder.itemView.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,
                NewsDetailActivity::class.java
            )

            intent.putExtra(
                "title",
                news.title
            )

            intent.putExtra(
                "description",
                news.description
            )

            intent.putExtra(
                "image",
                news.imageUrl
            )

            intent.putExtra(
                "category",
                news.category
            )

            intent.putExtra(
                "likes",
                news.likes
            )

            holder.itemView.context
                .startActivity(intent)
        }
    }

    override fun getItemCount(): Int {

        return newsList.size
    }
}