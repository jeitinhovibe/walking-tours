package com.walkingtours.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ui.nav.WalkingToursNavHost
import com.walkingtours.app.ui.components.SystemBarBackdrop
import com.walkingtours.app.ui.theme.FootnotesTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        // This launcher asks for notifications at startup and location on tour start. Only a location
        // grant matters here: a tour begun before the user answered could not start its foreground
        // service, so retry now while the app is in front of the user.
        if (grants.keys.any {
            it == Manifest.permission.ACCESS_FINE_LOCATION ||
                it == Manifest.permission.ACCESS_COARSE_LOCATION
        }
        ) {
            // The location tracker reports a refusal to the UI itself if permission is missing.
            ServiceLocator.session.onLocationPermissionChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            val aiState by ServiceLocator.aiSettings.state.collectAsStateWithLifecycle()
            val darkBars = when (aiState.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            FootnotesTheme(themeMode = aiState.themeMode) {
                val background = MaterialTheme.colorScheme.background
                // The app is edge-to-edge. Paint the bars through the window for older Android
                // releases and through a Compose backdrop for Android 15+, which ignores window
                // colours; both use the same paper as the page.
                @Suppress("DEPRECATION")
                SideEffect {
                    window.statusBarColor = background.toArgb()
                    window.navigationBarColor = background.toArgb()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isStatusBarContrastEnforced = false
                        window.isNavigationBarContrastEnforced = false
                    }
                    WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                        controller.isAppearanceLightStatusBars = !darkBars
                        controller.isAppearanceLightNavigationBars = !darkBars
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        WalkingToursNavHost(
                            onRequestLocationPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                        )
                        SystemBarBackdrop()
                    }
                }
            }
        }
    }

    /**
     * Android 13+ requires runtime permission for notifications. The tour notification is how the
     * user knows location is still being tracked, so ask for it up front.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    override fun onDestroy() {
        // isFinishing is true when the user deliberately leaves the app (back press, or clearing it
        // from Recents) and false for a configuration change such as a rotation. Narration is
        // spoken by a separate system process, so it has to be stopped explicitly or it keeps
        // talking after the UI is gone.
        if (isFinishing) {
            ServiceLocator.session.shutdown()
        }
        super.onDestroy()
    }
}
