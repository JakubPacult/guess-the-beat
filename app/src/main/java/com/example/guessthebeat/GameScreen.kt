package com.example.guessthebeat

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
@Composable
fun GameScreen(
    musicPlayer: MusicPlayer,
    viewModel: GameViewModel
) {
    val context = LocalContext.current

    // AUTOMATYCZNE URUCHAMIANIE I WYŁĄCZANIE CZUJNIKA OBROTU
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val flipDetector = FlipDetector(sensorManager) {
            if (viewModel.isSongLoaded && !viewModel.isRoundOver) {
                viewModel.skipCurrentAttempt(musicPlayer)
            }
        }

        onDispose {
            // Sprzątamy po wyjściu z ekranu
            flipDetector.unregister(sensorManager)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. EKRAN PODSUMOWANIA GRY
        if (viewModel.isGameSessionFinished) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Koniec Gry!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Twój wynik: ${viewModel.score} / ${viewModel.maxScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetPlaylistSelection(musicPlayer) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zagraj ponownie")
                    }
                }
            }
        }
        // 2. EKRAN PRZED STARTEM ROZGRYWKI (po wybraniu playlisty)
        else if (!viewModel.isSongLoaded) {
            Text(
                text = "Wybrano playlistę:\n${viewModel.selectedPlaylist?.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.loadPlaylistAndPlay(musicPlayer) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Rozpocznij Grę", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { viewModel.resetPlaylistSelection(musicPlayer) }) {
                Text(text = "← Zmień playlistę", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // 3. WŁAŚCIWA ROZGRYWA
        else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Runda: ${viewModel.currentRound}/${viewModel.totalRounds}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Punkty: ${viewModel.score}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pozostało prób: ${viewModel.attemptsLeft}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (viewModel.attemptsLeft <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PASEK POSTĘPU CZASU ODSŁUCHU
            if (!viewModel.isRoundOver) {
                val currentAttemptIndex = 6 - viewModel.attemptsLeft
                val progress = (currentAttemptIndex + 1).toFloat() / 6f

                val currentSecondsText = when(currentAttemptIndex) {
                    0 -> "0.2s"
                    1 -> "1s"
                    2 -> "3s"
                    3 -> "6s"
                    else -> "15s"
                }

                Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Dostępny czas odsłuchu:", style = MaterialTheme.typography.bodySmall)
                        Text(text = currentSecondsText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // OKŁADKA ORAZ INFORMACJE O UTWORY PO ZAKOŃCZENIU RUNDY
            if (viewModel.isRoundOver) {
                if (viewModel.correctCoverUrl.isNotEmpty()) {
                    Card(
                        modifier = Modifier.size(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        AsyncImage(
                            model = viewModel.correctCoverUrl,
                            contentDescription = "Okładka albumu",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.correctTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Wykonawca: ${viewModel.correctArtist}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- 3 STANOWY PRZYCISK PLAY / PAUSE / REFRESH ---
            val playButtonIcon = when {
                viewModel.isPlaying -> Icons.Default.Pause
                viewModel.isRoundOver -> Icons.Default.Refresh
                else -> Icons.Default.PlayArrow
            }

            if (!viewModel.isRoundOver) {
                Button(
                    onClick = {
                        if (viewModel.isPlaying) {
                            musicPlayer.pause()
                            viewModel.isPlaying = false
                        } else if (viewModel.isRoundOver) {
                            musicPlayer.playSnippet(15000L)
                            viewModel.isPlaying = true
                        } else {
                            musicPlayer.playSnippet(viewModel.getCurrentSnippetDuration())
                            viewModel.isPlaying = true
                        }
                    },
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = playButtonIcon,
                        contentDescription = "Odtwórz próbkę",
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PRZEWIJANA LISTA PODPOWIEDZI Z OGRANICZONĄ WYSOKOŚCIĄ (MAX 180.DP)
                if (viewModel.showSuggestions && viewModel.songSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(4.dp)
                        ) {
                            viewModel.songSuggestions.forEach { suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectSuggestion(suggestion, musicPlayer) }
                                        .padding(10.dp)
                                ) {
                                    Text(text = suggestion.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = suggestion.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // --- WYSZUKIWARKA ORAZ PRZYCISK SKIP OBOK NIEJ ---
                // --- WYSZUKIWARKA ORAZ PRZYCISK SKIP ETAPU OBOK NIEJ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.userGuess,
                        onValueChange = { viewModel.updateGuess(it, musicPlayer) },
                        // Używamy placeholder zamiast label, żeby pole miało stabilną, równą wysokość bez wystającej etykiety
                        placeholder = { Text("Wpisz tytuł piosenki") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )

                    // Przycisk pomijania etapu czasowego (Skip)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.skipCurrentAttempt(musicPlayer) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Pomiń i posłuchaj dłuższego fragmentu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.checkAnswer(musicPlayer) },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Sprawdź odpowiedź", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.nextRound(musicPlayer) },
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = if (viewModel.currentRound >= viewModel.totalRounds) "Zakończ grę" else "Następna piosenka", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.message.isNotEmpty()) {
                val messageColor = when {
                    viewModel.message.contains("Brawo") -> Color(0xFF00E676)
                    viewModel.message.contains("Błędnie") || viewModel.message.contains("Koniec prób") || viewModel.message.contains("Brak połączenia") || viewModel.message.contains("Pominięto") -> MaterialTheme.colorScheme.error
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Błędne strzały:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
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

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                viewModel.resetPlaylistSelection(musicPlayer)
                viewModel.clearSearch()
            }) {
                Text(text = "← Wybierz inną playlistę", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}