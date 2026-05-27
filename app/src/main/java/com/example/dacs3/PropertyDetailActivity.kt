package com.example.dacs3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityPropertyDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityPropertyDetailBinding

    // Comment
    private lateinit var adapter:
            CommentAdapter

    private val commentList =
        mutableListOf<Comment>()

    private lateinit var firestore:
            FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPropertyDetailBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        firestore =
            FirebaseFirestore.getInstance()

        // GET DATA
        val title =
            intent.getStringExtra("title")

        val price =
            intent.getStringExtra("price")

        val location =
            intent.getStringExtra("location")

        val area =
            intent.getStringExtra("area")

        val image =
            intent.getStringExtra("image")

        val rating =
            intent.getFloatExtra(
                "rating",
                4f
            )

        // SET DATA
        binding.txtTitle.text =
            title

        binding.txtPrice.text =
            price

        binding.txtLocation.text =
            location

        binding.txtArea.text =
            area

        binding.ratingBar.rating =
            rating

        // LOAD IMAGE
        Glide.with(this)
            .load(image)

            .placeholder(
                android.R.drawable.ic_menu_gallery
            )

            .error(
                R.drawable.ic_house
            )

            .into(binding.imgProperty)

        // BACK
        binding.btnBack.setOnClickListener {

            finish()
        }

        // FAVORITE
        binding.btnFavorite.setOnClickListener {

            binding.btnFavorite.setImageResource(
                android.R.drawable.btn_star_big_on
            )
        }

        // GOOGLE MAP FREE
        binding.btnMap.setOnClickListener {

            val mapUri = Uri.parse(

                "https://www.google.com/maps/search/?api=1&query=$location"
            )

            val intent = Intent(
                Intent.ACTION_VIEW,
                mapUri
            )

            startActivity(intent)
        }

        // CHAT
        binding.btnChat.setOnClickListener {

            val intent = Intent(
                this,
                ChatActivity::class.java
            )

            startActivity(intent)
        }

        // COMMENT
        setupCommentRecycler()

        loadComments()

        binding.btnSendComment
            .setOnClickListener {

                sendComment()
            }
    }

    // RECYCLER
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

        firestore.collection("comments")
            .add(comment)

        binding.edtComment.text.clear()
    }

    // LOAD COMMENT
    private fun loadComments() {

        firestore.collection("comments")

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