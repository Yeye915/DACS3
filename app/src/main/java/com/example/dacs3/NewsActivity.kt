package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityNewsBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsActivity : HouseActivity() {
    private lateinit var binding: ActivityNewsBinding
    private val articles = mutableListOf<News>()
    private val filtered = mutableListOf<News>()
    private val adapter = NewsAdapter(filtered)
    private var loading = true
    private var loadFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerNews.layoutManager = LinearLayoutManager(this)
        binding.recyclerNews.adapter = adapter
        binding.edtSearchNews.doOnTextChanged { _, _, _, _ -> filterNews() }
        binding.btnAddBlog.setOnClickListener { startActivity(Intent(this, AddNewsActivity::class.java)) }
        binding.btnRetry.setOnClickListener {
            if (!binding.edtSearchNews.text.isNullOrBlank()) binding.edtSearchNews.text.clear()
            else loadNews()
        }
        binding.bottomNav.selectedItemId = R.id.nav_blog
        binding.bottomNav.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_rooms -> PropertyActivity::class.java
                R.id.nav_profile -> ProfileActivity::class.java
                else -> null
            }
            if (destination != null) {
                startActivity(Intent(this, destination))
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        loadNews()
    }

    private fun loadNews() {
        loading = true
        loadFailed = false
        renderState()
        Backend.db.collection("news").get()
            .addOnSuccessListener { documents ->
                if (isDestroyed) return@addOnSuccessListener
                loading = false
                articles.clear()
                documents.forEach { document ->
                    articles.add(document.toObject(News::class.java).apply { id = document.id })
                }
                filterNews()
            }
            .addOnFailureListener {
                if (isDestroyed) return@addOnFailureListener
                loading = false
                loadFailed = true
                filterNews()
            }
    }

    private fun filterNews() {
        val query = binding.edtSearchNews.text.toString().trim()
        filtered.clear()
        filtered.addAll(articles.filter {
            it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        })
        adapter.notifyDataSetChanged()
        renderState()
    }

    private fun renderState() {
        val searching = !binding.edtSearchNews.text.isNullOrBlank()
        binding.loadingIndicator.isVisible = loading && articles.isEmpty()
        binding.emptyState.isVisible = !loading && filtered.isEmpty()
        binding.recyclerNews.isVisible = filtered.isNotEmpty()
        binding.emptyTitle.setText(when {
            loadFailed -> R.string.connection_empty_title
            searching -> R.string.search_empty_title
            else -> R.string.news_empty_title
        })
        binding.emptyDescription.setText(when {
            loadFailed -> R.string.connection_empty_body
            searching -> R.string.search_empty_body
            else -> R.string.news_empty_body
        })
        binding.btnRetry.setText(if (searching) R.string.action_clear_search else R.string.action_retry)
    }
}
