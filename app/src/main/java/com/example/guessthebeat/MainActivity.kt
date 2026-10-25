package com.example.guessthebeat

import android.os.Bundle
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.guessthebeat.ui.theme.GuessTheBeatTheme

class MainActivity : ComponentActivity() {
    private val musicPlayer = MusicPlayer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Tutaj wywołujesz swój główny ekran gry
            GuessTheBeatTheme() {
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
        musicPlayer.stop() // zatrzymuje muzykę
    }
}

// To jest funkcja budująca interfejs (ekran gry)
@Composable
fun GameScreen(musicPlayer: MusicPlayer) {
    val coroutineScope = rememberCoroutineScope()

    var playlistTrackIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var correctTitle by remember { mutableStateOf("") }
    var correctArtist by remember { mutableStateOf("") }
    var userGuess by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSongLoaded by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.stop()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Przycisk startowy / ładowania playlisty
        Button(onClick = {
            coroutineScope.launch {
                message = "Pobieranie playlisty..."
                userGuess = ""

                // Id playlisty z Deezer
                val ids = musicPlayer.loadPlaylist(8424792262)

                if (ids.isNotEmpty()) {
                    playlistTrackIds = ids
                    // Losowanie id
                    val randomId = playlistTrackIds.random()
                    val result = musicPlayer.loadAndPlaySong(randomId)

                    if (result != null) {
                        correctTitle = result.first
                        correctArtist = result.second
                        isSongLoaded = true
                        message = "Playlista wczytana! Zgadnij utwór."
                    } else {
                        message = "Błąd odtwarzania utworu."
                    }
                } else {
                    message = "Nie udało się pobrać playlisty."
                }
            }
        }) {
            Text(text = "Rozpocznij / Wczytaj playlistę")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSongLoaded) {
            Text(text = "Punkty: $score", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Wykonawca: $correctArtist", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userGuess,
                onValueChange = { userGuess = it },
                label = { Text("Wpisz tytuł piosenki") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (userGuess.trim().equals(correctTitle.trim(), ignoreCase = true)) {
                    score += 1
                    message = "Brawo! Poprawna odpowiedź: $correctTitle - $correctArtist. Losuję kolejny utwór..."

                    // Losowanie kolejnego utworu z playlisty
                    coroutineScope.launch {
                        userGuess = ""
                        val randomId = playlistTrackIds.random()
                        val result = musicPlayer.loadAndPlaySong(randomId)
                        if (result != null) {
                            correctTitle = result.first
                            correctArtist = result.second
                        }
                    }
                } else {
                    message = "Błędnie! Spróbuj ponownie."
                }
            }) {
                Text(text = "Sprawdź odpowiedź")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}