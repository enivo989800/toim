package com.toimg.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toimg.app.feature.categories.CategoriesScreen
import com.toimg.app.feature.home.HomeScreen
import com.toimg.app.feature.picker.ImagePickerScreen
import com.toimg.app.feature.result.MoveResultScreen
import com.toimg.app.feature.settings.SettingsScreen
import com.toimg.app.feature.workspace.ImageWorkspaceScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToImgNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.HOME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppRoute.titleFor(currentRoute)) },
                navigationIcon = {
                    if (currentRoute != AppRoute.HOME) {
                        TextButton(onClick = { navController.navigateUp() }) {
                            Text("返回")
                        }
                    }
                },
                actions = {
                    if (currentRoute != AppRoute.SETTINGS) {
                        TextButton(onClick = { navController.navigate(AppRoute.SETTINGS) }) {
                            Text("设置")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoute.HOME) {
                HomeScreen(
                    onSelectImages = { navController.navigate(AppRoute.PICKER) },
                    onOpenWorkspace = { navController.navigate(AppRoute.WORKSPACE) },
                    onOpenCategories = { navController.navigate(AppRoute.CATEGORIES) }
                )
            }
            composable(AppRoute.PICKER) {
                ImagePickerScreen(
                    onContinue = { navController.navigate(AppRoute.WORKSPACE) },
                )
            }
            composable(AppRoute.WORKSPACE) {
                ImageWorkspaceScreen(
                    onPreviewComplete = { navController.navigate(AppRoute.RESULT) },
                    onNavigateToSettings = { navController.navigate(AppRoute.SETTINGS) }
                )
            }
            composable(AppRoute.CATEGORIES) {
                CategoriesScreen(
                    onGoToWorkspaceOrResult = { navController.navigate(AppRoute.WORKSPACE) }
                )
            }
            composable(AppRoute.RESULT) {
                MoveResultScreen(
                    onBackHome = {
                        navController.navigate(AppRoute.HOME) {
                            popUpTo(AppRoute.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppRoute.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
