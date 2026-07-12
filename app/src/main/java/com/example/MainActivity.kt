package com.example.app

import android.os.Bundle
import androidx.compose.runtime.remember
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.AiRecommendationsScreen
import com.example.ui.HomeScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.viewmodel.MediaViewModel
import com.example.viewmodel.MediaUiState
import androidx.compose.ui.layout.ContentScale
import com.example.model.TmdbMovie
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.ui.shimmerEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MediaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
      val themeMode by viewModel.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
          "Light" -> false
          "Dark" -> true
          else -> androidx.compose.foundation.isSystemInDarkTheme()
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
    authViewModel: com.example.viewmodel.AuthViewModel = viewModel()
) {
  val navController = rememberNavController()
  val hazeState = rememberHazeState()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: "my_lists"
  val isRateLimited by viewModel.isRateLimited.collectAsState()
  val sessionStatus by authViewModel.sessionStatus.collectAsState()
  var isGuestMode by remember { mutableStateOf(false) }

  LaunchedEffect(isGuestMode, sessionStatus) {
      if (!isGuestMode && sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
          while (true) {
              try {
                  viewModel.syncData()
              } catch (e: Exception) {
                  // Silent failure for auto-sync
              }
              kotlinx.coroutines.delay(10 * 60 * 1000L) // 10 minutes
          }
      }
  }

  if (sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Initializing || 
      sessionStatus.javaClass.simpleName == "LoadingFromStorage") {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
      }
      return
  }

  if (!isGuestMode && sessionStatus !is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
      com.example.ui.AuthScreen(
          viewModel = authViewModel,
          onAuthSuccess = { },
          onGuestClick = { isGuestMode = true }
      )
      return
  }


      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
          Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                androidx.compose.animation.AnimatedVisibility(visible = isRateLimited) {
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
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
              NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
              ) {
                composable("home") {
                  HomeScreen(navController = navController, viewModel = viewModel)
                }
                composable("my_lists") {
                  MyListsScreen(navController = navController, isGuestMode = isGuestMode)
                }
                composable("ai_recs") {
                  AiRecommendationsScreen(viewModel = viewModel)
                }
                composable("discover") {
                  DiscoverScreen(navController = navController)
                }
                composable("settings") {
                  SettingsScreen(
                    navController = navController, 
                    viewModel = viewModel,
                    isGuestMode = isGuestMode,
                    onLogout = { isGuestMode = false }
                  )
                }
              }
            }
          }

          // Loopa bottom navigation bar — warm, blurred, pill-style tabs
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(150.dp)
                  .align(Alignment.BottomCenter)
                  .hazeEffect(state = hazeState) {
                      blurRadius = 24.dp
                      progressive = HazeProgressive.verticalGradient(
                          startIntensity = 0f,
                          endIntensity = 1f
                      )
                  }
                  .background(
                      brush = Brush.verticalGradient(
                          colorStops = arrayOf(
                              0.0f to Color.Transparent,
                              0.25f to Color(0x0D0F0E0C),
                              0.45f to Color(0x730F0E0C),
                              0.62f to Color(0xCC0F0E0C),
                              1.0f to Color(0xF50F0E0C)
                          )
                      )
                  )
          ) {
              Column(
                  modifier = Modifier
                      .fillMaxWidth()
                      .align(Alignment.BottomCenter)
              ) {
                  Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .windowInsetsPadding(WindowInsets.navigationBars)
                  ) {
                      Row(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(vertical = 12.dp, horizontal = 8.dp),
                          horizontalArrangement = Arrangement.SpaceEvenly,
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          val tabs = listOf(
                              "home"     to "Home",
                              "discover" to "Discover",
                              "my_lists" to "My List",
                              "ai_recs"  to "For You"
                          )

                          tabs.forEach { (tabId, label) ->
                              val isSelected = currentRoute == tabId
                              val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                              val isPressed by interactionSource.collectIsPressedAsState()
                              val scale by animateFloatAsState(
                                  targetValue = if (isPressed) 0.93f else 1f,
                                  label = "nav_scale_$tabId",
                                  animationSpec = spring(
                                      dampingRatio = Spring.DampingRatioMediumBouncy,
                                      stiffness = Spring.StiffnessMedium
                                  )
                              )

                              Box(
                                  modifier = Modifier
                                      .scale(scale)
                                      .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                                      .background(
                                          color = if (isSelected) com.example.ui.Loopa.Amber else Color.Transparent
                                      )
                                      .then(
                                          if (!isSelected) Modifier.border(
                                              width = 1.dp,
                                              color = com.example.ui.Loopa.BorderMd,
                                              shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                          ) else Modifier
                                      )
                                      .clickable(
                                          interactionSource = interactionSource,
                                          indication = null,
                                          onClick = {
                                              navController.navigate(tabId) {
                                                  popUpTo("home") { saveState = true }
                                                  launchSingleTop = true
                                                  restoreState = true
                                              }
                                          }
                                      )
                                      .padding(horizontal = 20.dp, vertical = 10.dp),
                                  contentAlignment = Alignment.Center
                              ) {
                                  Text(
                                      text = label,
                                      color = if (isSelected) com.example.ui.Loopa.Base else com.example.ui.Loopa.TextMuted,
                                      fontSize = 13.sp,
                                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                  )
                              }
                          }
                      }
                  }
              }
          }
          // ── Loop Toast overlay — single host for the whole app ────────────────
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .align(Alignment.TopCenter)
          ) {
              com.example.ui.LoopToastHost(toastFlow = viewModel.toastEvent)
          }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListsScreen(
    navController: androidx.navigation.NavController, 
    viewModel: MediaViewModel = viewModel(),
    isGuestMode: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    var listQuery by remember { mutableStateOf("") }
    val tabs = listOf("All", "Movies", "TV Shows", "Anime")
    val savedItems by viewModel.savedMediaItems.collectAsState()
    
    val filteredItems = remember(savedItems, selectedTab, listQuery) {
        viewModel.getFilteredLocalItems(savedItems, selectedTab, listQuery)
    }
    
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(com.example.ui.Loopa.Base)
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Page header matching Loopa design
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.LoopSectionHeader(
                title = "My List",
                subtitle = "${savedItems.size} Titles",
                showDivider = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sync button
                com.example.ui.LoopButton(
                    text = if (isSyncing) "Syncing…" else "Sync",
                    onClick = {
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
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Sync",
                            tint = com.example.ui.Loopa.Base,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Settings button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(com.example.ui.Loopa.Surface)
                        .border(1.dp, com.example.ui.Loopa.Border, CircleShape)
                        .clickable { navController.navigate("settings") }
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = com.example.ui.Loopa.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = com.example.ui.Loopa.Border, modifier = Modifier.padding(horizontal = 16.dp))
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
                        .clip(com.example.ui.Loopa.PillShape)
                        .clickable { selectedTab = index }
                        .background(if (isSelected) com.example.ui.Loopa.Amber else com.example.ui.Loopa.Surface)
                        .border(1.dp, if (isSelected) Color.Transparent else com.example.ui.Loopa.Border, com.example.ui.Loopa.PillShape)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) com.example.ui.Loopa.Base else com.example.ui.Loopa.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Search Bar
        com.example.ui.LoopTextField(
            value = listQuery,
            onValueChange = { listQuery = it },
            label = "Search My List…",
            leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = com.example.ui.Loopa.Amber) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // Saved List Content
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.LoopEmptyState(
                    message = if (listQuery.isBlank()) "Your list is empty." else "No matches found."
                )
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(viewModel: MediaViewModel = viewModel()) {
    val recommendationState by viewModel.recommendationState.collectAsState()
    val savedItems by viewModel.savedMediaItems.collectAsState()

    LaunchedEffect(savedItems) {
        if (savedItems.size >= 10 && recommendationState !is MediaUiState.Success) {
            viewModel.fetchRecommendations(savedItems)
        } else if (savedItems.size < 10) {
            viewModel.fetchRecommendations(savedItems) // This will set InsufficientData state
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Recommended for You", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )
        
        when (val state = recommendationState) {
            is MediaUiState.Loading -> {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(6) {
                        RecommendationCardSkeleton()
                    }
                }
            }
            is MediaUiState.InsufficientData -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = "Spark", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Add at least 10 items to your lists to unlock smart recommendations.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Currently: ${savedItems.size}/10", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is MediaUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
            is MediaUiState.Success -> {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.trending.size) { index ->
                        val movie = state.trending[index]
                        RecommendationCard(movie = movie)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecommendationCardSkeleton() {
    com.example.ui.LoopCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecommendationCard(
    movie: TmdbMovie,
    viewModel: MediaViewModel = viewModel(),
    onLongPress: ((TmdbMovie) -> Unit)? = null,
    onRelease: (() -> Unit)? = null
) {
    val title = movie.title ?: movie.name ?: "Unknown Title"
    val date = movie.releaseDate ?: movie.firstAirDate ?: "Unknown Date"
    val imageUrl = movie.backdropPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500$it" }
        ?: movie.posterPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it" }

    var showDetails by remember { mutableStateOf(false) }

    val mediaTypeStr = when (movie.mediaType) {
        "tv" -> "SERIES"
        "movie" -> "MOVIE"
        else -> "MEDIA"
    }

    com.example.ui.LoopPosterCard(
        title = title,
        imageUrl = imageUrl,
        mediaType = movie.mediaType ?: "movie",
        onClick = { showDetails = true },
        onLongPress = { if (onLongPress != null) onLongPress(movie) },
        onRelease = onRelease,
        score = movie.voteAverage,
        modifier = Modifier.fillMaxWidth()
    )

    if (showDetails) {
        LoopTrackDialog(
            title = title,
            mediaTypeStr = mediaTypeStr,
            overview = movie.overview,
            onDismiss = { showDetails = false },
            onWatched = {
                viewModel.addMediaItem(movie.id ?: 0, title, imageUrl, date, movie.voteAverage, "Watched", movie.mediaType ?: "movie")
                showDetails = false
            },
            onToWatch = {
                viewModel.addMediaItem(movie.id ?: 0, title, imageUrl, date, movie.voteAverage, "To Watch", movie.mediaType ?: "movie")
                showDetails = false
            }
        )
    }
}

// ─── Shared Loop Track Dialog (used in Discover) ────────────────────────────
@Composable
fun LoopTrackDialog(
    title: String,
    mediaTypeStr: String,
    overview: String?,
    onDismiss: () -> Unit,
    onWatched: () -> Unit,
    onToWatch: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            com.example.ui.LoopDialogContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = com.example.ui.Loopa.TextPrimary,
                        lineHeight = 28.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Media type badge
                    com.example.ui.LoopBadge(
                        text = mediaTypeStr,
                        textColor = com.example.ui.Loopa.Amber,
                        borderColor = com.example.ui.Loopa.Amber.copy(0.4f)
                    )

                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.Loopa.TextSecondary,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = com.example.ui.Loopa.Border)

                    Text(
                        text = "ADD TO LIST",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = com.example.ui.Loopa.TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.LoopButton(
                            text = "Watched",
                            onClick = onWatched,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.example.ui.LoopButton(
                            text = "To Watch",
                            onClick = onToWatch,
                            isSecondary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ─── Shared Loop Quick Action Dialog (long-press actions) ───────────────────
@Composable
fun LoopQuickActionDialog(
    title: String,
    onDismiss: () -> Unit,
    onWatched: () -> Unit,
    onToWatch: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            com.example.ui.LoopDialogContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = com.example.ui.Loopa.TextPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "MOVE TO LIST",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = com.example.ui.Loopa.TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    HorizontalDivider(color = com.example.ui.Loopa.Border)
                    
                    com.example.ui.LoopButton(
                        text = "Watched",
                        onClick = onWatched,
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.LoopButton(
                        text = "To Watch",
                        onClick = onToWatch,
                        isSecondary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (onRemove != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(com.example.ui.Loopa.PillShape)
                                .background(com.example.ui.Loopa.Error.copy(alpha = 0.1f))
                                .border(1.dp, com.example.ui.Loopa.Error.copy(alpha = 0.4f), com.example.ui.Loopa.PillShape)
                                .clickable(onClick = onRemove)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = com.example.ui.Loopa.Error)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaItemCard(item: com.example.db.MediaItemEntity, viewModel: MediaViewModel) {
    var showDetails by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }

    // Determine status color based on watch status
    val statusColor = when (item.listName) {
        "Watching", "Active" -> Color(0xFF00E5FF)   // Cyan = currently watching
        "Watched" -> Color(0xFF33CC33)               // Green = completed
        else -> Color(0xFFFF4500)                     // Orange = to watch
    }
    val statusLabel = when (item.listName) {
        "To Watch", "Want" -> "TO WATCH"
        "Watching", "Active" -> "WATCHING"
        "Watched" -> "WATCHED"
        else -> item.listName.uppercase()
    }

    val progressText = if (item.listName == "Watching" && (item.mediaType == "tv" || item.mediaType == "anime")) {
        "S${item.currentSeason} E${item.currentEpisode}"
    } else {
        item.progressString
    }

    com.example.ui.LoopPosterCard(
        title = item.title,
        imageUrl = item.imageUrl,
        mediaType = item.mediaType,
        onClick = { showDetails = true },
        onLongPress = { showQuickAdd = true },
        score = item.score,
        statusLabel = statusLabel,
        progressText = progressText,
        modifier = Modifier.fillMaxWidth()
    )

    if (showDetails) {
        com.example.ui.EditMediaDialog(
            item = item,
            onDismiss = { showDetails = false },
            onSave = { updatedItem ->
                viewModel.updateMediaItem(updatedItem)
                showDetails = false
            },
            onDelete = {
                viewModel.removeMediaItem(item.id, item.mediaType)
                showDetails = false
            }
        )
    }

    if (showQuickAdd) {
        LoopQuickActionDialog(
            title = item.title,
            onDismiss = { showQuickAdd = false },
            onWatched = {
                viewModel.updateMediaItem(item.copy(listName = "Watched"))
                showQuickAdd = false
            },
            onToWatch = {
                viewModel.updateMediaItem(item.copy(listName = "To Watch"))
                showQuickAdd = false
            },
            onRemove = {
                viewModel.removeMediaItem(item.id, item.mediaType)
                showQuickAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineRangeSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    currentRange: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    RangeSlider(
        value = currentRange,
        onValueChange = {
            if (it.start.toInt() != currentRange.start.toInt() || it.endInclusive.toInt() != currentRange.endInclusive.toInt()) {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
            onValueChange(it)
        },
        valueRange = valueRange,
        startThumb = {
            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        },
        endThumb = {
            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        },
        track = { sliderState ->
            val fractionStart = (sliderState.activeRangeStart - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val fractionEnd = (sliderState.activeRangeEnd - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val activeTrackColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                // Draw timeline ticks
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val tickCount = 20
                    val width = size.width
                    val height = size.height
                    
                    // Draw base line
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f),
                        start = androidx.compose.ui.geometry.Offset(0f, height / 2),
                        end = androidx.compose.ui.geometry.Offset(width, height / 2),
                        strokeWidth = 4.dp.toPx()
                    )
                    
                    // Draw ticks
                    for (i in 0..tickCount) {
                        val x = (i.toFloat() / tickCount) * width
                        val isMajor = i % 5 == 0
                        val tickHeight = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(x, height / 2 - tickHeight / 2),
                            end = androidx.compose.ui.geometry.Offset(x, height / 2 + tickHeight / 2),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    
                    // Draw active track line
                    drawLine(
                        color = activeTrackColor,
                        start = androidx.compose.ui.geometry.Offset(fractionStart * width, height / 2),
                        end = androidx.compose.ui.geometry.Offset(fractionEnd * width, height / 2),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(navController: androidx.navigation.NavController, viewModel: MediaViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var hoverMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    val searchState by viewModel.searchState.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedMediaType by remember { mutableStateOf("All") }
    var selectedSortBy by remember { mutableStateOf("Relevance") }
    
    var releaseYearRange by remember { mutableStateOf(1980f..2026f) }
    var selectedStudio by remember { mutableStateOf("All") }
    var selectedGenres by remember { mutableStateOf(setOf<Int>()) }
    var studioDropdownExpanded by remember { mutableStateOf(false) }
    
    val genreMap = remember { mapOf(
        28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
        80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
        14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
        9648 to "Mystery", 10749 to "Romance", 878 to "Sci-Fi",
        10770 to "TV Movie", 53 to "Thriller", 10752 to "War", 37 to "Western"
    ) }
    val studios = listOf("All", "Netflix", "HBO", "Disney+", "Crunchyroll", "Bones", "MAPPA")

    var genresExpanded by remember { mutableStateOf(false) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (query.isBlank()) {
            viewModel.search("")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
        ) {
        Spacer(modifier = Modifier.height(12.dp))
        com.example.ui.LoopSectionHeader(
            title = "Global Radar",
            subtitle = "Discover Movies, TV & Anime"
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Loopa-styled search bar
        com.example.ui.LoopTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            label = "Search Directive...",
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = com.example.ui.Loopa.Amber)
            },
            trailingIcon = {
                IconButton(onClick = { showFilterSheet = !showFilterSheet }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.MoreVert,
                        contentDescription = "Filter",
                        tint = if (showFilterSheet) com.example.ui.Loopa.Amber else com.example.ui.Loopa.TextSecondary
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        AnimatedVisibility(visible = showFilterSheet) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(com.example.ui.Loopa.CardShape)
                    .background(com.example.ui.Loopa.Surface)
                    .padding(16.dp)
            ) {
                Text("Media Type", fontWeight = FontWeight.SemiBold, color = com.example.ui.Loopa.TextPrimary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Movies", "TV Shows", "Anime").forEach { type ->
                        val isSelected = selectedMediaType == type
                        Box(
                            modifier = Modifier
                                .clip(com.example.ui.Loopa.PillShape)
                                .clickable { selectedMediaType = type }
                                .background(if (isSelected) com.example.ui.Loopa.Amber else com.example.ui.Loopa.Base)
                                .border(1.dp, if (isSelected) Color.Transparent else com.example.ui.Loopa.Border, com.example.ui.Loopa.PillShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = type,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) com.example.ui.Loopa.Base else com.example.ui.Loopa.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Release Year: ${releaseYearRange.start.toInt()} - ${if (releaseYearRange.endInclusive >= 2026f) "Present" else releaseYearRange.endInclusive.toInt()}", fontWeight = FontWeight.SemiBold, color = com.example.ui.Loopa.TextPrimary)
                TimelineRangeSlider(
                    valueRange = 1980f..2026f,
                    currentRange = releaseYearRange,
                    onValueChange = { releaseYearRange = it },
                    haptics = haptics
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Genres", fontWeight = FontWeight.SemiBold, color = com.example.ui.Loopa.TextPrimary)
                    Text(if (selectedGenres.isEmpty()) "Any" else "${selectedGenres.size} selected", style = MaterialTheme.typography.labelMedium, color = com.example.ui.Loopa.Amber)
                }
                
                ResponsiveGrid(
                    items = genreMap.toList(),
                    modifier = Modifier.heightIn(max = 120.dp).padding(vertical = 8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { (id, name) ->
                    val isSelected = selectedGenres.contains(id)
                    Box(
                        modifier = Modifier
                            .clip(com.example.ui.Loopa.PillShape)
                            .clickable {
                                selectedGenres = if (isSelected) selectedGenres - id else selectedGenres + id
                            }
                            .background(if (isSelected) com.example.ui.Loopa.Amber else com.example.ui.Loopa.Base)
                            .border(1.dp, if (isSelected) Color.Transparent else com.example.ui.Loopa.Border, com.example.ui.Loopa.PillShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = name,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) com.example.ui.Loopa.Base else com.example.ui.Loopa.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                com.example.ui.LoopButton(
                    text = "Apply Filters",
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (query.isNotBlank()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("Relevance", "Rating (High to Low)", "Newest First").forEach { sort ->
                    FilterChip(
                        selected = selectedSortBy == sort,
                        onClick = { selectedSortBy = sort },
                        label = { Text(sort) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
            }
        }



        when (val state = searchState) {
            is MediaUiState.Loading -> {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(6) {
                        RecommendationCardSkeleton()
                    }
                }
            }
            is MediaUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
            is MediaUiState.InsufficientData -> {
                // Not used here
            }
            is MediaUiState.Success -> {
                val filteredList = state.trending.filter { movie ->
                    val typeMatch = when (selectedMediaType) {
                        "Movies" -> movie.mediaType == "movie"
                        "TV Shows" -> movie.mediaType == "tv"
                        "Anime" -> movie.genreIds?.contains(16) == true
                        else -> true
                    }
                    val date = movie.releaseDate ?: movie.firstAirDate ?: ""
                    val year = if (date.length >= 4) date.substring(0, 4).toIntOrNull() ?: 0 else 0
                    val yearMatch = year == 0 || year in releaseYearRange.start.toInt()..releaseYearRange.endInclusive.toInt()
                    val studioMatch = if (selectedStudio == "All") true else movie.overview?.contains(selectedStudio, ignoreCase = true) == true
                    val genreMatch = if (selectedGenres.isEmpty()) true else movie.genreIds?.any { it in selectedGenres } == true
                    
                    typeMatch && yearMatch && studioMatch && genreMatch
                }.let { list ->
                    when (selectedSortBy) {
                        "Rating (High to Low)" -> list.sortedByDescending { it.voteAverage ?: 0.0 }
                        "Newest First" -> list.sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" }
                        else -> list
                    }
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList.size) { index ->
                        val movie = filteredList[index]
                        RecommendationCard(
                            movie = movie,
                            onLongPress = { hoverMovie = it },
                            onRelease = { hoverMovie = null }
                        )
                    }
                }
            }
        }
    }

    // ── Hover Preview Overlay ──
    hoverMovie?.let { movie ->
        val title = movie.title ?: movie.name ?: "Unknown"
        val imageUrl = movie.backdropPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500$it" }
            ?: movie.posterPath?.let { "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342$it" }
        val date = movie.releaseDate ?: movie.firstAirDate
        val mediaTypeVal = movie.mediaType ?: "movie"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.ui.Loopa.Base.copy(alpha = 0.85f))
                .clickable { hoverMovie = null },
            contentAlignment = Alignment.Center
        ) {
            // Rounded Loopa Preview Card
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clip(com.example.ui.Loopa.CardShape)
                    .background(com.example.ui.Loopa.Surface)
                    .border(1.dp, com.example.ui.Loopa.Border, com.example.ui.Loopa.CardShape)
                    .padding(20.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = com.example.ui.Loopa.TextPrimary,
                        lineHeight = 26.sp
                    )

                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(com.example.ui.Loopa.CardShape)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.LoopBadge(
                            text = mediaTypeVal,
                            textColor = com.example.ui.Loopa.Amber,
                            borderColor = com.example.ui.Loopa.Amber.copy(0.4f)
                        )

                        val year = if (!date.isNullOrBlank() && date.length >= 4) {
                            date.substring(0, 4)
                        } else null
                        if (year != null) {
                            Text(
                                text = year,
                                color = com.example.ui.Loopa.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (movie.voteAverage != null && movie.voteAverage > 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rating",
                                    tint = com.example.ui.Loopa.Amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = String.format("%.1f", movie.voteAverage),
                                    color = com.example.ui.Loopa.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!movie.overview.isNullOrBlank()) {
                        Text(
                            text = movie.overview,
                            fontSize = 13.sp,
                            color = com.example.ui.Loopa.TextSecondary,
                            lineHeight = 18.sp,
                            maxLines = 5,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController,
    viewModel: MediaViewModel,
    authViewModel: com.example.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    isGuestMode: Boolean = false,
    onLogout: () -> Unit = {}
) {
    var isSyncing by androidx.compose.runtime.remember { mutableStateOf(false) }
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    var currentTimeMs by androidx.compose.runtime.remember { mutableStateOf(System.currentTimeMillis()) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            currentTimeMs = System.currentTimeMillis()
        }
    }
    
    val formattedLastSync = androidx.compose.runtime.remember(lastSyncTime, currentTimeMs) {
        if (lastSyncTime == 0L) "Never synced"
        else {
            val relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                lastSyncTime,
                currentTimeMs,
                android.text.format.DateUtils.MINUTE_IN_MILLIS,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            "Last synced: $relativeTime"
        }
    }
    
    Column(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Loopa header for Settings
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.LoopSectionHeader(
                title = "Settings",
                subtitle = if (isGuestMode) "Guest Mode" else "Preferences",
                showDivider = false
            )
            // Back button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(com.example.ui.Loopa.Surface)
                    .border(1.dp, com.example.ui.Loopa.Border, CircleShape)
                    .clickable { navController.popBackStack() }
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = com.example.ui.Loopa.TextPrimary,
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
                com.example.ui.LoopSectionHeader(
                    title = "Account & Data",
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

                // Section header: Preferences
                com.example.ui.LoopSectionHeader(
                    title = "Preferences",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                var notificationsEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Updates & reminders", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { 
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it }) 
                    },
                    modifier = Modifier.clickable { notificationsEnabled = !notificationsEnabled }
                )

                // Section header: Support & About
                com.example.ui.LoopSectionHeader(
                    title = "Support & About",
                    subtitle = null,
                    showDivider = false,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Help & Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.Help, contentDescription = "Help", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Loopa v1.0", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.Info, contentDescription = "About", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable { }
                )
            }
        }
    }
}

@Composable
fun <T> ResponsiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (T) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth > 600.dp) {
            (maxWidth / 320.dp).toInt().coerceAtLeast(2)
        } else {
            1
        }
        
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = items.size,
                key = { index -> items[index].hashCode() }
            ) { index ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.animateItem()) {
                    itemContent(items[index])
                }
            }
        }
    }
}
