package com.example.dacs3

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityNewsBinding
import com.google.firebase.firestore.FirebaseFirestore

class NewsActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityNewsBinding

    private lateinit var adapter:
            NewsAdapter

    private val newsList =
        mutableListOf<News>()

    private val filteredList =
        mutableListOf<News>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityNewsBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        setupRecycler()

        setupSearch()

        loadNews()
    }

    private fun setupRecycler() {

        adapter =
            NewsAdapter(filteredList)

        binding.recyclerNews.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerNews.adapter =
            adapter
    }

    private fun loadNews() {

        FirebaseFirestore.getInstance()

            .collection("news")

            .get()

            .addOnSuccessListener { documents ->

                newsList.clear()

                filteredList.clear()

                for (document in documents) {

                    val news =
                        document.toObject(
                            News::class.java
                        )

                    newsList.add(news)

                    filteredList.add(news)
                }

                adapter.notifyDataSetChanged()
            }
    }

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

                        val text =
                            s.toString()
                                .lowercase()

                        filteredList.clear()

                        for (news in newsList) {

                            if (
                                news.title
                                    .lowercase()
                                    .contains(text)
                            ) {

                                filteredList
                                    .add(news)
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