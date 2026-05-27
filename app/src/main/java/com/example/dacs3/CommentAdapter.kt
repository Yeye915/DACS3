package com.example.dacs3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemCommentBinding

class CommentAdapter(
    private val commentList: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(
        val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentViewHolder {

        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CommentViewHolder,
        position: Int
    ) {

        val comment = commentList[position]

        holder.binding.txtUsername.text =
            comment.username

        holder.binding.txtContent.text =
            comment.content
    }

    override fun getItemCount(): Int {

        return commentList.size
    }
}