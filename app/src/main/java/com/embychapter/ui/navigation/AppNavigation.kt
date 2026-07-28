package com.embychapter.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.embychapter.ui.chapter.ChapterScreen
import com.embychapter.ui.history.HistoryScreen
import com.embychapter.ui.videowall.VideoWallScreen
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

// Type-safe navigation routes (serializable so NavHost can reconstruct them)
@Serializable data object ChapterRoute
@Serializable data object HistoryRoute
@Serializable data object VideoWallRoute

data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<out Any>,
    val icon: ImageVector,
    val label: String,
    val title: String
)

val topLevelDestinations = listOf(
    TopLevelDestination(ChapterRoute, ChapterRoute::class, Icons.Outlined.PlayCircle, "章节管理", "章节管理大师 Pro"),
    TopLevelDestination(HistoryRoute, HistoryRoute::class, Icons.Outlined.History, "播放历史", "播放历史"),
    TopLevelDestination(VideoWallRoute, VideoWallRoute::class, Icons.Outlined.Movie, "视频墙", "视频墙")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteClass = navBackStackEntry?.destination?.route?.let { it::class }

    // NavigationSuiteScaffold is adaptive: bottom bar on phones, rail on
    // medium screens, drawer on expanded — the same scaffold pattern nowinandroid uses.
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            topLevelDestinations.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = currentRouteClass == destination.routeClass,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                val title = topLevelDestinations.firstOrNull { it.routeClass == currentRouteClass }?.title
                    ?: "Emby 工具箱"
                TopAppBar(
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ChapterRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<ChapterRoute> { ChapterScreen() }
                composable<HistoryRoute> { HistoryScreen() }
                composable<VideoWallRoute> { VideoWallScreen() }
            }
        }
    }
}
