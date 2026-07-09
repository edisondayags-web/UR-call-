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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urcall.app.data.Contact
import com.urcall.app.ui.theme.*

@Composable
fun ContactsScreen(
    onAddClick: () -> Unit,
    onCallClick: (String) -> Unit
) {
    // TODO: palitan mo 'to ng totoong list galing Firebase (contacts/{myUid}/*)
    val contacts = remember {
        listOf(
            Contact(uid = "1", name = "Maya", isOnline = true),
            Contact(uid = "2", name = "Liam", isOnline = false),
            Contact(uid = "3", name = "Zara", isOnline = true),
            Contact(uid = "4", name = "Noah", isOnline = false),
            Contact(uid = "5", name = "Eli", isOnline = true)
        )
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
            // Top row: profile icon (left) + add contact icon (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBox(icon = Icons.Outlined.Person, size = 48.dp)
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

            // Big glowing call button + search/dialpad/filter row
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
                    IconBox(icon = Icons.Outlined.Search, size = 48.dp)
                    IconBox(icon = Icons.Outlined.GridView, size = 48.dp)
                    IconBox(icon = Icons.Filled.FilterList, size = 48.dp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Numbered contacts list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(contacts) { index, contact ->
                    ContactRow(
                        number = index + 1,
                        contact = contact,
                        onCallClick = { onCallClick(contact.uid) }
                    )
                }
            }
        }

        // Bottom nav bar
        BottomNavBar(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22000000))
            .border(1.dp, UrNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
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
        // number badge
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

        // avatar
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

        Text(
            text = contact.name,
            color = UrTextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // call button - only tappable / lit up kung online (naka-on ang data)
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

// helper for LazyColumn indexed items
private inline fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    list: List<Contact>,
    crossinline content: @Composable (Int, Contact) -> Unit
) {
    items(list.size) { index -> content(index, list[index]) }
}
