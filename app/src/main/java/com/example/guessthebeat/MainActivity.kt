package com.example.guessthebeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guessthebeat.ui.theme.GuessTheBeatTheme

class MainActivity : ComponentActivity() {
    private val musicPlayer = MusicPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuessTheBeatTheme {
                val isDark = isSystemInDarkTheme()
                val gradientEndColor = if (isDark) Color(0xFF121212) else Color(0xFFE0E0E0)

                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        gradientEndColor
                    )
                )

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundGradient),
                    color = Color.Transparent
                ) {
                    MainContainer(musicPlayer = musicPlayer)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        musicPlayer.stop()
    }
}

@Composable
fun MainContainer(
    musicPlayer: MusicPlayer,
    viewModel: GameViewModel = viewModel()
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(viewModel = viewModel, musicPlayer = musicPlayer)
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 💡 ZMIANA: Ignorujemy dolny padding z Scaffolda, żeby lista wjeżdżała POD pasek!
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "GuessTheBeat",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            when {
                viewModel.currentScreen == "login" -> {
                    LoginScreen()
                }
                viewModel.selectedPlaylist == null -> {
                    MainMenuScreen(viewModel = viewModel, musicPlayer = musicPlayer)
                }
                else -> {
                    GameScreen(musicPlayer = musicPlayer, viewModel = viewModel)
                }
            }

            // 💡 Pusty odstęp na końcu, żeby ostatni element na liście dało się przewinąć ponad pasek
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}