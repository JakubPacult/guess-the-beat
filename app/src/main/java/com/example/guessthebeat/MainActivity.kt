package com.example.guessthebeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guessthebeat.ui.theme.GuessTheBeatTheme

class MainActivity : ComponentActivity() {
    private val musicPlayer = MusicPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuessTheBeatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(musicPlayer = musicPlayer)
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
fun GameScreen(
    musicPlayer: MusicPlayer,
    viewModel: GameViewModel = viewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "GuessTheBeat", style = MaterialTheme.typography.displayMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.loadPlaylistAndPlay(musicPlayer)
        }) {
            Text(text = "Rozpocznij / Wczytaj playlistę")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isSongLoaded) {
            Text(text = "Punkty: ${viewModel.score}", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Wykonawca: ${viewModel.correctArtist}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // 💡 Jeśli runda trzyma się dalej (nie odgadnięto i nie minęły próby)
            if (!viewModel.isRoundOver) {
                Button(
                    onClick = {
                        viewModel.togglePlayPause(musicPlayer)
                    },
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape
                ) {
                    val icon = when {
                        viewModel.isFinished -> Icons.Default.Refresh
                        viewModel.isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = "Sterowanie odtwarzaniem",
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.userGuess,
                    onValueChange = { viewModel.updateGuess(it) },
                    label = { Text("Wpisz tytuł piosenki") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    viewModel.checkAnswer(musicPlayer)
                }) {
                    Text(text = "Sprawdź odpowiedź")
                }

            } else {
                // 💡 JEŚLI RUNDA SIĘ SKOŃCZYŁA: Pokazujemy przycisk "Następna piosenka"
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.playRandomSong(musicPlayer)
                    },
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
                ) {
                    Text(text = "Następna piosenka", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wyświetlanie błędnych prób
            if (viewModel.incorrectGuesses.isNotEmpty()) {
                Text(text = "Twoje błędne próby:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    viewModel.incorrectGuesses.forEach { guess ->
                        Text(text = "❌ $guess", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = viewModel.message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}