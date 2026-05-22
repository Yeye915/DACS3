package com.example.dacs3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemBlogBinding

class BlogAdapter(private val blogList: List<Blog>) : RecyclerView.Adapter<BlogAdapter.BlogViewHolder>() {

    class BlogViewHolder(val binding: ItemBlogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val binding = ItemBlogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        val blog = blogList[position]
        holder.binding.tvBlogTitle.text = blog.title
        holder.binding.tvBlogDate.text = blog.date
        holder.binding.tvBlogSummary.text = blog.content
    }

    override fun getItemCount(): Int = blogList.size
}