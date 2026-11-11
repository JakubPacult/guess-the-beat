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
    var currentRound by mutableStateOf(1)
        private set
    val totalRounds = 5 // Gra kończy się po 5 piosenkach
    var isGameSessionFinished by mutableStateOf(false)
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

    // --- NOWE ZMIENNE WYSZUKIWARKI ---
    var searchQuery by mutableStateOf("")
        private set
    // Używamy mutableStateListOf aby Compose obserwowało zmiany na liście
    var searchResults = mutableStateListOf<DeezerPlaylist>()
        private set
    var isSearching by mutableStateOf(false)
        private set

    // --- Domyślne playlisty ---
    val availablePlaylists = listOf(
        DeezerPlaylist(5123717724, "Hity lat 2000+", "Największe przeboje XXI wieku"),
        DeezerPlaylist(3227703762, "Rock Classics", "Ponadczasowe hymny rockowe"),
        DeezerPlaylist(1115594221, "Pop Top", "Świeże hity popowe"),
        DeezerPlaylist(1963962142, "Polski Rap", "Najlepsze kawałki PL rap"),
        DeezerPlaylist(1447230235, "Top 50", "Obecnie na czasie")
        DeezerPlaylist(1578812305, "Rock Classics", "Ponadczasowe hymny rockowe"), // 1578812305
        DeezerPlaylist(3227703762, "Pop Top", "Świeże hity popowe")
    )

    var selectedPlaylist by mutableStateOf<DeezerPlaylist?>(null)
        private set

    private var playlistTrackIds = listOf<Int>()
    private var correctTitle = ""

    fun updateGuess(newGuess: String) {
        userGuess = newGuess
    }

    // NOWE: Metoda aktualizująca pole wyszukiwania
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    // NOWE: Metoda wykonująca wyszukiwanie przez MusicPlayer
    fun searchForPlaylists(musicPlayer: MusicPlayer) {
        if (searchQuery.trim().isEmpty()) {
            searchResults.clear()
            return
        }

        isSearching = true
        viewModelScope.launch {
            val results = musicPlayer.searchPlaylists(searchQuery)
            searchResults.clear()
            searchResults.addAll(results)
            isSearching = false
        }
    }

    // NOWE: Czyści pole wyszukiwania i wyniki
    fun clearSearch() {
        searchQuery = ""
        searchResults.clear()
    }

    fun selectPlaylist(playlist: DeezerPlaylist) {
        selectedPlaylist = playlist
    }

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
            currentRound = 1 // Reset rund
            isGameSessionFinished = false // Reset stanu końca
            attemptsLeft = 6
            isAnswerCorrect = false
            isRoundOver = false
            incorrectGuesses.clear()

            val ids = musicPlayer.loadPlaylist(playlist.id)

            if (ids.isNotEmpty()) {
                playlistTrackIds = ids
                playRandomSong(musicPlayer)
            } else {
                message = "Nie udało się pobrać playlisty."
            }
        }
    }

    fun nextRound(musicPlayer: MusicPlayer) {
        if (currentRound >= totalRounds) {
            isGameSessionFinished = true
            musicPlayer.stop()
        } else {
            currentRound++
            playRandomSong(musicPlayer)
        }
    }
    fun playRandomSong(musicPlayer: MusicPlayer) {
        if (playlistTrackIds.isEmpty()) {
            message = "Brak utworów z dostępnym audio na tej playliście."
            return
        }

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
                // JEŚLI UTWÓR JEST PUSTY/USZKODZONY:
                // 1. Wyrzucamy to ID z naszej puli, żeby nie trafić na nie ponownie
                val mutableIds = playlistTrackIds.toMutableList()
                mutableIds.remove(randomId)
                playlistTrackIds = mutableIds

                // 2. Automatycznie próbujemy załadować inny utwór, jeśli coś zostało
                if (playlistTrackIds.isNotEmpty()) {
                    message = "Pomijanie utworu bez dema audio..."
                    playRandomSong(musicPlayer) // Rekurencyjne wywołanie dla nowego utworu
                } else {
                    message = "Żaden utwór na tej playliście nie posiada dema audio."
                }
                message = "Pominięto uszkodzony utwór, losuję kolejny..."
                playRandomSong(musicPlayer)
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