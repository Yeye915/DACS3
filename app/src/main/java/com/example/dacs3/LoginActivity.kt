package com.example.dacs3

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dacs3.databinding.ActivityLoginBinding

class LoginActivity : HouseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val register=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { if(it.resultCode==RESULT_OK) finishLogin() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);binding=ActivityLoginBinding.inflate(layoutInflater);setContentView(binding.root)
        binding.btnLogin.setOnClickListener {
            val email=binding.etUsername.text.toString().trim();val password=binding.etPassword.text.toString()
            binding.etUsername.error=if(email.isEmpty()) getString(R.string.auth_required_email) else null
            binding.etPassword.error=if(password.isEmpty()) getString(R.string.auth_required_password) else null
            if(email.isEmpty() || password.isEmpty()) return@setOnClickListener
            binding.btnLogin.isEnabled=false;binding.btnLogin.setText(R.string.auth_logging_in)
            Backend.auth.signInWithEmailAndPassword(email,password).addOnSuccessListener { finishLogin() }
                .addOnFailureListener { binding.btnLogin.isEnabled=true;binding.btnLogin.setText(R.string.auth_login);notice(getString(R.string.auth_login_failed)) }
        }
        binding.etPassword.imeOptions=EditorInfo.IME_ACTION_DONE
        binding.etPassword.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_DONE) { binding.btnLogin.performClick();true } else false }
        binding.tvGoToRegister.setOnClickListener { register.launch(Intent(this,RegisterActivity::class.java)) }
    }
    private fun finishLogin() {
        val user=Backend.auth.currentUser ?: return
        Backend.db.collection("Users").document(user.uid).get().addOnCompleteListener { task ->
            val role=if(task.isSuccessful) task.result?.getString("role") ?: "seeker" else "seeker"
            getSharedPreferences("AppPrefs",MODE_PRIVATE).edit().putString("user_role",role).putString("role_uid",user.uid).apply()
            if(!intent.getBooleanExtra("RETURN_TO_CALLER",false)) startActivity(Intent(this,MainActivity::class.java))
            setResult(RESULT_OK);finish()
        }
    }
}
