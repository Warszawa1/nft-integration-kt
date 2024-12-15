package com.ireav.diabetesalert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope

import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Log.d("NFC_DEBUG", "NFC Adapter initialized: ${nfcAdapter != null}")

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EmergencyScreen(
                        nfcEnabled = nfcAdapter != null,
                        nfcActive = nfcAdapter?.isEnabled == true
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        Log.d("NFC_DEBUG", "NFC Foreground dispatch enabled")

    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        Log.d("NFC_DEBUG", "NFC Foreground dispatch disabled")

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("NFC_DEBUG", "onNewIntent received: ${intent.action}")

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            // Vibrate to confirm detection
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }

            Log.d("NFC_DEBUG", "NFC Tag detected, triggering email")

            // Show immediate visual feedback
            Toast.makeText(this, "NFC Tag detected! Sending alert...", Toast.LENGTH_SHORT).show()

            val emailSender = EmailSender(this)
            lifecycleScope.launch {
                try {
                    Log.d("NFC_DEBUG", "Attempting to send email")
                    val success = emailSender.sendEmail()
                    val message = if (success) {
                        "Emergency alert sent via NFC!"
                    } else {
                        "Failed to send alert. Please try again."
                    }
                    Log.d("NFC_DEBUG", "Email result: $success")
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("NFC_DEBUG", "Error sending email", e)
                    Toast.makeText(this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun EmergencyScreen(
    nfcEnabled: Boolean,
    nfcActive: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailSender = remember { EmailSender(context) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // NFC Status
        when {
            !nfcEnabled -> {
                Text(
                    text = "NFC not available on this device",
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            !nfcActive -> {
                Text(
                    text = "NFC is disabled. Please enable it in settings",
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            else -> {
                Text(
                    text = "NFC Ready - Tap tag for emergency",
                    color = Color.Green,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        // Test button
        Button(
            onClick = {
                val result = emailSender.testValues()
                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue
            )
        ) {
            Text("Test Values")
        }

        // Emergency button
        Button(
            onClick = {
                scope.launch {
                    val success = emailSender.sendEmail()
                    val message = if (success) {
                        "Emergency alert sent!"
                    } else {
                        "Failed to send alert. Please try again."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Text(
                text = "EMERGENCY\nALERT",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}
