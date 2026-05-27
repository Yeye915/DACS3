package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.databinding.ActivityPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore

class PropertyActivity : AppCompatActivity() {

    // VIEW BINDING
    private lateinit var binding:
            ActivityPropertyBinding

    // ADAPTER
    private lateinit var adapter:
            PropertyAdapter

    // LIST
    private val propertyList =
        mutableListOf<Property>()

    private val filteredList =
        mutableListOf<Property>()

    // FIREBASE
    private lateinit var firestore:
            FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BINDING
        binding =
            ActivityPropertyBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // FIREBASE
        firestore =
            FirebaseFirestore.getInstance()

        // SETUP
        setupBottomNavigation()

        setupRecycler()

        setupSearch()

        setupAddButton()

        loadProperties()
    }

    // ===================================
    // BOTTOM NAVIGATION
    // ===================================

    private fun setupBottomNavigation() {

        binding.bottomNav.selectedItemId =
            R.id.nav_rooms

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

                        true
                    }

                    // BLOG
                    R.id.nav_blog -> {

                        startActivity(

                            Intent(
                                this,
                                NewsActivity::class.java
                            )
                        )

                        overridePendingTransition(0, 0)

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
            PropertyAdapter(filteredList)

        binding.recyclerProperty
            .layoutManager =
            GridLayoutManager(this, 2)

        binding.recyclerProperty
            .adapter =
            adapter
    }

    // ===================================
    // LOAD FIREBASE
    // ===================================

    private fun loadProperties() {

        firestore.collection("properties")
            .get()

            .addOnSuccessListener { documents ->

                propertyList.clear()

                filteredList.clear()

                for (document in documents) {

                    val property =
                        document.toObject(
                            Property::class.java
                        )

                    propertyList.add(property)
                }

                filteredList.addAll(propertyList)

                adapter.notifyDataSetChanged()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Không thể tải dữ liệu",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ===================================
    // SEARCH
    // ===================================

    private fun setupSearch() {

        binding.edtSearch
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

                        filterData(
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

    private fun filterData(
        keyword: String
    ) {

        filteredList.clear()

        for (property in propertyList) {

            if (

                property.title
                    ?.lowercase()
                    ?.contains(
                        keyword.lowercase()
                    ) == true ||

                property.location
                    ?.lowercase()
                    ?.contains(
                        keyword.lowercase()
                    ) == true
            ) {

                filteredList.add(property)
            }
        }

        adapter.notifyDataSetChanged()
    }

    // ===================================
    // ADD BUTTON
    // ===================================

    private fun setupAddButton() {

        binding.btnAddProperty
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Thêm phòng mới",
                    Toast.LENGTH_SHORT
                ).show()

                // TODO:
                // OPEN ADD PROPERTY SCREEN
            }
    }
}