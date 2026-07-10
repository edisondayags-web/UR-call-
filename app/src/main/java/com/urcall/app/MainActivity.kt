package com.urcall.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.urcall.app.ui.AddContactScreen
import com.urcall.app.ui.CallScreen
import com.urcall.app.ui.ContactsScreen
import com.urcall.app.ui.IncomingCallScreen
import com.urcall.app.ui.RequestCallScreen
import com.urcall.app.ui.SettingsScreen
import com.urcall.app.ui.theme.URCallTheme
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.PresenceManager

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        AuthManager.signInIfNeeded {
            PresenceManager.startPresenceTracking(this)
        }

        setContent {
            URCallTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "contacts") {
                    composable("contacts") {
                        ContactsScreen(
                            onAddClick = { navController.navigate("request_call") },
                            onCallClick = { contactUid -> navController.navigate("call/$contactUid") },
                            onBellClick = { navController.navigate("request_call") },
                            onIncomingCall = { fromUid, fromUrCallId ->
                                navController.navigate("incoming_call/$fromUid/$fromUrCallId")
                            },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onDone = { navController.popBackStack() })
                    }
                    composable("incoming_call/{fromUid}/{fromUrCallId}") { backStackEntry ->
                        val fromUid = backStackEntry.arguments?.getString("fromUid") ?: ""
                        val fromUrCallId = backStackEntry.arguments?.getString("fromUrCallId") ?: ""
                        IncomingCallScreen(
                            fromUid = fromUid,
                            fromUrCallId = fromUrCallId,
                            onAccept = { navController.navigate("call/$fromUid") { popUpTo("contacts") } },
                            onDecline = { navController.popBackStack() }
                        )
                    }
                    composable("request_call") {
                        RequestCallScreen(
                            onRequestSent = { targetUid -> navController.navigate("call/$targetUid") }
                        )
                    }
                    composable("add_contact") {
                        AddContactScreen(onDone = { navController.popBackStack() })
                    }
                    composable("call/{contactUid}") { backStackEntry ->
                        val contactUid = backStackEntry.arguments?.getString("contactUid") ?: ""
                        CallScreen(
                            contactUid = contactUid,
                            onEndCall = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
