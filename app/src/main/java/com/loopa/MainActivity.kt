package com.loopa.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loopa.network.NetworkModule
import com.loopa.ui.*
import com.loopa.ui.theme.MyApplicationTheme
import com.loopa.viewmodel.AuthViewModel
import com.loopa.viewmodel.MediaViewModel
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetworkModule.prewarmConnections()
        setContent {
            val viewModel: MediaViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                MediaTrackerApp(viewModel)
            }
        }
    }
}

@Composable
fun MediaTrackerApp(
    viewModel: MediaViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val hazeState = rememberHazeState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "discover"
    val isRateLimited by viewModel.isRateLimited.collectAsState()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()
    var isGuestMode by remember { mutableStateOf(false) }

    // Connectivity callback — flushes offline write queue when device goes online
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.flushPendingOps()
            }
        }
        cm.registerNetworkCallback(request, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    LaunchedEffect(isGuestMode, sessionStatus) {
        if (!isGuestMode && sessionStatus is SessionStatus.Authenticated) {
            viewModel.startRealtime()
            viewModel.syncMissingMetadata()
            while (true) {
                try {
                    viewModel.syncData()
                } catch (_: Exception) {
                    // Silent failure for auto-sync
                }
                delay(10 * 60 * 1000L) // 10 minutes
            }
        }
    }

    if (sessionStatus is SessionStatus.Initializing || sessionStatus.javaClass.simpleName == "LoadingFromStorage") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Loopa.Amber)
        }
        return
    }

    if (!isGuestMode && sessionStatus !is SessionStatus.Authenticated) {
        AuthScreen(
            viewModel = authViewModel,
            onAuthSuccess = { },
            onGuestClick = { isGuestMode = true }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    AnimatedVisibility(visible = isRateLimited) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth().padding(WindowInsets.statusBars.asPaddingValues())
                        ) {
                            Text(
                                "Service is currently rate-limited. Waiting to resume...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                    NavHost(
                        navController = navController,
                        startDestination = "discover",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("discover") {
                            DiscoverScreen(navController = navController, viewModel = viewModel, hazeState = hazeState)
                        }
                        composable("my_lists") {
                            MyListsScreen(navController = navController, isGuestMode = isGuestMode)
                        }
                        composable("ai_recs") {
                            AiRecommendationsScreen(viewModel = viewModel)
                        }
                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                viewModel = viewModel,
                                isGuestMode = isGuestMode,
                                onLogout = { isGuestMode = false }
                            )
                        }
                        composable("help_feedback") {
                            HelpFeedbackScreen(navController = navController)
                        }
                        composable("privacy_policy") {
                            PrivacyPolicyScreen(navController = navController)
                        }
                        composable("about") {
                            AboutScreen(navController = navController)
                        }
                    }
                }
            }

            // Loopa Floating Frosted Glass Capsule Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0x99101016),
                    border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(32.dp))
                        .hazeEffect(state = hazeState) {
                            blurRadius = 24.dp
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple("discover", "Radar", Icons.Filled.Search),
                            Triple("my_lists", "My List", Icons.Filled.PlayArrow),
                            Triple("ai_recs", "For You", Icons.Filled.AutoFixHigh)
                        )

                        tabs.forEach { (tabId, label, icon) ->
                            val isSelected = currentRoute == tabId
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.94f else 1f,
                                label = "nav_scale_$tabId",
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        color = if (isSelected) Loopa.Amber else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected) Modifier.shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Loopa.Amber)
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            if (currentRoute == "settings") {
                                                navController.popBackStack()
                                                if (tabId != "my_lists") {
                                                    navController.navigate(tabId) {
                                                        popUpTo("discover") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            } else if (currentRoute != tabId) {
                                                navController.navigate(tabId) {
                                                    popUpTo("discover") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Loopa.Base else Loopa.TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Loopa.Base else Loopa.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loop Toast overlay — single host for the whole app
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                LoopToastHost(toastFlow = viewModel.toastEvent)
            }
        }
    }
}
