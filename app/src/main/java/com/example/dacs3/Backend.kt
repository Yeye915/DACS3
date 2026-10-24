package com.example.dacs3

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HouseApplication : Application() {
    override fun onCreate() { super.onCreate(); Backend.initialize(this) }
}
object Backend {
    private lateinit var app: FirebaseApp
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance(app)
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance(app)
    val storage: FirebaseStorage get() = FirebaseStorage.getInstance(app)
    fun initialize(context: android.content.Context) {
        app = if (BuildConfig.USE_FIREBASE_EMULATORS) FirebaseApp.initializeApp(context,
            FirebaseOptions.Builder().setProjectId("demo-house").setApplicationId("1:123:android:demo")
                .setApiKey("demo-key").setStorageBucket("demo-house.appspot.com").build(), "local-tests")
        else FirebaseApp.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            auth.useEmulator("10.0.2.2", 9099); db.useEmulator("10.0.2.2", 8080)
            storage.useEmulator("10.0.2.2", 9199)
        }
    }
}
