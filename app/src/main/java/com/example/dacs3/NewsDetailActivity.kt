package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityNewsDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityNewsDetailBinding

    // COMMENT
    private lateinit var adapter:
            CommentAdapter

    private val commentList =
        mutableListOf<Comment>()

    private lateinit var firestore:
            FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityNewsDetailBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        firestore =
            FirebaseFirestore.getInstance()

        // GET DATA
        val title =
            intent.getStringExtra("title")

        val description =
            intent.getStringExtra("description")

        val image =
            intent.getStringExtra("image")

        val category =
            intent.getStringExtra("category")

        // SET DATA
        binding.txtTitle.text =
            title

        binding.txtDescription.text =
            description

        binding.txtCategory.text =
            category

        // LOAD IMAGE
        Glide.with(this)
            .load(image)

            .placeholder(
                android.R.drawable.ic_menu_gallery
            )

            .error(
                R.drawable.ic_delete
            )

            .into(binding.imgNews)

        // BACK
        binding.btnBack.setOnClickListener {

            finish()
        }

        // LIKE
        binding.btnLike.setOnClickListener {

            binding.btnLike.text =
                "Liked ❤️"
        }

        // SHARE
        binding.btnShare.setOnClickListener {

            val shareIntent = Intent()

            shareIntent.action =
                Intent.ACTION_SEND

            shareIntent.type =
                "text/plain"

            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "$title\n\n$description"
            )

            startActivity(

                Intent.createChooser(
                    shareIntent,
                    "Share News"
                )
            )
        }

        // COMMENT
        setupCommentRecycler()

        loadComments()

        binding.btnSendComment
            .setOnClickListener {

                sendComment()
            }
    }

    // SETUP RECYCLER
    private fun setupCommentRecycler() {

        adapter =
            CommentAdapter(commentList)

        binding.recyclerComment.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerComment.adapter =
            adapter
    }

    // SEND COMMENT
    private fun sendComment() {

        val text =
            binding.edtComment.text
                .toString()

        if (text.isEmpty()) {

            return
        }

        val comment = hashMapOf(

            "username" to "Guest",

            "content" to text
        )

        firestore.collection("news_comments")
            .add(comment)

        binding.edtComment.text.clear()
    }

    // LOAD COMMENT
    private fun loadComments() {

        firestore.collection("news_comments")

            .addSnapshotListener { value, _ ->

                commentList.clear()

                value?.documents?.forEach {

                    val comment =
                        it.toObject(
                            Comment::class.java
                        )

                    if (comment != null) {

                        commentList.add(comment)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }
}