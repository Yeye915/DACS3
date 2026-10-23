package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityNewsBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsActivity : AppCompatActivity() {

    // ===================================
    // VIEW BINDING
    // ===================================

    private lateinit var binding:
            ActivityNewsBinding

    // ===================================
    // FIREBASE
    // ===================================

    private lateinit var firestore:
            FirebaseFirestore

    // ===================================
    // ADAPTER
    // ===================================

    private lateinit var adapter:
            NewsAdapter

    // ===================================
    // LIST
    // ===================================

    private val newsList =
        mutableListOf<News>()

    private val filteredList =
        mutableListOf<News>()

    // ===================================
    // ON CREATE
    // ===================================

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
        setupRecycler()

        setupSearch()

        setupBottomNavigation()

        setupButtons()


    }
    override fun onStart() {
        super.onStart()
        loadNews() // Tự động tải lại dữ liệu mới nhất
    }

    // ===================================
    // BUTTONS
    // ===================================

    private fun setupButtons() {

        // ADD BLOG
        binding.btnAddBlog
            .setOnClickListener {

                startActivity(

                    Intent(
                        this,
                        AddNewsActivity::class.java
                    )
                )
            }
    }

    // ===================================
    // RECYCLER
    // ===================================

    private fun setupRecycler() {

        adapter =
            NewsAdapter(filteredList)

        binding.recyclerNews.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerNews.adapter =
            adapter
    }

    // ===================================
    // FIREBASE
    // ===================================

    private fun loadNews() {

        firestore.collection("news")
            .get()

            .addOnSuccessListener { documents ->

                newsList.clear()

                filteredList.clear()

                for (document in documents) {

                    val news =
                        document.toObject(
                            News::class.java
                        )

                    news.id =
                        document.id

                    newsList.add(news)
                }

                filteredList.addAll(
                    newsList
                )

                adapter.notifyDataSetChanged()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Không tải được blogs",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ===================================
    // SEARCH
    // ===================================

    private fun setupSearch() {

        binding.edtSearchNews
            .addTextChangedListener(

                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        filterNews(
                            s.toString()
                        )
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {

                    }
                }
            )
    }

    // ===================================
    // FILTER
    // ===================================

    private fun filterNews(
        keyword: String
    ) {

        filteredList.clear()

        for (news in newsList) {

            if (

                news.title
                    .lowercase()
                    .contains(
                        keyword.lowercase()
                    )

                ||

                news.category
                    .lowercase()
                    .contains(
                        keyword.lowercase()
                    )
            ) {

                filteredList.add(news)
            }
        }

        adapter.notifyDataSetChanged()
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
}
