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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Stałe logo i nagłówek na górze aplikacji
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(140.dp)
        )
        Text(
            text = "GuessTheBeat",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Inteligentne przełączanie między menu głównym a grą na podstawie stanu w ViewModel
        if (viewModel.selectedPlaylist == null) {
            MainMenuScreen(viewModel = viewModel, musicPlayer = musicPlayer)
        } else {
            GameScreen(musicPlayer = musicPlayer, viewModel = viewModel)
        }
    }
}