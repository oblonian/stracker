package com.oblonian.stracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.MainViewModel
import com.oblonian.stracker.ui.onboarding.OnboardingScreen
import com.oblonian.stracker.ui.theme.StrackerTheme

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
        settings == null -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        settings?.onboardingDone == false -> OnboardingScreen(onDone = { mainViewModel.completeOnboarding() })
        else -> Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("stracker", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
