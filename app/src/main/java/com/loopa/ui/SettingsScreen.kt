package com.loopa.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.loopa.viewmodel.AuthViewModel
import com.loopa.viewmodel.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Settings Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MediaViewModel,
    authViewModel: AuthViewModel = viewModel(),
    isGuestMode: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    // Import / Export states
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0 to 0) }
    var importCurrentTitle by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }

    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val formattedLastSync = remember(lastSyncTime, currentTimeMs) {
        if (lastSyncTime == 0L) "Never synced"
        else {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                lastSyncTime,
                currentTimeMs,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            "Last synced: $relativeTime"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (content.isNotEmpty()) {
                        isImporting = true
                        importProgress = 0 to 0
                        importCurrentTitle = "Parsing file..."
                        viewModel.importWatchlistData(
                            content = content,
                            fileName = uri.lastPathSegment,
                            onProgress = { cur, tot, title ->
                                importProgress = cur to tot
                                importCurrentTitle = title
                            },
                            onComplete = { summary ->
                                isImporting = false
                                viewModel.showToast("Import complete: ${summary.imported} imported, ${summary.failed} failed")
                            }
                        )
                    } else {
                        viewModel.showToast("File is empty")
                    }
                } catch (e: Exception) {
                    isImporting = false
                    viewModel.showToast("Import error: ${e.message}")
                }
            }
        }
    }

    var pendingExportData by remember { mutableStateOf<String?>(null) }

    val createJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportData != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(pendingExportData!!.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.showToast("Watchlist JSON saved to device!")
                } catch (e: Exception) {
                    viewModel.showToast("Save failed: ${e.message}")
                } finally {
                    pendingExportData = null
                }
            }
        }
    }

    val createCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && pendingExportData != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(pendingExportData!!.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.showToast("Watchlist CSV saved to device!")
                } catch (e: Exception) {
                    viewModel.showToast("Save failed: ${e.message}")
                } finally {
                    pendingExportData = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Loopa.Base)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Loopa header for Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Loopa.TextPrimary
                )
                Text(
                    text = if (isGuestMode) "Guest Mode" else "Preferences",
                    fontSize = 12.sp,
                    color = Loopa.TextSecondary
                )
            }

            // Back button to go back to My List
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Loopa.Surface)
                    .border(1.dp, Loopa.Border, CircleShape)
                    .clickable {
                        if (!navController.popBackStack()) {
                            navController.navigate("my_lists")
                        }
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to My List",
                    tint = Loopa.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                // Section header: Account & Data
                LoopSectionHeader(
                    title = "Account & Cloud",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Sync with Cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Column {
                            Text(if (isSyncing) "Syncing..." else "Sync media items with Supabase", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!isGuestMode && !isSyncing) {
                                Text(formattedLastSync, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    },
                    leadingContent = { Icon(Icons.Filled.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sync") },
                    modifier = Modifier.clickable {
                        if (!isSyncing && !isGuestMode) {
                            isSyncing = true
                            coroutineScope.launch {
                                try {
                                    viewModel.syncData()
                                    viewModel.showToast("Sync complete!")
                                } catch (e: Exception) {
                                    viewModel.showToast("Sync failed: ${e.message}")
                                } finally {
                                    isSyncing = false
                                }
                            }
                        } else if (isGuestMode) {
                            viewModel.showToast("Sign in to sync data")
                        }
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(if (isGuestMode) "Log In" else "Log Out", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(if (isGuestMode) "Sign in to sync your data" else "Sign out from your account", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = if (isGuestMode) "Login" else "Logout", tint = if (isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (isGuestMode) "Login" else "Logout") },
                    modifier = Modifier.clickable {
                        if (!isGuestMode) authViewModel.signOut()
                        onLogout()
                    }
                )

                // Section header: Data Portability Suite
                LoopSectionHeader(
                    title = "Data Portability",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Export Watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Export full library to JSON or CSV", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Export") },
                    modifier = Modifier.clickable { showExportSheet = true }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Import Watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Import from Letterboxd, IMDb, MAL, or JSON", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Import") },
                    modifier = Modifier.clickable {
                        importLauncher.launch("*/*")
                    }
                )

                // Section header: Preferences
                LoopSectionHeader(
                    title = "Preferences",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                var notificationsEnabled by remember { mutableStateOf(true) }
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Updates & reminders", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    },
                    modifier = Modifier.clickable { notificationsEnabled = !notificationsEnabled }
                )

                // Section header: Support & About
                LoopSectionHeader(
                    title = "Support & About",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Help & Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Loopa v2.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = "About", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
            }
        }
    }

    // ── Export Options Dialog ──
    if (showExportSheet) {
        AlertDialog(
            onDismissRequest = { showExportSheet = false },
            title = { Text("Export Watchlist", fontWeight = FontWeight.Bold) },
            text = { Text("Choose your preferred export format:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportSheet = false
                        coroutineScope.launch {
                            try {
                                val json = viewModel.getWatchlistExportJSON()
                                pendingExportData = json
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                createJsonFileLauncher.launch("loopa_watchlist_$timestamp.json")
                            } catch (e: Exception) {
                                viewModel.showToast("Export failed: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Save JSON")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportSheet = false
                        coroutineScope.launch {
                            try {
                                val csv = viewModel.getWatchlistExportCSV()
                                pendingExportData = csv
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                createCsvFileLauncher.launch("loopa_watchlist_$timestamp.csv")
                            } catch (e: Exception) {
                                viewModel.showToast("Export failed: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Save CSV")
                }
            },
            containerColor = Loopa.Surface,
            titleContentColor = Loopa.TextPrimary,
            textContentColor = Loopa.TextSecondary
        )
    }

    // ── Import Progress Dialog ──
    if (isImporting) {
        Dialog(onDismissRequest = { /* prevent dismissal during import */ }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Loopa.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Loopa.Border),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Importing Watchlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Loopa.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (importProgress.second > 0) {
                        val progress = importProgress.first / importProgress.second.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Loopa.Amber,
                            trackColor = Loopa.Raised
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${importProgress.first} of ${importProgress.second} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextSecondary
                        )
                    } else {
                        CircularProgressIndicator(color = Loopa.Amber, modifier = Modifier.size(36.dp))
                    }
                    if (importCurrentTitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importCurrentTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Loopa.TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
