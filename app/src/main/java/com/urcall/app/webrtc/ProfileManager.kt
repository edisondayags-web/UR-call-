package com.urcall.app.webrtc

import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

object ProfileManager {

    fun ensureProfile(myUid: String, onReady: (urCallId: String) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$myUid")

        userRef.get().addOnSuccessListener { snapshot ->
            val existingId = snapshot.child("urCallId").getValue(String::class.java)
            if (existingId != null) {
                onReady(existingId)
            } else {
                val newId = generateUrCallId()
                userRef.setValue(
                    mapOf(
                        "urCallId" to newId,
                        "name" to "User $newId"
                    )
                )
                onReady(newId)
            }
        }
    }

    private fun generateUrCallId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
