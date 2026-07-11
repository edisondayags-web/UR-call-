package com.urcall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.webrtc.PresenceManager
import com.urcall.app.webrtc.SignalingManager
import com.urcall.app.webrtc.WebRTCClient
import com.urcall.app.ui.theme.*
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

@Composable
fun CallScreen(
    contactUid: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Kumokonekta...") }
    var isOnline by remember { mutableStateOf(false) }

    DisposableEffect(contactUid) {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val callId = listOf("me_uid_placeholder", contactUid).sorted().joinToString("_")
        val signaling = SignalingManager(callId)
        val webRtc = WebRTCClient(context)

        PresenceManager.observeContactStatus(contactUid) { online ->
            isOnline = online
            status = if (online) "Kumokonekta..." else "Kailangan naka-on ang data niya"
        }

        val peerConnection = webRtc.createPeerConnection(object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                signaling.sendIceCandidate(candidate, isCaller = true)
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    status = "Nakakonekta"
                }
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
            override fun onAddStream(p0: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(p0: org.webrtc.MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out org.webrtc.MediaStream>?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        })

        webRtc.addLocalAudioTrack()
        webRtc.createOffer { offer -> signaling.sendOffer(offer) }
        signaling.listenForAnswer { answer -> webRtc.setRemoteDescription(answer) }
        signaling.listenForCandidates(isCaller = true) { candidate -> webRtc.addIceCandidate(candidate) }

        com.urcall.app.webrtc.CallForegroundService.start(context)

        onDispose {
            webRtc.endCall()
            signaling.endCall()
            com.urcall.app.webrtc.CallForegroundService.stop(context)
            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(UrPinkGlow.copy(alpha = 0.4f), UrBlack))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(UrNeon.copy(alpha = 0.15f))
                    .border(2.dp, UrNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = UrNeon, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("UR Call", color = UrTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(status, color = UrTextGrey, fontSize = 15.sp)

            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(UrPink)
                    .border(2.dp, UrPinkGlow, CircleShape)
                    .clickable { onEndCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CallEnd,
                    contentDescription = "End call",
                    tint = UrTextWhite,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
