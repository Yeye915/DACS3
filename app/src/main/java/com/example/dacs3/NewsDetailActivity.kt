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
        val newsId = intent.getStringExtra("newsId")
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val image = intent.getStringExtra("imageUrl")
        val category = intent.getStringExtra("category")

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
                "Đã thích ❤️"
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
        val text = binding.edtComment.text.toString().trim()
        val newsId = intent.getStringExtra("newsId") // Lấy ID bài viết từ Intent

        if (text.isEmpty() || newsId == null) return

        val comment = hashMapOf(
            "newsId" to newsId, // Lưu ID bài để biết bình luận này của bài nào
            "username" to "Người dùng",
            "content" to text,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("news_comments").add(comment)
        binding.edtComment.text.clear()
    }

    // LOAD COMMENT
    private fun loadComments() {
        val newsId = intent.getStringExtra("newsId")

        firestore.collection("news_comments")
            .whereEqualTo("newsId", newsId) // CHỈ lấy bình luận của bài này
            .addSnapshotListener { value, _ ->
                commentList.clear()
                value?.forEach { doc ->
                    val comment = doc.toObject(Comment::class.java)
                    commentList.add(comment)
                }
                adapter.notifyDataSetChanged()
            }
    }
}