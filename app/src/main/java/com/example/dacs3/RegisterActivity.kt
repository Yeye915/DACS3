package com.example.dacs3

import android.os.Bundle
import android.util.Patterns
import com.example.dacs3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : HouseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private var pendingUid: String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityRegisterBinding.inflate(layoutInflater);setContentView(binding.root)
        pendingUid=savedInstanceState?.getString("pendingUid")
        binding.btnRegisterSubmit.setOnClickListener { register() }
        binding.tvBackToLogin.setOnClickListener { finish() }
    }
    private fun register() {
        val name=binding.etFullName.text.toString().trim();val email=binding.etEmail.text.toString().trim();val phone=binding.etPhone.text.toString().trim()
        val password=binding.etPassReg.text.toString();val confirm=binding.etConfirmPass.text.toString()
        val role=if(binding.rbHost.isChecked) "host" else "seeker"
        if(name.length !in 2..100 || phone.length !in 8..20) { notice("Nhập họ tên từ 2 đến 100 ký tự và số điện thoại hợp lệ");return }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error="Email không hợp lệ";return }
        if(password.length<6) { binding.etPassReg.error="Mật khẩu tối thiểu 6 ký tự";return }
        if(password!=confirm) { binding.etConfirmPass.error="Mật khẩu xác nhận không khớp";return }
        binding.btnRegisterSubmit.isEnabled=false
        fun profile(uid: String) {
            val ref=Backend.db.collection("Users").document(uid)
            Backend.db.runTransaction { tx ->
                require(!tx.get(ref).exists()) { "Tài khoản đã có hồ sơ. Hãy đăng nhập." }
                tx.set(ref,mapOf("fullName" to name,"email" to email,"phone" to phone,"role" to role));null
            }.addOnSuccessListener {
                Backend.auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                getSharedPreferences("AppPrefs",MODE_PRIVATE).edit().putString("user_role",role).putString("role_uid",uid).apply()
                notice("Đăng ký thành công");setResult(RESULT_OK);finish()
            }.addOnFailureListener {
                binding.btnRegisterSubmit.isEnabled=true;binding.btnRegisterSubmit.text="Hoàn tất hồ sơ"
                notice("Tài khoản đã được tạo nhưng chưa lưu được hồ sơ. Giữ màn hình này và nhấn Hoàn tất hồ sơ để thử lại.")
            }
        }
        if(pendingUid!=null && Backend.auth.currentUser?.uid==pendingUid && Backend.auth.currentUser?.email==email) { profile(pendingUid!!);return }
        Backend.auth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {
            pendingUid=it.user!!.uid;profile(pendingUid!!)
        }.addOnFailureListener { binding.btnRegisterSubmit.isEnabled=true;notice("Không thể đăng ký. Email có thể đã được sử dụng; kiểm tra kết nối hoặc đăng nhập.") }
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("pendingUid",pendingUid);super.onSaveInstanceState(out) }
}
