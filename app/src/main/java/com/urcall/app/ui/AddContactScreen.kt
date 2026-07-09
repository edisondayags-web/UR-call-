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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.urcall.app.ui.theme.*

/**
 * Dito i-a-add ang isang tao gamit ang "URCall ID" nila
 * (parang paano ka nag-a-add ng contact sa Messenger gamit username).
 */
@Composable
fun AddContactScreen(onDone: () -> Unit) {
    var urCallId by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UrBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("Mag-add ng UR Contact", color = UrTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = urCallId,
                onValueChange = { urCallId = it },
                label = { Text("URCall ID ng kaibigan mo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UrNeon,
                    unfocusedBorderColor = UrTextGrey,
                    focusedTextColor = UrTextWhite,
                    unfocusedTextColor = UrTextWhite
                )
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val myUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (myUid == null || urCallId.isBlank()) {
                        statusMessage = "Kailangan naka-login ka at may ID na nailagay"
                        return@Button
                    }
                    // Sa v1: hinahanap natin ang user base sa urCallId nila sa "users/" node,
                    // tapos idadagdag sa "contacts/{myUid}/{theirUid}"
                    FirebaseDatabase.getInstance().getReference("users")
                        .orderByChild("urCallId").equalTo(urCallId)
                        .get().addOnSuccessListener { snapshot ->
                            val found = snapshot.children.firstOrNull()
                            if (found == null) {
                                statusMessage = "Walang nahanap na user na may ID na '$urCallId'"
                            } else {
                                val theirUid = found.key ?: return@addOnSuccessListener
                                FirebaseDatabase.getInstance()
                                    .getReference("contacts/$myUid/$theirUid")
                                    .setValue(true)
                                statusMessage = "Nadagdag na si $urCallId!"
                                onDone()
                            }
                        }
                        .addOnFailureListener {
                            statusMessage = "May error, subukan ulit"
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = UrNeon, contentColor = UrBlack),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I-add", fontWeight = FontWeight.Bold)
            }

            if (statusMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(statusMessage, color = UrPink, fontSize = 14.sp)
            }
        }
    }
}
