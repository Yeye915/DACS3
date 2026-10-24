package com.example.dacs3

import android.os.Bundle
import android.util.Patterns
import com.example.dacs3.databinding.ActivityEditProfileBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue

class EditProfileActivity : HouseActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityEditProfileBinding.inflate(layoutInflater);setContentView(binding.root)
        if(!requireAccount()) { finish();return }
        val user=Backend.auth.currentUser!!
        binding.etEditEmail.setText(user.email)
        Backend.db.collection("Users").document(user.uid).get().addOnSuccessListener { doc ->
            binding.etEditName.setText(doc.getString("fullName"));binding.etEditPhone.setText(doc.getString("phone"))
        }.addOnFailureListener(::failure)
        binding.btnSaveProfile.setOnClickListener {
            val name=binding.etEditName.text.toString().trim();val phone=binding.etEditPhone.text.toString().trim();val email=binding.etEditEmail.text.toString().trim()
            val old=binding.etOldPassword.text.toString();val new=binding.etNewPassword.text.toString()
            if(name.length !in 2..100 || phone.length !in 8..20 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { notice("Kiểm tra họ tên, số điện thoại và email");return@setOnClickListener }
            if(new.isNotEmpty() && new.length<6) { binding.etNewPassword.error="Mật khẩu tối thiểu 6 ký tự";return@setOnClickListener }
            val sensitive=email!=user.email || new.isNotEmpty()
            if(sensitive && old.isEmpty()) { binding.etOldPassword.error="Nhập mật khẩu hiện tại để xác nhận";return@setOnClickListener }
            binding.btnSaveProfile.isEnabled=false
            val verify=if(sensitive) user.reauthenticate(EmailAuthProvider.getCredential(user.email!!,old)).continueWith { it.result;null } else Tasks.forResult<Void>(null)
            verify.continueWithTask { checked ->
                if(!checked.isSuccessful) throw checked.exception!!
                if(new.isNotEmpty()) user.updatePassword(new) else Tasks.forResult<Void>(null)
            }.continueWithTask { changed ->
                if(!changed.isSuccessful) throw changed.exception!!
                if(email!=user.email) user.verifyBeforeUpdateEmail(email) else Tasks.forResult<Void>(null)
            }.continueWithTask { changed ->
                if(!changed.isSuccessful) throw changed.exception!!
                Backend.db.collection("Users").document(user.uid).update(mapOf("fullName" to name,"phone" to phone,"email" to user.email.orEmpty(),"password" to FieldValue.delete()))
            }.addOnSuccessListener {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                notice(if(email!=user.email) "Đã lưu hồ sơ. Mở email mới và xác minh để hoàn tất đổi email." else "Đã lưu hồ sơ")
                finish()
            }.addOnFailureListener { notice("Chưa hoàn tất cập nhật. Kiểm tra mật khẩu và kết nối; thay đổi đã được máy chủ chấp nhận sẽ được giữ lại.") }
                .addOnCompleteListener { binding.btnSaveProfile.isEnabled=true }
        }
    }
}
