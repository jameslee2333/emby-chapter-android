package com.embychapter.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.embychapter.ui.chapter.ChapterScreen
import com.embychapter.ui.history.HistoryScreen
import com.embychapter.ui.theme.SurfaceDark
import com.embychapter.ui.videowall.VideoWallScreen

sealed class Screen(val route: String, val label: String) {
    data object Chapter : Screen("chapter", "章节管理")
    data object History : Screen("history", "播放历史")
    data object VideoWall : Screen("videowall", "视频墙")
}

val bottomNavItems = listOf(
    Screen.Chapter to Icons.Outlined.PlayCircle,
    Screen.History to Icons.Outlined.History,
    Screen.VideoWall to Icons.Outlined.Movie
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val title = when (currentRoute) {
                Screen.Chapter.route -> "章节管理大师 Pro"
                Screen.History.route -> "播放历史"
                Screen.VideoWall.route -> "视频墙"
                else -> "Emby 工具箱"
            }
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                bottomNavItems.forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chapter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chapter.route) { ChapterScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.VideoWall.route) { VideoWallScreen() }
        }
    }
}
