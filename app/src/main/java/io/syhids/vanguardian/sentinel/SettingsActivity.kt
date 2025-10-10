package io.syhids.vanguardian.sentinel

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.syhids.vanguardian.sentinel.ui.theme.VanGuardianSentinelTheme

private val TAG = SettingsActivity::class.java.simpleName

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val permissions = mutableListOf(Manifest.permission.CAMERA)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val cameraPermissionState = rememberMultiplePermissionsState(permissions)
            var serviceRunning by remember { mutableStateOf(isServiceRunning<SentinelService>()) }

            val runService: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!serviceRunning) {
                        Log.d(TAG, "Launching SentryService foreground service")
                        startForegroundService(
                            Intent(this, SentinelService::class.java)
                                .putExtra("interval", 4)
                        )
                        serviceRunning = true
                    }
                } else {
                    // TODO: Require opening activity
                }
            }

            val stopService: () -> Unit = {
                this.stopService(Intent(this, SentinelService::class.java))
                serviceRunning = false
            }

            VanGuardianSentinelTheme {
                Scaffold { paddingValues ->
                    val permissionsGranted = cameraPermissionState.allPermissionsGranted

                    LaunchedEffect(null) {
                        if (!serviceRunning && permissionsGranted) {
                            runService()
                        }
                    }

                    if (!permissionsGranted) {
                        ForegroundServiceConsent(
                            Modifier.padding(paddingValues),
                            cameraPermissionState
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (serviceRunning) {
                                Button(onClick = stopService) {
                                    Text("Stop service")
                                }
                            } else {
                                Button(onClick = runService) {
                                    Text("Start service")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun ForegroundServiceConsent(
        modifier: Modifier = Modifier,
        cameraPermissionState: MultiplePermissionsState,
    ) {
        if (cameraPermissionState.allPermissionsGranted) {
            Text("Permission accepted", textAlign = TextAlign.Center, modifier = modifier)
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .wrapContentSize()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textToShow = if (cameraPermissionState.shouldShowRationale) {
                    "You must accept the permission to run the foreground service"
                } else {
                    "Grant permissions to continue"
                }
                Text(textToShow, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchMultiplePermissionRequest() }) {
                    Text("Launch Permission Request")
                }
            }
        }
    }
}