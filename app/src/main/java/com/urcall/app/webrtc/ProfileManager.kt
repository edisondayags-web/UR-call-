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

    fun regenerateId(myUid: String, onDone: (newId: String) -> Unit) {
        val newId = generateUrCallId()
        FirebaseDatabase.getInstance().getReference("users/$myUid/urCallId")
            .setValue(newId)
            .addOnSuccessListener { onDone(newId) }
    }

    private fun generateUrCallId(): String {
        return (1..10).map { Random.nextInt(0, 10) }.joinToString("")
    }
}
