package com.urcall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.ui.theme.*
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.ProfileManager

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    var currentId by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var newId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = AuthManager.currentUid() ?: return@LaunchedEffect
        ProfileManager.ensureProfile(uid) { id -> currentId = id }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UrBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("Settings", color = UrTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Text("Kasalukuyang URCall ID mo:", color = UrTextGrey, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UrNeon.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(currentId.ifBlank { "..." }, color = UrNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            Text("Delete Your Old ID", color = UrPink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Kung may humawak ng ID mo na hindi mo gusto (hal. na-scam ka o naibigay mo sa maling tao), i-paste dito ang kasalukuyan mong ID para mapalitan ito ng bago. Hindi na magagamit ng iba ang lumang ID mo pagkatapos nito.",
                color = UrTextGrey,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmText,
                onValueChange = { confirmText = it },
                placeholder = { Text("I-paste ang kasalukuyan mong ID dito") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UrPink,
                    unfocusedBorderColor = UrTextGrey,
                    focusedTextColor = UrTextWhite,
                    unfocusedTextColor = UrTextWhite
                )
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val uid = AuthManager.currentUid()
                    if (uid == null) {
                        statusMessage = "Kailangan naka-login ka muna"
                        return@Button
                    }
                    if (confirmText.trim() != currentId) {
                        statusMessage = "Hindi tugma ang inilagay mo sa kasalukuyan mong ID"
                        return@Button
                    }
                    ProfileManager.regenerateId(uid) { generatedId ->
                        newId = generatedId
                        currentId = generatedId
                        confirmText = ""
                        statusMessage = "Nabago na ang ID mo. Bagong ID: $generatedId"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = UrPink, contentColor = UrTextWhite),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Your Old ID", fontWeight = FontWeight.Bold)
            }

            if (statusMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(statusMessage, color = UrNeon, fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))
            TextButton(onClick = onDone) {
                Text("Bumalik", color = UrTextGrey)
            }
        

Spacer(Modifier.height(20.dp))
Button(onClick = {
    val uid = AuthManager.currentUid() ?: return@Button
    ProfileManager.ensureProfile(uid) { myUrCallId ->
        CallRequestManager.sendTestRequestToSelf(uid, myUrCallId) { success, message ->
            // pwede mong ipakita sa Toast o Text kung gusto
        }
    }
}) {
    Text("Test Incoming Call (Debug)")
   }
