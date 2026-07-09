package com.urcall.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
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
import com.urcall.app.ui.theme.*
import com.urcall.app.webrtc.AuthManager
import com.urcall.app.webrtc.CallRequestManager

@Composable
fun IncomingCallScreen(
    fromUid: String,
    fromUrCallId: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val mediaPlayer = try {
            val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
                context, RingtoneManager.TYPE_RINGTONE
            )
            MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }

        vibrateRinging(context)

        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            stopVibration(context)
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
            Text("Tumatawag sa'yo...", color = UrTextGrey, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(fromUrCallId, color = UrTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(60.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(UrPink)
                            .clickable {
                                CallRequestManager.clearRequest(
                                    AuthManager.currentUid() ?: "", fromUid
                                )
                                onDecline()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "Tanggihan", tint = UrTextWhite, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tanggihan", color = UrTextGrey, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(UrNeon)
                            .clickable {
                                CallRequestManager.clearRequest(
                                    AuthManager.currentUid() ?: "", fromUid
                                )
                                onAccept()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Sagutin", tint = UrBlack, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Sagutin", color = UrTextGrey, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun vibrateRinging(context: Context) {
    val pattern = longArrayOf(0, 800, 500)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
    } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }
}

private fun stopVibration(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator.cancel()
    } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }
}
