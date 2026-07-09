package com.urcall.app.webrtc

import com.google.firebase.auth.FirebaseAuth

object AuthManager {

    fun signInIfNeeded(onReady: (uid: String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            onReady(currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                onReady(uid)
            }
            .addOnFailureListener {
                // wala munang connection, susubukan ulit sa susunod na pagbukas
            }
    }

    fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid
}
