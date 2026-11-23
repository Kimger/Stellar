package roro.stellar.manager.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import roro.stellar.manager.R

internal const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

internal fun Context.canDiscoverLocalAdb() = Build.VERSION.SDK_INT < 37 ||
    checkSelfPermission(LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED

@Composable
internal fun NearbyDevicesPermission(onGranted: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var granted by remember { mutableStateOf(context.canDiscoverLocalAdb()) }
    var declined by rememberSaveable { mutableStateOf(false) }
    val latestOnGranted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        declined = !it
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.canDiscoverLocalAdb()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(granted) { if (granted) latestOnGranted() }
    if (granted) {
        Text(stringResource(R.string.nearby_devices_granted))
    } else {
        Button(onClick = {
            if (declined) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()))
            } else {
                launcher.launch(LOCAL_NETWORK)
            }
        }) {
            Text(stringResource(if (declined) R.string.nearby_devices_settings else R.string.nearby_devices_allow))
        }
    }
}
