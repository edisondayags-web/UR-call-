package com.urcall.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.PresenceManager
import com.urcall.app.webrtc.SignalingManager
import com.urcall.app.webrtc.WebRTCClient
import com.urcall.app.ui.theme.*
import kotlinx.coroutines.delay
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

@Composable
fun CallScreen(
    contactUid: String,
    isCaller: Boolean,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Kumokonekta...") }
    var isConnected by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var showCallInfo by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    var audioManagerRef by remember { mutableStateOf<android.media.AudioManager?>(null) }

    // Live call timer — only ticks once actually connected, resets to 00:00 otherwise
    LaunchedEffect(isConnected) {
        if (isConnected) {
            elapsedSeconds = 0
            while (true) {
                delay(1000)
                elapsedSeconds += 1
            }
        }
    }

    DisposableEffect(contactUid) {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            micDenied = true
            return@DisposableEffect onDispose {}
        }

        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        audioManagerRef = audioManager

        val myUid = AuthManager.currentUid() ?: ""
        val callId = listOf(myUid, contactUid).sorted().joinToString("_")
        val signaling = SignalingManager(callId)
        val webRtc = WebRTCClient(context)

        PresenceManager.observeContactStatus(contactUid) { online ->
            isOnline = online
            if (!isConnected) {
                status = if (online) "Kumokonekta..." else "Kailangan naka-on ang data niya"
            }
        }

        webRtc.createPeerConnection(object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                signaling.sendIceCandidate(candidate, isCaller = isCaller)
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    status = "Nakakonekta"
                    isConnected = true
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

        if (isCaller) {
            webRtc.createOffer { offer -> signaling.sendOffer(offer) }
            signaling.listenForAnswer { answer -> webRtc.setRemoteDescription(answer) }
        } else {
            signaling.listenForOffer { offer ->
                webRtc.setRemoteDescription(offer)
                webRtc.createAnswer { answer -> signaling.sendAnswer(answer) }
            }
        }
        signaling.listenForCandidates(isCaller = isCaller) { candidate -> webRtc.addIceCandidate(candidate) }
        signaling.listenForCallEnded { onEndCall() }

        com.urcall.app.webrtc.CallForegroundService.start(context)

        onDispose {
            webRtc.endCall()
            signaling.endCall()
            com.urcall.app.webrtc.CallForegroundService.stop(context)
            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        }
    }

    fun formatDuration(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(UrPinkGlow.copy(alpha = 0.4f), UrBlack)))
    ) {
        // Top bar: back + overflow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22000000))
                    .clickable { onEndCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Balik", tint = UrPink)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, UrPink.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Iba pa", tint = UrPink)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                formatDuration(elapsedSeconds),
                color = UrTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            if (isConnected) {
                LiveWaveform()
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingAvatar(isConnected = isConnected)

            Spacer(Modifier.height(20.dp))
            Text("UR Call", color = UrTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (micDenied) "Kailangan ng mic permission - buksan sa Settings ng phone mo" else status,
                color = if (micDenied) UrPink else UrTextGrey,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, UrNeon.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = UrNeon, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Secure Call", color = UrNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(50.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                CallActionButton(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = "Mute",
                    active = isMuted,
                    onClick = {
                        isMuted = !isMuted
                        audioManagerRef?.isMicrophoneMute = isMuted
                    }
                )
                CallActionButton(
                    icon = Icons.Filled.Dialpad,
                    label = "Keypad",
                    active = false,
                    enabled = false,
                    onClick = {}
                )
                CallActionButton(
                    icon = Icons.Filled.VolumeUp,
                    label = "Speaker",
                    active = isSpeakerOn,
                    onClick = {
                        isSpeakerOn = !isSpeakerOn
                        audioManagerRef?.isSpeakerphoneOn = isSpeakerOn
                    }
                )
                CallActionButton(
                    icon = Icons.Filled.PersonAdd,
                    label = "Add Call",
                    active = false,
                    enabled = false,
                    onClick = {}
                )
            }

            Spacer(Modifier.height(40.dp))

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
            Spacer(Modifier.height(8.dp))
            Text("Hang Up", color = UrTextGrey, fontSize = 13.sp)
        }

        // Call info panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xCC0A0002))
                .border(1.dp, UrPink.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .animateContentSize()
                .clickable { showCallInfo = !showCallInfo }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Call Info", color = UrPink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(
                    if (showCallInfo) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = UrPink
                )
            }
            if (showCallInfo) {
                Spacer(Modifier.height(12.dp))
                CallInfoRow(Icons.Filled.Call, "Call Type", "Voice Call")
                Spacer(Modifier.height(10.dp))
                CallInfoRow(Icons.Filled.Schedule, "Duration", formatDuration(elapsedSeconds))
                Spacer(Modifier.height(10.dp))
                CallInfoRow(Icons.Filled.VerifiedUser, "Encryption", "End-to-End")
            }
        }
    }
}

@Composable
private fun CallInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = UrTextGrey, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = UrTextGrey, fontSize = 14.sp)
        }
        Text(value, color = UrTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (active) UrPink.copy(alpha = 0.25f) else Color(0x22FF3D6B))
                .border(1.dp, UrPink.copy(alpha = if (enabled) 0.6f else 0.2f), CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) UrPink else UrPink.copy(alpha = 0.35f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (enabled) UrTextGrey else UrTextGrey.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun PulsingAvatar(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnected) 6000 else 12000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(180.dp)
                .rotate(rotation)
        ) {
            val dotCount = 48
            val radius = size.minDimension / 2f
            for (i in 0 until dotCount) {
                val angle = (i * (360f / dotCount)) * (Math.PI / 180f)
                val dotRadius = if (i % 3 == 0) 2.8f else 1.6f
                val x = center.x + radius * kotlin.math.cos(angle).toFloat()
                val y = center.y + radius * kotlin.math.sin(angle).toFloat()
                drawCircle(
                    color = UrPink.copy(alpha = if (isConnected) 0.8f else 0.35f),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }

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
    }
}

@Composable
private fun LiveWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val barCount = 9
        for (i in 0 until barCount) {
            val heightFactor by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420 + (i * 60), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((18.dp.value * heightFactor).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(UrPink)
            )
        }
    }
}
