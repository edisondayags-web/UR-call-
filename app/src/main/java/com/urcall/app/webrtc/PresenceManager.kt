package com.urcall.app.webrtc

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

/**
 * Ito yung "uyy si Juan naka-on ang data, bigyan natin ng connection" na part.
 *
 * Paano gumagana:
 * 1. Naka-connect ang phone sa Firebase Realtime Database (kahit mahina lang ang data, gagana 'to).
 * 2. Firebase mismo ang may built-in na ".info/connected" node - kaya alam nito
 *    real-time kung online o offline ang device, kahit hindi mo tina-track manually.
 * 3. Gamit ang onDisconnect(), kapag na-off ang data o namatay ang connection,
 *    OTOMATIKONG mababago ang status sa "offline" - kahit nag-crash o nawalan ng
 *    signal bigla ang user (hindi na kailangan mag-timeout pa).
 */
object PresenceManager {

    fun startPresenceTracking(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance()
        val myStatusRef = db.getReference("presence/$uid")
        val connectedRef = db.getReference(".info/connected")

        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                if (isConnected) {
                    // Pag na-cut ang connection (data off, walang signal, force-close),
                    // ito ang awtomatikong isusulat ng Firebase server mismo.
                    myStatusRef.onDisconnect().setValue(
                        mapOf(
                            "online" to false,
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                    )
                    // Habang naka-on ang data ngayon, markahan nating online.
                    myStatusRef.setValue(
                        mapOf(
                            "online" to true,
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                    )
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // walang connection ngayon, wala tayong magagawa dito - normal lang 'to
            }
        })
    }

    fun observeContactStatus(
        contactUid: String,
        onStatusChanged: (Boolean) -> Unit
    ) {
        FirebaseDatabase.getInstance().getReference("presence/$contactUid/online")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    onStatusChanged(snapshot.getValue(Boolean::class.java) ?: false)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }
}
