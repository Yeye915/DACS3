package com.example.dacs3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HostWalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_wallet)
        // Nhi có thể load dữ liệu từ Firestore vào RecyclerView ở đây nhé
    }
}