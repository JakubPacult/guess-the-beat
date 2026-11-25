package com.example.guessthebeat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavBar(
    viewModel: GameViewModel,
    musicPlayer: MusicPlayer
) {
    val currentScreen = viewModel.currentScreen
    val baseBg = MaterialTheme.colorScheme.background

    val navGradient = Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.35f to baseBg.copy(alpha = 0.65f),
        0.75f to baseBg.copy(alpha = 0.92f),
        1.0f to baseBg
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(navGradient)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Przycisk Menu głównego
            NavItemBox(
                title = "Menu główne",
                icon = Icons.Default.Home,
                isSelected = currentScreen == "menu" && viewModel.selectedPlaylist == null,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.requestExit("menu")
                }
            )

            // Przycisk Logowania
            NavItemBox(
                title = "Logowanie",
                icon = Icons.Default.Person,
                isSelected = currentScreen == "login",
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.requestExit("login")
                }
            )
        }
    }

    // Okno dialogowe ostrzegające przed przerwaniem gry
    if (viewModel.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExit() },
            title = {
                Text(text = "Przerwać rozgrywkę?")
            },
            text = {
                Text(text = "Czy na pewno chcesz opuścić grę? Trwająca sesja zostanie utracona.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmExit(musicPlayer) }
                ) {
                    Text("Tak, opuść", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissExit() }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun NavItemBox(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(30.dp),
            tint = iconTint
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}