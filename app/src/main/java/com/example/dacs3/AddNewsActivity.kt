package com.example.dacs3

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAddNewsBinding
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AddNewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNewsBinding
    private lateinit var firestore: FirebaseFirestore
    private var imageUri: Uri? = null

    // Thay API KEY của bạn vào đây (Lấy tại https://api.imgbb.com/)
    private val IMGBB_API_KEY = "e7d8779e9cfe41eb737396f40eea2808"

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.imgSelected.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSelectImage.setOnClickListener { getImage.launch("image/*") }
        binding.btnPost.setOnClickListener { addNews() }
    }

    private fun addNews() {
        val title = binding.edtTitle.text.toString().trim()
        val category = binding.edtCategory.text.toString().trim()
        val description = binding.edtDescription.text.toString().trim()

        if (title.isEmpty() || category.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ và chọn ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPost.isEnabled = false
        binding.btnPost.text = "Đang tải ảnh lên..."

        // Chuyển Uri thành File để gửi
        val file = uriToFile(imageUri!!)
        uploadImageToImgBB(file, title, category, description)
    }

    private fun uploadImageToImgBB(file: File, title: String, category: String, description: String) {
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
                    binding.btnPost.isEnabled = true
                    Toast.makeText(this@AddNewsActivity, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseData ->
                    if (response.isSuccessful) {
                        val json = JSONObject(responseData)
                        val url = json.getJSONObject("data").getString("url")

                        val news = News(
                            category = category,
                            title = title,
                            description = description,
                            imageUrl = url,
                            likes = 0,
                            comments = 0
                        )
                        runOnUiThread { saveToFirestore(news) }
                    } else {
                        runOnUiThread {
                            binding.btnPost.isEnabled = true
                            Toast.makeText(this@AddNewsActivity, "Tải ảnh thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun saveToFirestore(news: News) {
        firestore.collection("news").add(news)
            .addOnSuccessListener {
                Toast.makeText(this, "Đăng blog thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.btnPost.isEnabled = true
                Toast.makeText(this, "Lỗi Firestore", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm phụ biến Uri thành File
    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        return tempFile
    }
}