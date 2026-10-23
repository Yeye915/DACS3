package com.example.dacs3

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAddPropertyBinding
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AddPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPropertyBinding
    private lateinit var firestore: FirebaseFirestore

    // 1. Khai báo Uri và API Key ImgBB
    private var imageUri: Uri? = null

    // Lấy API Key tại: https://api.imgbb.com/
    private val IMGBB_API_KEY = "e7d8779e9cfe41eb737396f40eea2808"

    // 2. Bộ chọn ảnh từ thư viện
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            // Hiển thị ảnh lên ImageView (đảm bảo XML có id là imgSelected)
            binding.imgSelected.setImageURI(uri)
            Toast.makeText(this, "Đã chọn ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        setupClick()
    }

    private fun setupClick() {
        // Nút chọn ảnh
        binding.btnSelectImage.setOnClickListener {
            getImage.launch("image/*")
        }

        // Nút thêm nhà
        binding.btnAddProperty.setOnClickListener {
            addProperty()
        }
    }

    private fun addProperty() {
        val title = binding.edtTitle.text.toString().trim()
        val price = binding.edtPrice.text.toString().trim()
        val location = binding.edtLocation.text.toString().trim()
        val bedroomText = binding.edtBedroom.text.toString().trim()
        val bathroomText = binding.edtBathroom.text.toString().trim()
        val area = binding.edtArea.text.toString().trim()

        // Validate thông tin và ảnh
        if (title.isEmpty() || price.isEmpty() || location.isEmpty() ||
            bedroomText.isEmpty() || bathroomText.isEmpty() || area.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "Vui lòng chọn hình ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val bedroom = bedroomText.toIntOrNull()
        val bathroom = bathroomText.toIntOrNull()

        if (bedroom == null || bathroom == null) {
            Toast.makeText(this, "Bedroom/Bathroom must be number", Toast.LENGTH_SHORT).show()
            return
        }

        // Trạng thái Loading
        binding.btnAddProperty.isEnabled = false
        binding.btnAddProperty.text = "Uploading Image..."

        // 3. Quy trình: Tải ảnh lên ImgBB trước -> Lấy link -> Lưu Firestore
        val file = uriToFile(imageUri!!)
        uploadImageToImgBB(file, title, price, location, bedroom, bathroom, area)
    }

    private fun uploadImageToImgBB(
        file: File,
        title: String,
        price: String,
        location: String,
        bed: Int,
        bath: Int,
        area: String
    ) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", IMGBB_API_KEY)
            .addFormDataPart("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.btnAddProperty.isEnabled = true
                    binding.btnAddProperty.text = "Add Property"
                    Toast.makeText(
                        this@AddPropertyActivity,
                        "Upload ảnh thất bại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    val jsonObject = JSONObject(responseData)
                    val url = jsonObject.getJSONObject("data").getString("url")

                    // 4. Lưu thông tin vào Firestore sau khi đã có link ảnh
                    runOnUiThread {
                        savePropertyToFirestore(url, title, price, location, bed, bath, area)
                    }
                }
            }
        })
    }
    // Hàm phụ để biến Uri thành File tạm thời để gửi lên ImgBB
    private fun uriToFile(uri: android.net.Uri): java.io.File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = java.io.File.createTempFile("upload_house", ".jpg", cacheDir)
        val outputStream = java.io.FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }

    private fun savePropertyToFirestore(
        imageUrl: String,
        title: String,
        price: String,
        location: String,
        bed: Int,
        bath: Int,
        area: String
    ) {
        val property = hashMapOf(
            "title" to title,
            "price" to price,
            "location" to location,
            "bedroom" to bed,
            "bathroom" to bath,
            "area" to area,
            "imageUrl" to imageUrl
        )

        firestore.collection("properties")
            .add(property)
            .addOnSuccessListener {
                Toast.makeText(this, "Property Added Successfully", Toast.LENGTH_SHORT).show()
                finish() // Đóng màn hình sau khi thêm thành công
            }
            .addOnFailureListener {
                binding.btnAddProperty.isEnabled = true
                binding.btnAddProperty.text = "Add Property"
                Toast.makeText(this, "Lỗi Firestore", Toast.LENGTH_SHORT).show()
            }
    }
}