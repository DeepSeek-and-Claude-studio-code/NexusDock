package com.nexus.phone

import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme {
                val call = ActiveCallHolder.currentCall
                var state by remember(call) { mutableIntStateOf(call?.state ?: Call.STATE_DISCONNECTED) }
                DisposableEffect(call) {
                    val callback = object : Call.Callback() {
                        override fun onStateChanged(call: Call, newState: Int) {
                            state = newState
                        }
                    }
                    call?.registerCallback(callback)
                    onDispose { call?.unregisterCallback(callback) }
                }
                val phoneNumber = call?.details?.handle?.schemeSpecificPart.orEmpty()
                val incoming = state == Call.STATE_RINGING
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(if (incoming) "Входящий вызов" else "Звонок", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(phoneNumber.ifBlank { "Неизвестный номер" }, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(36.dp))
                        if (incoming) {
                            Button(onClick = { call?.answer(VideoProfile.STATE_AUDIO_ONLY) }) { Text("Ответить") }
                            Spacer(Modifier.height(12.dp))
                        }
                        Button(onClick = {
                            call?.disconnect()
                            finish()
                        }) { Text(if (incoming) "Отклонить" else "Завершить звонок") }
                        if (state == Call.STATE_DISCONNECTED) {
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { finish() }) { Text("Закрыть") }
                        }
                    }
                }
            }
        }
    }
}
