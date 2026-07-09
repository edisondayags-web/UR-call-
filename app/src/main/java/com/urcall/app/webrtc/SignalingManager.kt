package com.urcall.app.webrtc

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Ito yung "signaling" - parang telepono operator noon: hindi siya yung tumatawag,
 * pero siya ang kumokonekta sa dalawang linya bago sila mag-usap directly.
 *
 * Gamit natin ang Firebase Realtime Database bilang signaling channel kasi:
 *  - wala kang kailangang ihost na hiwalay na server
 *  - Firebase mismo yung "cloud" mo dito, at mabilis kahit mahinang data
 *
 * Flow: calls/{callId}/offer, calls/{callId}/answer, calls/{callId}/callerCandidates, calleeCandidates
 */
class SignalingManager(private val callId: String) {

    private val callRef = FirebaseDatabase.getInstance().getReference("calls/$callId")

    fun sendOffer(sdp: SessionDescription) {
        callRef.child("offer").setValue(mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description))
    }

    fun sendAnswer(sdp: SessionDescription) {
        callRef.child("answer").setValue(mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description))
    }

    fun sendIceCandidate(candidate: IceCandidate, isCaller: Boolean) {
        val path = if (isCaller) "callerCandidates" else "calleeCandidates"
        callRef.child(path).push().setValue(
            mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.sdp
            )
        )
    }

    fun listenForAnswer(onAnswer: (SessionDescription) -> Unit) {
        callRef.child("answer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.child("sdp").getValue(String::class.java) ?: return
                onAnswer(SessionDescription(SessionDescription.Type.ANSWER, sdp))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForOffer(onOffer: (SessionDescription) -> Unit) {
        callRef.child("offer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.child("sdp").getValue(String::class.java) ?: return
                onOffer(SessionDescription(SessionDescription.Type.OFFER, sdp))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForCandidates(isCaller: Boolean, onCandidate: (IceCandidate) -> Unit) {
        val path = if (isCaller) "calleeCandidates" else "callerCandidates"
        callRef.child(path).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val sdpMid = child.child("sdpMid").getValue(String::class.java) ?: continue
                    val sdpMLineIndex = child.child("sdpMLineIndex").getValue(Int::class.java) ?: continue
                    val candidate = child.child("candidate").getValue(String::class.java) ?: continue
                    onCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun endCall() {
        callRef.removeValue()
    }
}
