package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PropertyActivity : HouseActivity() {
    private lateinit var binding: ActivityPropertyBinding
    private val properties = mutableListOf<Property>()
    private val filtered = mutableListOf<Property>()
    private val adapter = PropertyAdapter(filtered)
    private var listener: ListenerRegistration? = null
    private var loading = true
    private var loadFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerProperty.layoutManager = GridLayoutManager(this,
            if (resources.configuration.screenWidthDp >= 600) 2 else 1)
        binding.recyclerProperty.adapter = adapter
        binding.edtSearch.doOnTextChanged { _, _, _, _ -> filterProperties() }
        binding.btnAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }
        binding.btnRetry.setOnClickListener {
            if (!binding.edtSearch.text.isNullOrBlank()) binding.edtSearch.text.clear()
            else loadProperties()
        }
        binding.bottomNav.selectedItemId = R.id.nav_rooms
        binding.bottomNav.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_blog -> NewsActivity::class.java
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
        loadProperties()
    }

    private fun loadProperties() {
        loading = true
        loadFailed = false
        renderState()
        listener?.remove()
        listener = FirebaseFirestore.getInstance().collection("properties")
            .addSnapshotListener { snapshots, error ->
                loading = false
                loadFailed = error != null
                if (snapshots != null) {
                    properties.clear()
                    snapshots.forEach { document ->
                        properties.add(document.toObject(Property::class.java).apply { id = document.id })
                    }
                }
                filterProperties()
            }
    }

    private fun filterProperties() {
        val query = binding.edtSearch.text.toString().trim()
        filtered.clear()
        filtered.addAll(properties.filter {
            it.title.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
        })
        adapter.notifyDataSetChanged()
        renderState()
    }

    private fun renderState() {
        val searching = !binding.edtSearch.text.isNullOrBlank()
        binding.loadingIndicator.isVisible = loading && properties.isEmpty()
        binding.emptyState.isVisible = !loading && filtered.isEmpty()
        binding.recyclerProperty.isVisible = filtered.isNotEmpty()
        binding.emptyTitle.setText(when {
            loadFailed -> R.string.connection_empty_title
            searching -> R.string.search_empty_title
            else -> R.string.rooms_empty_title
        })
        binding.emptyDescription.setText(when {
            loadFailed -> R.string.connection_empty_body
            searching -> R.string.search_empty_body
            else -> R.string.rooms_empty_body
        })
        binding.btnRetry.setText(if (searching) R.string.action_clear_search else R.string.action_retry)
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }
}
