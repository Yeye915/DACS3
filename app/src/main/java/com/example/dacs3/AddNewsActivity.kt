package com.example.dacs3

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dacs3.databinding.ActivityAddNewsBinding
import com.google.firebase.firestore.FieldValue
class AddNewsActivity : HouseActivity() {
    private lateinit var binding: ActivityAddNewsBinding
    private var image: Uri?=null
    private var uploaded: String?=null
    private var posting=false
    private val picker=registerForActivityResult(ActivityResultContracts.GetContent()) { if(it!=null) { image=it;uploaded=null;binding.imgSelected.setImageURI(it) } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityAddNewsBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        binding.btnBack.setOnClickListener { finish() };binding.btnSelectImage.setOnClickListener { picker.launch("image/*") }
        binding.btnPost.setOnClickListener {
            if(posting) return@setOnClickListener
            val title=binding.edtTitle.text.toString().trim();val category=binding.edtCategory.text.toString().trim();val text=binding.edtDescription.text.toString().trim()
            if(title.length !in 3..150 || category.length !in 1..80 || text.length !in 10..20000) { notice("Nhập tiêu đề 3–150 ký tự, danh mục và nội dung 10–20.000 ký tự");return@setOnClickListener }
            posting=true;binding.btnPost.isEnabled=false
            fun restore() { posting=false;binding.btnPost.isEnabled=true }
            fun publish(url: String) {
                Backend.db.collection("news").add(mapOf("authorUid" to Backend.auth.currentUser!!.uid,"title" to title,"category" to category,"description" to text,"imageUrl" to url,"likes" to 0,"comments" to 0,"createdAt" to FieldValue.serverTimestamp()))
                    .addOnSuccessListener { notice("Đã đăng bài viết");finish() }.addOnFailureListener { failure(it);restore() }
            }
            if(image!=null && uploaded==null) ImageUpload.upload(this,image!!).addOnSuccessListener { uploaded=it;publish(it) }.addOnFailureListener { failure(it);restore() }
            else publish(uploaded.orEmpty())
        }
    }
}
