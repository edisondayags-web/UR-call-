package com.urcall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.ui.theme.*
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.CallRequestManager
import com.urcall.app.webrtc.ProfileManager

@Composable
fun RequestCallScreen(onRequestSent: (targetUid: String) -> Unit) {
    var myUrCallId by remember { mutableStateOf("") }
    var targetId by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = AuthManager.currentUid() ?: return@LaunchedEffect
        ProfileManager.ensureProfile(uid) { id -> myUrCallId = id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UrBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = UrNeon,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Mag-request ng Call", color = UrTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ipaste ang sariling ID mo at ang ID ng tatawagan mo",
                color = UrTextGrey,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            Text("Sarili mong ID", color = UrTextGrey, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = myUrCallId,
                onValueChange = { myUrCallId = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UrNeon,
                    unfocusedBorderColor = UrTextGrey,
                    focusedTextColor = UrTextWhite,
                    unfocusedTextColor = UrTextWhite
                )
            )

            Spacer(Modifier.height(16.dp))

            Text("ID ng tatawagan mo", color = UrTextGrey, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = targetId,
                onValueChange = { targetId = it },
                placeholder = { Text("i-paste dito ang ID nila") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UrNeon,
                    unfocusedBorderColor = UrTextGrey,
                    focusedTextColor = UrTextWhite,
                    unfocusedTextColor = UrTextWhite
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val uid = AuthManager.currentUid()
                    if (uid == null || targetId.isBlank()) {
                        statusMessage = "Kailangan naka-login ka at may nailagay na ID"
                        return@Button
                    }
                    isSending = true
                    CallRequestManager.sendRequest(uid, myUrCallId, targetId) { success, message, targetUid ->
                        isSending = false
                        statusMessage = message
                        if (success && targetUid != null) onRequestSent(targetUid)
                    }
                },
                enabled = !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = UrNeon, contentColor = UrBlack),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isSending) "Nagpapadala..." else "SEND REQUEST", fontWeight = FontWeight.Bold)
            }

            if (statusMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(statusMessage, color = UrPink, fontSize = 14.sp)
            }
        }
    }
}
