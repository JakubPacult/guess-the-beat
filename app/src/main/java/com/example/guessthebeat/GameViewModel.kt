package com.example.guessthebeat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

    private var playlistTrackIds = listOf<Int>()
    private var correctTitle = ""

    fun updateGuess(newGuess: String) {
        userGuess = newGuess
    }

    fun loadPlaylistAndPlay(musicPlayer: MusicPlayer) {
        viewModelScope.launch {
            message = "Pobieranie playlisty..."
            userGuess = ""
            score = 0
            attemptsLeft = 6
            isAnswerCorrect = false
            isRoundOver = false
            incorrectGuesses.clear()

            val ids = musicPlayer.loadPlaylist(5123717724) //  3227703762

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
        // Blokujemy sprawdzanie, jeśli runda już minęła lub nie ma prób
        if (isRoundOver || attemptsLeft <= 0) return

        val currentGuess = userGuess.trim()
        if (currentGuess.isEmpty()) return

        if (currentGuess.equals(correctTitle.trim(), ignoreCase = true)) {
            // sukces!
            score += 1
            isAnswerCorrect = true
            isRoundOver = true // 💡 Zamykamy rundę
            isPlaying = false
            musicPlayer.pause()
            message = "🎉 Brawo! Poprawna odpowiedź. Kliknij, aby przejść dalej."
        } else {
            // zła odpowiedź – zmniejszamy liczbę prób
            attemptsLeft -= 1

            // dodanie błędnych prób
            incorrectGuesses.add(currentGuess)
            // maks 5 napisow pod spodem
            if (incorrectGuesses.size > 5) {
                incorrectGuesses.removeAt(0)
            }

            // czyscimy pole do wpisywania
            userGuess = ""

            if (attemptsLeft > 0) {
                message = "❌ Błędnie! Pozostało prób: $attemptsLeft"
            } else {
                // wykorzystano 6 prób – kończymy rundę (przegrana)
                isAnswerCorrect = false
                isRoundOver = true // 💡 Zamykamy rundę, żeby pokazać przycisk następnej piosenki
                isPlaying = false
                musicPlayer.pause()
                message = "Koniec prób! Prawidłowy tytuł to: $correctTitle - $correctArtist. Spróbuj kolejnej piosenki."
            }
        }
    }

    fun togglePlayPause(musicPlayer: MusicPlayer) {
        if (isRoundOver) return // Blokujemy sterowanie, gdy runda się skończyła

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