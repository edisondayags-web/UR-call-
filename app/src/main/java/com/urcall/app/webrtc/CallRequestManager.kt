package com.urcall.app.webrtc

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object CallRequestManager {

    fun sendRequest(
        myUid: String,
        myUrCallId: String,
        targetUrCallId: String,
        onResult: (success: Boolean, message: String, targetUid: String?) -> Unit
    ) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("urCallId").equalTo(targetUrCallId.trim().uppercase())
            .get()
            .addOnSuccessListener { snapshot ->
                val targetUid = snapshot.children.firstOrNull()?.key
                if (targetUid == null) {
                    onResult(false, "Walang nahanap na user na may ID na '$targetUrCallId'", null)
                    return@addOnSuccessListener
                }
                if (targetUid == myUid) {
                    onResult(false, "Hindi mo puwedeng tawagan ang sarili mong ID", null)
                    return@addOnSuccessListener
                }

                FirebaseDatabase.getInstance()
                    .getReference("callRequests/$targetUid/$myUid")
                    .setValue(
                        mapOf(
                            "fromUrCallId" to myUrCallId,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    )
                    .addOnSuccessListener {
                        onResult(true, "Naipadala ang request kay $targetUrCallId", targetUid)
                    }
                    .addOnFailureListener {
                        onResult(false, "May error, subukan ulit", null)
                    }
            }
            .addOnFailureListener {
                onResult(false, "May error, subukan ulit", null)
            }
    }

    fun listenForIncomingRequests(
        myUid: String,
        onIncoming: (fromUid: String, fromUrCallId: String) -> Unit
    ) {
        val ref = FirebaseDatabase.getInstance().getReference("callRequests/$myUid")
        ref.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val fromUid = snapshot.key ?: return
                val fromUrCallId = snapshot.child("fromUrCallId").getValue(String::class.java) ?: ""
                onIncoming(fromUid, fromUrCallId)
            }
            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun clearRequest(myUid: String, fromUid: String) {
        FirebaseDatabase.getInstance().getReference("callRequests/$myUid/$fromUid").removeValue()
    }
fun sendTestRequestToSelf(
        myUid: String,
        myUrCallId: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        FirebaseDatabase.getInstance()
            .getReference("callRequests/$myUid/$myUid")
            .setValue(
                mapOf(
                    "fromUrCallId" to myUrCallId,
                    "timestamp" to ServerValue.TIMESTAMP
                )
            )
            .addOnSuccessListener {
                onResult(true, "Naipadala ang test call sa sarili mo")
            }
            .addOnFailureListener {
                onResult(false, "May error, subukan ulit")
            }
    }
}
