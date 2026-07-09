package com.urcall.app

import android.Manifest
import android.os.Bundle
import com.urcall.app.webrtc.AuthManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.urcall.app.ui.AddContactScreen
import com.urcall.app.ui.CallScreen
import com.urcall.app.ui.ContactsScreen
import com.urcall.app.ui.theme.URCallTheme

class MainActivity : ComponentActivity() {

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.signInIfNeeded { }
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            URCallTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "contacts") {
                    composable("contacts") {
                        ContactsScreen(
                            onAddClick = { navController.navigate("add_contact") },
                            onCallClick = { contactUid -> navController.navigate("call/$contactUid") }
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
