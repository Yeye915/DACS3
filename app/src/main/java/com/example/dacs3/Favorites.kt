package com.example.dacs3

import com.google.android.gms.tasks.Task
object Favorites {
    fun toggle(propertyId: String): Task<Boolean> {
        val uid=checkNotNull(Backend.auth.currentUser?.uid)
        val ref=Backend.db.collection("Users").document(uid).collection("favorites").document(propertyId)
        return Backend.db.runTransaction { tx ->
            val exists=tx.get(ref).exists()
            if(exists) tx.delete(ref) else tx.set(ref,mapOf("propertyId" to propertyId,"savedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            !exists
        }
    }
}
