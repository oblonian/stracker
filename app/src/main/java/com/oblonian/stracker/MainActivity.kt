package com.oblonian.stracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.MainViewModel
import com.oblonian.stracker.ui.calendar.CalendarScreen
import com.oblonian.stracker.ui.day.AppUsageScreen
import com.oblonian.stracker.ui.day.DayMapScreen
import com.oblonian.stracker.ui.day.DayScreen
import com.oblonian.stracker.ui.onboarding.OnboardingScreen
import com.oblonian.stracker.ui.settings.ButtonsScreen
import com.oblonian.stracker.ui.settings.PlacesScreen
import com.oblonian.stracker.ui.settings.SettingsScreen
import com.oblonian.stracker.ui.settings.WorkingHoursScreen
import com.oblonian.stracker.ui.theme.StrackerTheme
import java.time.LocalDate

object Routes {
    const val CALENDAR = "calendar"
    const val DAY = "day/{date}"
    const val DAY_APPS = "day/{date}/apps"
    const val DAY_MAP = "day/{date}/map"
    const val SETTINGS = "settings"
    const val SETTINGS_HOURS = "settings/hours"
    const val SETTINGS_PLACES = "settings/places"
    const val SETTINGS_BUTTONS = "settings/buttons"

    fun day(date: LocalDate) = "day/$date"
    fun dayApps(date: LocalDate) = "day/$date/apps"
    fun dayMap(date: LocalDate) = "day/$date/map"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrackerTheme {
                StrackerAppUi()
            }
        }
    }
}

@Composable
private fun StrackerAppUi() {
    val mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()

    when {
        settings == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        settings?.onboardingDone == false -> OnboardingScreen(onDone = { mainViewModel.completeOnboarding() })
        else -> StrackerNavHost()
    }
}

@Composable
private fun StrackerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.CALENDAR) {
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onDay = { navController.navigate(Routes.day(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        val dateArg = listOf(navArgument("date") { type = NavType.StringType })
        composable(Routes.DAY, arguments = dateArg) { entry ->
            DayScreen(
                onNavigateDay = { d -> navController.navigate(Routes.day(d)) { popUpTo(Routes.CALENDAR); launchSingleTop = true } },
                onApps = { navController.navigate(Routes.dayApps(LocalDate.parse(entry.arguments!!.getString("date")))) },
                onMap = { navController.navigate(Routes.dayMap(LocalDate.parse(entry.arguments!!.getString("date")))) },
            )
        }
        composable(Routes.DAY_APPS, arguments = dateArg) { AppUsageScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DAY_MAP, arguments = dateArg) { DayMapScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onWorkingHours = { navController.navigate(Routes.SETTINGS_HOURS) },
                onPlaces = { navController.navigate(Routes.SETTINGS_PLACES) },
                onButtons = { navController.navigate(Routes.SETTINGS_BUTTONS) },
                onBackup = {}, onRestore = {},
            )
        }
        composable(Routes.SETTINGS_HOURS) { WorkingHoursScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS_PLACES) { PlacesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS_BUTTONS) { ButtonsScreen(onBack = { navController.popBackStack() }) }
    }
}
