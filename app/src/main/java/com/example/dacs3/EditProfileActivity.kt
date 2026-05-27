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
            val oldPass = binding.etOldPassword.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim() // Ô nhập mật khẩu MỚI

            val user = auth.currentUser ?: return@setOnClickListener

            // Bắt buộc nhập mật khẩu cũ để xác thực trước khi đổi thông tin nhạy cảm (Email/Pass)
            if (oldPass.isEmpty() && (newEmail != user.email || newPass.isNotEmpty())) {
                Toast.makeText(
                    this,
                    "Vui lòng nhập mật khẩu hiện tại để xác nhận thay đổi",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 1. Nếu có thay đổi Email hoặc muốn đổi Mật khẩu mới
            if (newEmail != user.email || newPass.isNotEmpty()) {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)

                user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {

                        // Đổi Email trên hệ thống Auth
                        user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {

                                // KIỂM TRA: Nếu Nhi có nhập mật khẩu mới thì thực hiện đổi luôn
                                if (newPass.isNotEmpty()) {
                                    if (newPass.length < 6) {
                                        Toast.makeText(
                                            this,
                                            "Mật khẩu mới phải từ 6 ký tự",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        user.updatePassword(newPass)
                                            .addOnCompleteListener { passTask ->
                                                if (!passTask.isSuccessful) {
                                                    Toast.makeText(
                                                        this,
                                                        "Lỗi đổi mật khẩu: ${passTask.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    }
                                }

                                // Sau khi xong các bước Auth, lưu thông tin hiển thị xuống Firestore
                                saveToFirestore(
                                    user.uid,
                                    name,
                                    phone,
                                    newEmail,
                                    if (newPass.isNotEmpty()) newPass else oldPass
                                )

                            } else {
                                Toast.makeText(
                                    this,
                                    "Lỗi đổi Email: ${emailTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Mật khẩu hiện tại không chính xác",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                // 2. Nếu không đổi Email/Pass, chỉ cập nhật Tên và SĐT
                saveToFirestore(user.uid, name, phone, user.email!!, oldPass)
            }
        }
    }

        // Cập nhật lại hàm saveToFirestore để lưu cả pass (nếu Nhi muốn quản lý pass trong DB)
        private fun saveToFirestore(uid: String, name: String, phone: String, email: String, pass: String) {
            val userUpdates = hashMapOf(
                "fullName" to name,
                "phone" to phone,
                "email" to email,
                "password" to pass // Lưu pass vào Firestore để Nhi dễ quản lý (tùy chọn)
            )

            db.collection("Users").document(uid)
                .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

}