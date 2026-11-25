package com.immodi.webdock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immodi.webdock.AppContainer
import com.immodi.webdock.ui.edit.EditRoute
import com.immodi.webdock.ui.home.HomeRoute
import com.immodi.webdock.webview.WebAppActivity

@Composable
fun WebDockApp(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeRoute(
                container = container,
                onAdd = { navController.navigate("edit") },
                onEdit = { appId -> navController.navigate("edit?appId=$appId") },
                onOpen = { app -> context.startActivity(WebAppActivity.createIntent(context, app.id)) },
            )
        }

        composable(
            route = "edit?appId={appId}",
            arguments = listOf(
                navArgument("appId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            EditRoute(
                container = container,
                appId = entry.arguments?.getString("appId"),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
