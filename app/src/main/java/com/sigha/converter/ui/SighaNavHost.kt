package com.sigha.converter.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sigha.converter.R

private object Routes {
    const val WORKBENCH = "workbench"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val QUEUE = "queue"
    const val ABOUT = "about"
    const val MODEL_PACKS = "modelPacks"
    const val LICENSES = "licenses"
    const val PRIVACY = "privacy"
}

private val TOP_LEVEL_ROUTES = setOf(Routes.WORKBENCH, Routes.HISTORY, Routes.SETTINGS)

/**
 * Single owner of the window: this is the only composable that knows about the system
 * bars and the keyboard. Screens keep their own Scaffold/TopAppBar (so they own the top
 * inset and their snackbar hosts), while this root supplies the bottom-bar slot height,
 * the horizontal safe area (landscape 3-button nav, cutout), the bottom safe area when
 * the bottom bar is hidden (gesture pill / 3-button nav), and IME avoidance.
 */
@Composable
fun SighaNavHost(workbenchViewModel: WorkbenchViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || currentRoute in TOP_LEVEL_ROUTES

    // A share from another app is added to the workbench ViewModel while the user may be on
    // any other tab or sub-screen; bring the workbench forward so the files are visible
    // instead of silently landing behind the current screen.
    LaunchedEffect(workbenchViewModel) {
        workbenchViewModel.openWorkbench.collect {
            if (navController.currentBackStackEntry?.destination?.route != Routes.WORKBENCH) {
                navController.navigate(Routes.WORKBENCH) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.WORKBENCH,
                        onClick = { navigateTopLevel(navController, Routes.WORKBENCH) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.nav_convert)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navigateTopLevel(navController, Routes.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.nav_history)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navigateTopLevel(navController, Routes.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.nav_settings)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Screens own the top inset via their TopAppBar; the NavHost only carries the
        // bottom-bar slot height, the horizontal safe area, and the bottom safe area when
        // no bar is showing. Insets consumed here are removed for descendants, so screens
        // that still call navigationBarsPadding() become no-ops instead of double-padding.
        val navHostModifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .then(
                if (showBottomBar) {
                    Modifier
                } else {
                    Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                    )
                }
            )
        NavHost(
            navController = navController,
            startDestination = Routes.WORKBENCH,
            modifier = navHostModifier
        ) {
            composable(Routes.WORKBENCH) {
                WorkbenchScreen(
                    viewModel = workbenchViewModel,
                    onOpenQueue = { navController.navigate(Routes.QUEUE) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenWorkbench = { navigateTopLevel(navController, Routes.WORKBENCH) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenLicenses = { navController.navigate(Routes.LICENSES) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenModelPacks = { navController.navigate(Routes.MODEL_PACKS) }
                )
            }
            composable(Routes.QUEUE) {
                QueueScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWorkbench = {
                        // The queue is opened above the workbench, so popping back to that
                        // entry is the shortest way home; fall back to a top-level jump.
                        if (!navController.popBackStack(Routes.WORKBENCH, false)) {
                            navigateTopLevel(navController, Routes.WORKBENCH)
                        }
                    }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    onOpenLicenses = { navController.navigate(Routes.LICENSES) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.MODEL_PACKS) {
                ModelPacksScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LICENSES) {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
