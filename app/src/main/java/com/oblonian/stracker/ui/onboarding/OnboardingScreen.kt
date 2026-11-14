package com.oblonian.stracker.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oblonian.stracker.ui.theme.HeroGradient

/** Placeholder until the three-page onboarding lands (plan Task 15). */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    OnboardingContent(onStart = onDone)
}

@Composable
fun OnboardingContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(HeroGradient)
                .padding(start = 28.dp, end = 28.dp, top = 72.dp, bottom = 40.dp),
        ) {
            Column {
                Text("Your days, mapped.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "stracker records sleep, phone use, movement and places on its own, and lets you " +
                        "log the rest with one tap. Everything stays on this phone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HeroGradient)
                    .clickable(role = Role.Button, onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Text("Start", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
