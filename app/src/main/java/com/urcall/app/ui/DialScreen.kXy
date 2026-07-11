package com.urcall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.data.Contact
import com.urcall.app.ui.theme.*
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.ContactsRepository
import com.urcall.app.webrtc.PresenceManager

@Composable
fun DialScreen(
    onCallClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val onlineStatus = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        AuthManager.signInIfNeeded { uid ->
            ContactsRepository.observeMyContacts(uid) { updated ->
                contacts = updated
                updated.forEach { contact ->
                    PresenceManager.observeContactStatus(contact.uid) { online ->
                        onlineStatus[contact.uid] = online
                    }
                }
            }
        }
    }

    val visibleContacts = if (searchQuery.isBlank()) {
        contacts
    } else {
        contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UrBlack)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22000000))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Balik", tint = UrNeon)
                }
                Spacer(Modifier.width(12.dp))
                Text("Tumawag", color = UrTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Hanapin ang tatawagan...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UrNeon,
                    unfocusedBorderColor = UrTextGrey,
                    focusedTextColor = UrTextWhite,
                    unfocusedTextColor = UrTextWhite
                )
            )

            Spacer(Modifier.height(20.dp))

            if (visibleContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Wala ka pang contact na pwedeng tawagan", color = UrTextGrey)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(visibleContacts.size) { index ->
                        val contact = visibleContacts[index]
                        val isOnline = onlineStatus[contact.uid] ?: false
                        DialRow(
                            contact = contact.copy(isOnline = isOnline),
                            onCallClick = { onCallClick(contact.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialRow(contact: Contact, onCallClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(enabled = contact.isOnline) { onCallClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (contact.isOnline) UrNeon.copy(alpha = 0.18f) else Color(0x22FF3D6B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = if (contact.isOnline) UrNeon else UrPink)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = UrTextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (contact.isOnline) "Naka-on ang data" else "Naka-off ang data",
                color = if (contact.isOnline) UrNeon else UrTextGrey,
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.Filled.Call,
            contentDescription = "Call ${contact.name}",
            tint = if (contact.isOnline) UrNeon else Color(0x66FFFFFF)
        )
    }
}
