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

    private lateinit var binding:
            ActivityPropertyBinding

    private lateinit var adapter:
            PropertyAdapter

    private val propertyList =
        mutableListOf<Property>()

    private val filteredList =
        mutableListOf<Property>()

    private lateinit var firestore:
            FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPropertyBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        firestore =
            FirebaseFirestore.getInstance()

        setupRecycler()

        setupSearch()

        loadProperties()

        // ADD PROPERTY
        binding.btnAdd.setOnClickListener {

            val intent = Intent(
                this,
                AddPropertyActivity::class.java
            )

            startActivity(intent)
        }
    }

    // SETUP RECYCLER
    private fun setupRecycler() {

        adapter =
            PropertyAdapter(filteredList)

        binding.recyclerProperty.layoutManager =
            GridLayoutManager(this, 2)

        binding.recyclerProperty.adapter =
            adapter
    }

    // LOAD FIREBASE
    private fun loadProperties() {

        firestore.collection("properties")

            .addSnapshotListener { value, error ->

                if (error != null) {

                    Toast.makeText(
                        this,
                        "Failed to load",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addSnapshotListener
                }

                propertyList.clear()

                filteredList.clear()

                value?.documents?.forEach {

                    val property =
                        it.toObject(
                            Property::class.java
                        )

                    if (property != null) {

                        propertyList.add(property)

                        filteredList.add(property)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }

    // SEARCH REALTIME
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

                        val text =
                            s.toString()
                                .lowercase()

                        filteredList.clear()

                        for (property in propertyList) {

                            if (
                                property.title
                                    .lowercase()
                                    .contains(text)
                            ) {

                                filteredList
                                    .add(property)
                            }
                        }

                        adapter.notifyDataSetChanged()
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                    }
                }
            )
    }
}