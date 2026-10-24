package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityNewsDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsDetailActivity : HouseActivity() {

    private lateinit var binding:
            ActivityNewsDetailBinding

    private val listeners=mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

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
            Backend.db

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

        binding.btnLike.setOnClickListener {
            if(requireAccount() && newsId!=null) {
                val like=firestore.collection("news").document(newsId).collection("likes").document(Backend.auth.currentUser!!.uid)
                binding.btnLike.isEnabled=false
                firestore.runTransaction { tx -> val exists=tx.get(like).exists();if(exists) tx.delete(like) else tx.set(like,mapOf("userUid" to Backend.auth.currentUser!!.uid));!exists }
                    .addOnSuccessListener { binding.btnLike.text=if(it) "Đã thích" else "Thích" }.addOnFailureListener(::failure).addOnCompleteListener { binding.btnLike.isEnabled=true }
            }
        }
        Backend.auth.currentUser?.uid?.let { uid -> if(newsId!=null) listeners.add(firestore.collection("news").document(newsId).collection("likes").document(uid).addSnapshotListener { doc,_ -> binding.btnLike.text=if(doc?.exists()==true) "Đã thích" else "Thích" }) }

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
        val text=binding.edtComment.text.toString().trim()
        val id=intent.getStringExtra("newsId") ?: return
        if(text.isBlank()) return
        if(text.length>2000) { binding.edtComment.error="Tối đa 2.000 ký tự";return }
        if(!requireAccount()) return
        binding.btnSendComment.isEnabled=false
        val user=Backend.auth.currentUser!!
        firestore.collection("news_comments").add(mapOf("newsId" to id,"userUid" to user.uid,"username" to (user.displayName ?: "Người dùng"),"content" to text,"timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener { if(binding.edtComment.text.toString().trim()==text) binding.edtComment.text.clear() }
            .addOnFailureListener(::failure).addOnCompleteListener { binding.btnSendComment.isEnabled=true }
    }

    // LOAD COMMENT
    private fun loadComments() {
        val newsId = intent.getStringExtra("newsId") ?: return

        listeners.add(firestore.collection("news_comments")
            .whereEqualTo("newsId", newsId) // CHỈ lấy bình luận của bài này
            .addSnapshotListener { value, error ->
                if(error!=null) { failure(error);return@addSnapshotListener }
                commentList.clear()
                value?.forEach { doc ->
                    val comment = doc.toObject(Comment::class.java)
                    commentList.add(comment)
                }
                adapter.notifyDataSetChanged()
            })
    }
    override fun onDestroy() { listeners.forEach { it.remove() };super.onDestroy() }
}