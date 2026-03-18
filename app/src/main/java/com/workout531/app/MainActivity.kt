package com.workout531.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workout531.app.data.MainLift
import com.workout531.app.data.Week
import com.workout531.app.ui.AppViewModel
import com.workout531.app.ui.setup.SetupScreen
import com.workout531.app.ui.workout.CycleOverviewScreen
import com.workout531.app.ui.workout.SettingsScreen
import com.workout531.app.ui.workout.WorkoutScreen
import com.workout531.app.ui.theme.WorkoutAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkoutApp()
                }
            }
        }
    }
}

@Composable
fun WorkoutApp() {
    val viewModel: AppViewModel = viewModel()
    val navController = rememberNavController()

    val startDest = if (viewModel.state.isSetup) "overview" else "setup"

    NavHost(navController = navController, startDestination = startDest) {
        composable("setup") {
            SetupScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("overview") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("overview") {
            CycleOverviewScreen(
                viewModel = viewModel,
                onWorkoutClick = { week, lift ->
                    navController.navigate("workout/${week.name}/${lift.name}")
                },
                onSettingsClick = { navController.navigate("settings") },
                onNextCycle = { /* stays on overview, UI refreshes */ }
            )
        }

        composable(
            "workout/{week}/{lift}",
            arguments = listOf(
                navArgument("week") { type = NavType.StringType },
                navArgument("lift") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val week = Week.valueOf(backStackEntry.arguments?.getString("week") ?: "WEEK1")
            val lift = MainLift.valueOf(backStackEntry.arguments?.getString("lift") ?: "SQUAT")
            WorkoutScreen(
                viewModel = viewModel,
                week = week,
                lift = lift,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    if (viewModel.state.isSetup) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
