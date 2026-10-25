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

    var correctTitle by remember { mutableStateOf("") }
    var userGuess by remember {mutableStateOf("") }
    var message by remember {mutableStateOf("") }
    var isSongLoaded by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.stop()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                message = ""
                userGuess = ""
                // Pobieramy utwór i przypisujemy zwrócony tytuł do zmiennej
                val result = musicPlayer.loadAndPlaySong(89077555)
                if (result != null) {
                    correctTitle = result.first
                    artist = result.second
                    isSongLoaded = true
                    message = "Utwór odtworzony! Zgadnij tytuł."
                } else {
                    message = "Błąd pobierania utworu."
                }
            }
        }) {
            Text(text = "Odtwórz losową piosenkę")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSongLoaded) {
            OutlinedTextField(
                value = userGuess,
                onValueChange = { userGuess = it },
                label = { Text("Wpisz tytuł piosenki") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                // ignorowanie wielkości liter
                if (userGuess.trim().equals(correctTitle.trim(), ignoreCase = true)) {
                    message = "Brawo! Poprawna odpowiedź: $correctTitle - $artist"
                } else {
                    message = "Błędnie! Spróbuj ponownie."
                }
            }) {
                Text(text = "Sprawdź odpowiedź")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // komunikat na dole
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}