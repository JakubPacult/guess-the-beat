package com.example.guessthebeat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Model reprezentujący playlistę
data class DeezerPlaylist(
    val id: Long,
    val name: String,
    val description: String
)

class GameViewModel : ViewModel() {
    var score by mutableStateOf(0)
        private set
    var message by mutableStateOf("")
        private set
    var userGuess by mutableStateOf("")
        private set
    var isSongLoaded by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isFinished by mutableStateOf(false)
        private set
    var correctArtist by mutableStateOf("")
        private set
    var isAnswerCorrect by mutableStateOf(false)
        private set
    var attemptsLeft by mutableStateOf(6)
        private set

    val incorrectGuesses = mutableStateListOf<String>()

    var isRoundOver by mutableStateOf(false)
        private set

    // --- Obsługa wielu playlist ---
    val availablePlaylists = listOf(
        DeezerPlaylist(5123717724, "Hity lat 2000+", "Największe przeboje XXI wieku"),
        DeezerPlaylist(3227703762, "Rock Classics", "Ponadczasowe hymny rockowe"),
        DeezerPlaylist(1115594221, "Pop Top", "Świeże hity popowe") // Przykładowe ID, możesz podmienić na własne
    )

    var selectedPlaylist by mutableStateOf<DeezerPlaylist?>(null)
        private set

    private var playlistTrackIds = listOf<Int>()
    private var correctTitle = ""

    fun updateGuess(newGuess: String) {
        userGuess = newGuess
    }

    // Wybór playlisty przez użytkownika
    fun selectPlaylist(playlist: DeezerPlaylist) {
        selectedPlaylist = playlist
    }

    // Reset wyboru, aby powrócić do menu playlist
    fun resetPlaylistSelection() {
        selectedPlaylist = null
        isSongLoaded = false
    }

    fun loadPlaylistAndPlay(musicPlayer: MusicPlayer) {
        val playlist = selectedPlaylist ?: return

        viewModelScope.launch {
            message = "Pobieranie playlisty: ${playlist.name}..."
            userGuess = ""
            score = 0
            attemptsLeft = 6
            isAnswerCorrect = false
            isRoundOver = false
            incorrectGuesses.clear()

            // Pobieramy ID utworów dla wybranej playlisty
            val ids = musicPlayer.loadPlaylist(playlist.id)

            if (ids.isNotEmpty()) {
                playlistTrackIds = ids
                playRandomSong(musicPlayer)
            } else {
                message = "Nie udało się pobrać playlisty."
            }
        }
    }

    fun playRandomSong(musicPlayer: MusicPlayer) {
        if (playlistTrackIds.isEmpty()) return
        val randomId = playlistTrackIds.random()

        isRoundOver = false
        isAnswerCorrect = false
        userGuess = ""
        attemptsLeft = 6
        incorrectGuesses.clear()

        viewModelScope.launch {
            val result = musicPlayer.loadAndPlaySong(randomId) {
                if (!isRoundOver) {
                    isPlaying = false
                    isFinished = true
                    message = "Utwór się skończył. Kliknij, aby odtworzyć ponownie."
                }
            }

            if (result != null) {
                correctTitle = result.first
                correctArtist = result.second
                isSongLoaded = true
                isPlaying = true
                isFinished = false
                message = "Zgadnij tytuł utworu! Pozostało prób: $attemptsLeft"
            } else {
                message = "Błąd odtwarzania utworu."
            }
        }
    }

    fun checkAnswer(musicPlayer: MusicPlayer) {
        if (isRoundOver || attemptsLeft <= 0) return

        val currentGuess = userGuess.trim()
        if (currentGuess.isEmpty()) return

        if (currentGuess.equals(correctTitle.trim(), ignoreCase = true)) {
            score += 1
            isAnswerCorrect = true
            isRoundOver = true
            isPlaying = false
            musicPlayer.pause()
            message = "🎉 Brawo! Poprawna odpowiedź. Kliknij, aby przejść dalej."
        } else {
            attemptsLeft -= 1
            incorrectGuesses.add(currentGuess)
            if (incorrectGuesses.size > 5) {
                incorrectGuesses.removeAt(0)
            }
            userGuess = ""

            if (attemptsLeft > 0) {
                message = "❌ Błędnie! Pozostało prób: $attemptsLeft"
            } else {
                isAnswerCorrect = false
                isRoundOver = true
                isPlaying = false
                musicPlayer.pause()
                message = "Koniec prób! Prawidłowy tytuł to: $correctTitle\n$correctArtist. Spróbuj kolejnej piosenki."
            }
        }
    }

    fun togglePlayPause(musicPlayer: MusicPlayer) {
        if (isRoundOver) return

        when {
            isFinished -> {
                musicPlayer.restart()
                isPlaying = true
                isFinished = false
                message = "Słuchaj uważnie! Pozostało prób: $attemptsLeft"
            }
            isPlaying -> {
                musicPlayer.pause()
                isPlaying = false
                message = "Muzyka zatrzymana."
            }
            else -> {
                musicPlayer.play()
                isPlaying = true
                message = "Odtwarzanie..."
            }
        }
    }
}