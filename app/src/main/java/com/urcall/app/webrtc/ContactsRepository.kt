package com.urcall.app.webrtc

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.urcall.app.data.Contact

object ContactsRepository {

    fun observeMyContacts(
        myUid: String,
        onUpdate: (List<Contact>) -> Unit
    ) {
        val contactsRef = FirebaseDatabase.getInstance().getReference("contacts/$myUid")

        contactsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val contactUids = snapshot.children.mapNotNull { it.key }

                if (contactUids.isEmpty()) {
                    onUpdate(emptyList())
                    return
                }

                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                usersRef.get().addOnSuccessListener { usersSnapshot ->
                    val contacts = contactUids.map { uid ->
                        val name = usersSnapshot.child(uid).child("name")
                            .getValue(String::class.java) ?: "Unknown"
                        Contact(uid = uid, name = name, isOnline = false)
                    }
                    onUpdate(contacts)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
