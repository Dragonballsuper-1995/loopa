package com.loopa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.loopa.db.MediaItemEntity
import com.loopa.ui.components.MediaItemCard
import com.loopa.viewmodel.MediaViewModel
import com.loopa.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// My Lists Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListsScreen(
    navController: NavController,
    viewModel: MediaViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel(),
    isGuestMode: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    var listQuery by remember { mutableStateOf("") }
    val tabs = listOf("All", "Movies", "TV Shows", "Anime")
    val savedItems by viewModel.savedMediaItems.collectAsState()

    var filteredItems by remember { mutableStateOf<List<MediaItemEntity>>(emptyList()) }
    LaunchedEffect(savedItems, selectedTab, listQuery) {
        filteredItems = viewModel.getFilteredLocalItems(savedItems, selectedTab, listQuery)
    }

    var isSyncing by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0 to 0) }
    var importCurrentTitle by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExportData by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Quick Import Launcher
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
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

    // Quick Export Launchers
    val createJsonFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        if (uri != null && pendingExportData != null) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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

    val createCsvFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: android.net.Uri? ->
        if (uri != null && pendingExportData != null) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        Spacer(modifier = Modifier.height(12.dp))

        // Page header matching Loopa design
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Loopa.TextPrimary
                )
                Text(
                    text = "${savedItems.size} Titles",
                    fontSize = 12.sp,
                    color = Loopa.TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick Export Button (Upload/Export icon)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, CircleShape)
                        .clickable { showExportSheet = true }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = "Export Watchlist",
                        tint = Loopa.Amber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Import Button (Download/Import icon)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, CircleShape)
                        .clickable { importLauncher.launch("*/*") }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Import Watchlist",
                        tint = Loopa.Amber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sync button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, CircleShape)
                        .clickable {
                            if (!isGuestMode) {
                                isSyncing = true
                                coroutineScope.launch {
                                    try {
                                        viewModel.syncData()
                                        viewModel.showToast("Sync complete!")
                                    } catch (e: Exception) {
                                        viewModel.showToast("Sync failed")
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            } else {
                                viewModel.showToast("Sign in to sync data")
                            }
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Sync",
                        tint = if (isSyncing) Loopa.Amber else Loopa.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings button (Gear icon)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Loopa.Surface)
                        .border(1.dp, Loopa.Border, CircleShape)
                        .clickable { navController.navigate("settings") }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Loopa.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Loopa.Border, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(Loopa.PillShape)
                        .clickable { selectedTab = index }
                        .background(if (isSelected) Loopa.Amber else Loopa.Surface)
                        .border(1.dp, if (isSelected) Color.Transparent else Loopa.Border, Loopa.PillShape)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Loopa.Base else Loopa.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Search Bar
        RadarSearchBar(
            query = listQuery,
            onQueryChange = { listQuery = it },
            placeholder = "Search My List...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Saved List Content
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                LoopEmptyState(
                    message = if (listQuery.isBlank()) "Your list is empty." else "No matches found."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems.size) { index ->
                    MediaItemCard(item = filteredItems[index], viewModel = viewModel)
                }
            }
        }
    }

    // ── Quick Export Options Dialog ──
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

    // ── Quick Import Progress Dialog ──
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
