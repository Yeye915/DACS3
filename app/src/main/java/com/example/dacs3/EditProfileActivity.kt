package com.example.dacs3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid ?: return

        // 1. Lấy dữ liệu hiện tại đổ vào ô nhập
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.etEditName.setText(doc.getString("fullName"))
                binding.etEditPhone.setText(doc.getString("phone"))
                binding.etEditEmail.setText(doc.getString("email"))
            }
        }

        // 2. Xử lý lưu dữ liệu
        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            val newPhone = binding.etEditPhone.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(this, "Vui lòng không để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "fullName" to newName,
                "phone" to newPhone
            )

            db.collection("Users").document(uid).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    finish() // Quay lại trang Profile
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                }
        }
        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text.toString().trim()
            val phone = binding.etEditPhone.text.toString().trim()
            val newEmail = binding.etEditEmail.text.toString().trim()
            val oldPass = binding.etOldPassword.text.toString().trim() // Ô nhập mật khẩu hiện tại

            val user = auth.currentUser ?: return@setOnClickListener

            // 1. Nếu Nhi có thay đổi Email
            if (newEmail != user.email) {
                if (oldPass.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập mật khẩu cũ để xác nhận đổi Email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Bước xác thực lại để vượt qua lỗi "Operation not allowed"
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Xác thực xong mới được phép gọi updateEmail
                        user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                saveToFirestore(user.uid, name, phone, newEmail)
                            } else {
                                Toast.makeText(this, "Lỗi: ${emailTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Mật khẩu hiện tại không chính xác", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 2. Nếu không đổi Email, chỉ lưu thông tin bình thường
                saveToFirestore(user.uid, name, phone, user.email!!)
            }
        }
        // Hàm này dùng để ghi dữ liệu xuống Firestore

        }
    private fun saveToFirestore(uid: String, name: String, phone: String, email: String) {
        // Tạo một Map chứa dữ liệu mới
        val userUpdates = hashMapOf(
            "fullName" to name,
            "phone" to phone,
            "email" to email // Đảm bảo email mới được đưa vào đây
        )

        // Sử dụng .set với SetOptions.merge() để tránh lỗi nếu tài khoản có thay đổi lớn
        db.collection("Users").document(uid)
            .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Đã cập nhật Firestore với Email: $email", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                // Nếu lỗi, nó sẽ hiện thông báo cụ thể ở đây
                Toast.makeText(this, "Lỗi Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}