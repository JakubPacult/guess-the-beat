package com.example.guessthebeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guessthebeat.ui.theme.GuessTheBeatTheme

class MainActivity : ComponentActivity() {
    private val musicPlayer = MusicPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuessTheBeatTheme {
                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF1F1B24)
                    )
                )

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundGradient),
                    color = Color.Transparent
                ) {
                    GameScreen(musicPlayer = musicPlayer)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Zatrzymanie muzyki i zwolnienie zasobów przez metodę z klasy MusicPlayer
        musicPlayer.stop()
    }
}

@Composable
fun GameScreen(
    musicPlayer: MusicPlayer,
    viewModel: GameViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tytuł gry
        Text(
            text = "GuessTheBeat",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // KROK 1: Wybór playlisty, jeśli użytkownik jeszcze jej nie wybrał
        if (viewModel.selectedPlaylist == null) {
            Text(
                text = "Wybierz playlistę:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generowanie kart dla każdej dostępnej playlisty
            viewModel.availablePlaylists.forEach { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.selectPlaylist(playlist) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = playlist.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = playlist.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            // KROK 2: Playlista została wybrana, ale gra się jeszcze nie rozpoczęła (utwór nie wczytany)
            if (!viewModel.isSongLoaded) {
                Text(
                    text = "Wybrano: ${viewModel.selectedPlaylist?.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.loadPlaylistAndPlay(musicPlayer) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Rozpocznij Grę", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { viewModel.resetPlaylistSelection() }) {
                    Text(text = "Zmień playlistę", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // KROK 3: Właściwa rozgrywka
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Punkty: ${viewModel.score}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                text = "Próby: ${viewModel.attemptsLeft}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (viewModel.attemptsLeft <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Wykonawca: ${viewModel.correctArtist}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!viewModel.isRoundOver) {
                    Button(
                        onClick = { viewModel.togglePlayPause(musicPlayer) },
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        val icon = when {
                            viewModel.isFinished -> Icons.Default.Refresh
                            viewModel.isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Sterowanie",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = viewModel.userGuess,
                        onValueChange = { viewModel.updateGuess(it) },
                        label = { Text("Wpisz tytuł piosenki") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.checkAnswer(musicPlayer) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "Sprawdź odpowiedź", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.playRandomSong(musicPlayer) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "Następna piosenka", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (viewModel.message.isNotEmpty()) {
                    val messageColor = when {
                        viewModel.message.contains("Brawo") -> Color(0xFF00E676)
                        viewModel.message.contains("Błędnie") || viewModel.message.contains("Koniec prób") -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = viewModel.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (viewModel.incorrectGuesses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Błędne strzały:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewModel.incorrectGuesses.takeLast(3).forEach { guess ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = guess, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Dodatkowy przycisk pozwalający zmienić playlistę w trakcie powrotu do menu
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    musicPlayer.stop()
                    viewModel.resetPlaylistSelection()
                }) {
                    Text(text = "← Wybierz inną playlistę", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}