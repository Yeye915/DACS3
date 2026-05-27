package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAddPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPropertyBinding

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityAddPropertyBinding.inflate(layoutInflater)

        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        setupClick()
    }

    private fun setupClick() {

        binding.btnAddProperty.setOnClickListener {

            addProperty()
        }
    }

    private fun addProperty() {

        val title =
            binding.edtTitle.text.toString().trim()

        val price =
            binding.edtPrice.text.toString().trim()

        val location =
            binding.edtLocation.text.toString().trim()

        val bedroomText =
            binding.edtBedroom.text.toString().trim()

        val bathroomText =
            binding.edtBathroom.text.toString().trim()

        val area =
            binding.edtArea.text.toString().trim()

        val imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688"
        binding.edtImage.text.toString().trim()

        // Validate
        if (
            title.isEmpty() ||
            price.isEmpty() ||
            location.isEmpty() ||
            bedroomText.isEmpty() ||
            bathroomText.isEmpty() ||
            area.isEmpty() ||
            imageUrl.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please fill all fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Convert number safely
        val bedroom =
            bedroomText.toIntOrNull()

        val bathroom =
            bathroomText.toIntOrNull()

        if (bedroom == null || bathroom == null) {

            Toast.makeText(
                this,
                "Bedroom/Bathroom must be number",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Loading
        binding.btnAddProperty.isEnabled = false

        binding.btnAddProperty.text =
            "Adding..."

        val property = hashMapOf(

            "title" to title,

            "price" to price,

            "location" to location,

            "bedroom" to bedroom,

            "bathroom" to bathroom,

            "area" to area,

            "imageUrl" to imageUrl
        )

        firestore.collection("properties")
            .add(property)

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Property Added Successfully",
                    Toast.LENGTH_SHORT
                ).show()

                clearForm()

                binding.btnAddProperty.isEnabled = true

                binding.btnAddProperty.text =
                    "Add Property"
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed",
                    Toast.LENGTH_SHORT
                ).show()

                binding.btnAddProperty.isEnabled = true

                binding.btnAddProperty.text =
                    "Add Property"
            }
    }

    private fun clearForm() {

        binding.edtTitle.text?.clear()

        binding.edtPrice.text?.clear()

        binding.edtLocation.text?.clear()

        binding.edtBedroom.text?.clear()

        binding.edtBathroom.text?.clear()

        binding.edtArea.text?.clear()

        binding.edtImage.text?.clear()
    }
}