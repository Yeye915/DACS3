package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityNewsBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsActivity : AppCompatActivity() {

    // VIEW BINDING
    private lateinit var binding:
            ActivityNewsBinding

    // FIREBASE
    private lateinit var firestore:
            FirebaseFirestore

    // ADAPTER
    private lateinit var adapter:
            NewsAdapter

    // LIST
    private val newsList =
        mutableListOf<News>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BINDING
        binding =
            ActivityNewsBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // FIREBASE
        firestore =
            FirebaseFirestore.getInstance()

        // SETUP
        setupBottomNavigation()

        setupRecycler()

        setupAddButton()

        loadNews()
    }

    // ===================================
    // BOTTOM NAVIGATION
    // ===================================

    private fun setupBottomNavigation() {

        binding.bottomNav.selectedItemId =
            R.id.nav_blog

        binding.bottomNav
            .setOnItemSelectedListener { item ->

                when (item.itemId) {

                    // HOME
                    R.id.nav_home -> {

                        startActivity(

                            Intent(
                                this,
                                MainActivity::class.java
                            )
                        )

                        overridePendingTransition(0, 0)

                        true
                    }

                    // PROPERTY
                    R.id.nav_rooms -> {

                        startActivity(

                            Intent(
                                this,
                                PropertyActivity::class.java
                            )
                        )

                        overridePendingTransition(0, 0)

                        true
                    }

                    // BLOG
                    R.id.nav_blog -> {

                        true
                    }

                    // PROFILE
                    R.id.nav_profile -> {

                        startActivity(

                            Intent(
                                this,
                                ProfileActivity::class.java
                            )
                        )

                        overridePendingTransition(0, 0)

                        true
                    }

                    else -> false
                }
            }
    }

    // ===================================
    // RECYCLER VIEW
    // ===================================

    private fun setupRecycler() {

        adapter =
            NewsAdapter(newsList)

        binding.recyclerNews.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerNews.adapter =
            adapter
    }

    // ===================================
    // LOAD FIREBASE
    // ===================================

    private fun loadNews() {

        firestore.collection("news")
            .get()

            .addOnSuccessListener { documents ->

                newsList.clear()

                for (document in documents) {

                    val news =
                        document.toObject(
                            News::class.java
                        )

                    newsList.add(news)
                }

                adapter.notifyDataSetChanged()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Không thể tải blogs",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ===================================
    // ADD BLOG
    // ===================================

    private fun setupAddButton() {

        binding.btnAddBlog
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Thêm blog mới",
                    Toast.LENGTH_SHORT
                ).show()

                // TODO:
                // OPEN ADD BLOG SCREEN
            }
    }
}