package com.example.workmanager.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.workmanager.ui.edit.ReminderEditScreen
import com.example.workmanager.ui.history.HistoryScreen
import com.example.workmanager.ui.list.ReminderListScreen
import com.example.workmanager.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LIST = "list"
    const val HISTORY = "history"
    const val EDIT = "edit?reminderId={reminderId}"

    fun edit(reminderId: Long? = null): String =
        if (reminderId == null) "edit" else "edit?reminderId=$reminderId"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LIST) {
            ReminderListScreen(
                onAdd = { navController.navigate(Routes.edit()) },
                onEdit = { id -> navController.navigate(Routes.edit(id)) },
                onHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("reminderId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            ReminderEditScreen(onBack = { navController.popBackStack() })
        }
    }
}