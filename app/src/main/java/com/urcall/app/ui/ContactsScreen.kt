package com.urcall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.data.Contact
import com.urcall.app.ui.theme.*
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.ContactsRepository
import com.urcall.app.webrtc.PresenceManager
import com.urcall.app.webrtc.ProfileManager

@Composable
fun ContactsScreen(
    onAddClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onBellClick: () -> Unit,
    onIncomingCall: (fromUid: String, fromUrCallId: String) -> Unit
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val onlineStatus = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var myUrCallId by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AuthManager.signInIfNeeded { uid ->
            ProfileManager.ensureProfile(uid) { id -> myUrCallId = id }
            ContactsRepository.observeMyContacts(uid) { updated ->
                contacts = updated
                updated.forEach { contact ->
                    PresenceManager.observeContactStatus(contact.uid) { online ->
                        onlineStatus[contact.uid] = online
                    }
                }
            }
            com.urcall.app.webrtc.CallRequestManager.listenForIncomingRequests(uid) { fromUid, fromUrCallId ->
                onIncomingCall(fromUid, fromUrCallId)
            }
        }
    }

    val visibleContacts = if (searchQuery.isBlank()) {
        contacts
    } else {
        contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showProfileDialog) {
        MyProfileDialog(urCallId = myUrCallId, onDismiss = { showProfileDialog = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(UrPinkGlow.copy(alpha = 0.35f), UrBlack),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22000000))
                        .border(1.dp, UrNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = "Sariling profile mo", tint = UrNeon, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(UrNeon)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add contact", tint = UrBlack)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(UrNeon.copy(alpha = 0.12f))
                        .border(2.dp, UrNeon, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "Dial",
                        tint = UrNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBox(icon = Icons.Filled.NotificationsActive, onClick = onBellClick)
                    IconBox(icon = Icons.Outlined.Search, onClick = { showSearch = !showSearch })
                    IconBox(icon = Icons.Outlined.GridView, onClick = { onAddClick() })
                    IconBox(icon = Icons.Filled.FilterList, onClick = { })
                }
            }

            if (showSearch) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Hanapin ang contact...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UrNeon,
                        unfocusedBorderColor = UrTextGrey,
                        focusedTextColor = UrTextWhite,
                        unfocusedTextColor = UrTextWhite
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            if (contacts.isEmpty()) {
                EmptyContactsState(onAddClick = onAddClick)
            } else if (visibleContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Walang nahanap na tugma", color = UrTextGrey)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(visibleContacts) { index, contact ->
                        val isOnline = onlineStatus[contact.uid] ?: false
                        ContactRow(
                            number = index + 1,
                            contact = contact.copy(isOnline = isOnline),
                            onCallClick = { onCallClick(contact.uid) }
                        )
                    }
                }
            }
        }

        BottomNavBar(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MyProfileDialog(urCallId: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UrBlack,
        title = { Text("Sarili mong URCall ID", color = UrTextWhite) },
        text = {
            Column {
                Text(
                    "Ibigay mo 'to sa kaibigan mo para ma-add ka nila:",
                    color = UrTextGrey,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(UrNeon.copy(alpha = 0.12f))
                        .border(1.dp, UrNeon, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = urCallId.ifBlank { "..." },
                        color = UrNeon,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(urCallId))
                onDismiss()
            }) {
                Text("I-copy", color = UrNeon)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Isara", color = UrTextGrey)
            }
        }
    )
}

@Composable
private fun EmptyContactsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.PersonAddAlt,
            contentDescription = null,
            tint = UrTextGrey,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Wala ka pang contact",
            color = UrTextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "I-tap ang + sa taas para mag-add gamit ang URCall ID ng kaibigan mo",
            color = UrTextGrey,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = UrNeon, contentColor = UrBlack),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Mag-add ng contact", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22000000))
            .border(1.dp, UrNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = UrNeon, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ContactRow(number: Int, contact: Contact, onCallClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.5.dp, UrNeon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$number", color = UrNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (contact.isOnline) UrNeon.copy(alpha = 0.18f) else Color(0x22FF3D6B))
                .border(1.dp, if (contact.isOnline) UrNeon else UrPink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = if (contact.isOnline) UrNeon else UrPink)
        }
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = UrTextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (contact.isOnline) "Naka-on ang data" else "Naka-off ang data",
                color = if (contact.isOnline) UrNeon else UrTextGrey,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (contact.isOnline) UrNeon.copy(alpha = 0.15f) else Color(0x11FFFFFF))
                .border(1.dp, if (contact.isOnline) UrNeon else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .clickable(enabled = contact.isOnline) { onCallClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Call,
                contentDescription = "Call ${contact.name}",
                tint = if (contact.isOnline) UrNeon else Color(0x66FFFFFF)
            )
        }
    }
}

@Composable
private fun BottomNavBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xCC000000))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(Icons.Filled.Group, contentDescription = "Contacts", tint = UrNeon)
        Icon(Icons.Outlined.StarBorder, contentDescription = "Favorites", tint = UrTextGrey)
        Icon(Icons.Outlined.AccessTime, contentDescription = "Recents", tint = UrTextGrey)
        Icon(Icons.Outlined.GridView, contentDescription = "Dialpad", tint = UrTextGrey)
    }
}

private inline fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    list: List<Contact>,
    crossinline content: @Composable (Int, Contact) -> Unit
) {
    items(list.size) { index -> content(index, list[index]) }
}
