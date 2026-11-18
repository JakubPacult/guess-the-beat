package com.example.guessthebeat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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
    val totalRounds = 5
    var isGameSessionFinished by mutableStateOf(false)
        private set
    var userGuess by mutableStateOf("")
        private set
    var isSongLoaded by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
    var correctArtist by mutableStateOf("")
        private set
    var correctCoverUrl by mutableStateOf("")
        private set
    var correctAlbum by mutableStateOf("")
        private set
    var isAnswerCorrect by mutableStateOf(false)
        private set
    var attemptsLeft by mutableStateOf(6)
        private set

    val incorrectGuesses = mutableStateListOf<String>()
    var isRoundOver by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
        private set
    var searchResults = mutableStateListOf<DeezerPlaylist>()
        private set
    var isSearching by mutableStateOf(false)
        private set

    var songSuggestions = mutableStateListOf<SongSuggestion>()
        private set
    var showSuggestions by mutableStateOf(false)
        private set

    val availablePlaylists = listOf(
        DeezerPlaylist(5123717724, "Hity lat 2000+", "Największe przeboje XXI wieku"),
        DeezerPlaylist(3227703762, "Rock Classics", "Ponadczasowe hymny rockowe"),
        DeezerPlaylist(1115594221, "Pop Top", "Świeże hity popowe"),
        DeezerPlaylist(1963962142, "Polski Rap", "Najlepsze kawałki PL rap"),
        DeezerPlaylist(1447230235, "Top 50", "Obecnie na czasie")
    )

    var selectedPlaylist by mutableStateOf<DeezerPlaylist?>(null)
        private set

    private var playlistTrackIds = listOf<Int>()
    var correctTitle = ""
    private var currentTrackId = 0

    // 6 poziomów czasu odsłuchu (dla 6 prób)
    private val attemptDurations = listOf(200L, 1000L, 3000L, 6000L, 10000L, 15000L)

    var totalScore by mutableStateOf(0)
        private set

    // Słownik punktacji za poszczególne próby (indeks 0 = 1. próba, indeks 5 = 6. próba)
    private val attemptScores = listOf(5000, 4000, 3000, 2000, 1000, 500)

    val maxScore = 25000

    // 💡 Pomocnicza funkcja zwracająca czas trwania obecnego kodu na podstawie prób
    fun getCurrentSnippetDuration(): Long {
        val attemptIndex = 6 - attemptsLeft
        return if (attemptIndex in attemptDurations.indices) {
            attemptDurations[attemptIndex]
        } else {
            15000L
        }
    }

    // Powrót do ekranu głównego po błędzie sieci
    fun handleNetworkError(musicPlayer: MusicPlayer) {
        resetPlaylistSelection(musicPlayer)
        message = "⚠️ Brak połączenia z siecią! Powrót do ekranu głównego."
    }

    fun updateGuess(newGuess: String, musicPlayer: MusicPlayer) {
        userGuess = newGuess
        if (newGuess.length >= 2) {
            viewModelScope.launch {
                val suggestions = musicPlayer.searchTracks(newGuess)
                songSuggestions.clear()
                songSuggestions.addAll(suggestions)
                showSuggestions = suggestions.isNotEmpty()
            }
        } else {
            songSuggestions.clear()
            showSuggestions = false
        }
    }

    fun selectSuggestion(suggestion: SongSuggestion, musicPlayer: MusicPlayer) {
        userGuess = suggestion.title
        songSuggestions.clear()
        showSuggestions = false
        checkAnswer(musicPlayer)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun searchForPlaylists(musicPlayer: MusicPlayer) {
        if (searchQuery.trim().isEmpty()) {
            searchResults.clear()
            return
        }

        isSearching = true
        viewModelScope.launch {
            val results = musicPlayer.searchPlaylists(searchQuery)
            if (results == null) {
                handleNetworkError(musicPlayer)
            } else {
                searchResults.clear()
                searchResults.addAll(results)
            }
            isSearching = false
        }
    }

    fun clearSearch() {
        searchQuery = ""
        searchResults.clear()
    }

    fun selectPlaylist(playlist: DeezerPlaylist) {
        selectedPlaylist = playlist
    }

    fun resetPlaylistSelection(musicPlayer: MusicPlayer) {
        musicPlayer.stop()
        totalScore = 0
        selectedPlaylist = null
        isSongLoaded = false
        isGameSessionFinished = false
        score = 0
        currentRound = 1
        userGuess = ""
        isRoundOver = false
        message = ""
        incorrectGuesses.clear()
        songSuggestions.clear()
        showSuggestions = false
    }

    fun loadPlaylistAndPlay(musicPlayer: MusicPlayer) {
        val playlist = selectedPlaylist ?: return

        viewModelScope.launch {
            musicPlayer.stop()
            message = "Pobieranie playlisty: ${playlist.name}..."
            totalScore = 0
            userGuess = ""
            score = 0
            currentRound = 1
            isGameSessionFinished = false
            attemptsLeft = 6
            isAnswerCorrect = false
            isRoundOver = false
            incorrectGuesses.clear()
            songSuggestions.clear()
            showSuggestions = false

            val ids = musicPlayer.loadPlaylist(playlist.id)

            if (ids == null) {
                handleNetworkError(musicPlayer)
            } else if (ids.isNotEmpty()) {
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

        currentTrackId = playlistTrackIds.random()

        totalScore = 0
        isRoundOver = false
        isAnswerCorrect = false
        userGuess = ""
        attemptsLeft = 6
        incorrectGuesses.clear()
        songSuggestions.clear()
        showSuggestions = false

        val duration = attemptDurations[0]

        viewModelScope.launch {
            val result = musicPlayer.loadAndPlaySong(
                trackId = currentTrackId,
                durationMillis = duration,
                onSongFinished = {
                    isPlaying = false
                    message = "Próbka zakończona. Wpisz tytuł!"
                },
                onNetworkError = {
                    handleNetworkError(musicPlayer)
                }
            )

            if (result != null) {
                correctTitle = result.first
                correctArtist = result.second
                correctCoverUrl = result.third
                isSongLoaded = true
                isPlaying = true
                message = "Zgadnij tytuł (Próba 1: 0.2s)!"
            } else if (selectedPlaylist != null) {
                val mutableIds = playlistTrackIds.toMutableList()
                mutableIds.remove(currentTrackId)
                playlistTrackIds = mutableIds

                if (playlistTrackIds.isNotEmpty()) {
                    playRandomSong(musicPlayer)
                }
            }
        }
    }

    fun checkAnswer(musicPlayer: MusicPlayer) {
        if (isRoundOver || attemptsLeft <= 0) return

        val currentGuess = userGuess.trim()
        if (currentGuess.isEmpty()) return

        songSuggestions.clear()
        showSuggestions = false

        if (currentGuess.equals(correctTitle.trim(), ignoreCase = true)) {
            val attemptIndex = 6 - attemptsLeft
            val earnedPoints = if (attemptIndex in attemptScores.indices) attemptScores[attemptIndex] else 0

            score += earnedPoints
            isAnswerCorrect = true
            isRoundOver = true
            isPlaying = false
            musicPlayer.stop()
            message = "🎉 Brawo! Poprawna odpowiedź (+${earnedPoints} pkt)."
        } else {
            attemptsLeft -= 1
            incorrectGuesses.add(currentGuess)
            if (incorrectGuesses.size > 5) {
                incorrectGuesses.removeAt(0)
            }
            userGuess = ""

            if (attemptsLeft > 0) {
                val nextDuration = getCurrentSnippetDuration()
                musicPlayer.playSnippet(nextDuration)
                val secondsText = if (nextDuration < 1000) "${nextDuration}ms" else "${nextDuration / 1000}s"
                message = "❌ Błędnie! Odsłuch (${secondsText}). Pozostało prób: $attemptsLeft"
            } else {
                isAnswerCorrect = false
                isRoundOver = true
                isPlaying = false
                musicPlayer.stop()
                message = "Koniec prób! 0 punktów. Prawidłowy tytuł to: $correctTitle\n$correctArtist."
            }
        }
    }

    fun togglePlayPause(musicPlayer: MusicPlayer) {
        if (isRoundOver) return
        val currentDuration = getCurrentSnippetDuration()
        musicPlayer.playSnippet(currentDuration)
        message = "Odtwarzanie próbki..."
    }

    // --- 💡 DODANA FUNKCJA SKIP SONG (kończy rundę z 0 pkt, gdy skończą się próby lub po wymuszonym pominięciu) ---
    fun skipSong(musicPlayer: MusicPlayer) {
        if (isRoundOver) return

        musicPlayer.stop()
        isRoundOver = true
        isPlaying = false
        attemptsLeft = 0
        isAnswerCorrect = false
        message = "⏭️ Koniec prób. Prawidłowy tytuł to: $correctTitle - $correctArtist."
    }

    // --- 💡 POPRAWIONA FUNKCJA SKIPOWANIA ETAPU CZASOWEGO ---
    fun skipCurrentAttempt(musicPlayer: MusicPlayer) {
        if (isRoundOver) return

        if (attemptsLeft > 1) {
            attemptsLeft -= 1
            val nextDuration = getCurrentSnippetDuration()

            musicPlayer.playSnippet(nextDuration)
            isPlaying = true
            val secondsText = if (nextDuration < 1000) "${nextDuration}ms" else "${nextDuration / 1000}s"
            message = "⏭️ Pominięto próbę. Nowy czas odsłuchu: ${secondsText}. Pozostało prób: $attemptsLeft"
        } else {
            skipSong(musicPlayer)
        }
    }

    // Nawigacja
    var currentScreen by mutableStateOf("menu")
        private set

    fun navigateTo(screen: String) {
        currentScreen = screen
    }

    var showExitConfirmation by mutableStateOf(false)
        private set

    private var pendingDestination: String = "menu"

    fun requestExit(destination: String) {
        if (selectedPlaylist != null && !isGameSessionFinished) {
            pendingDestination = destination
            showExitConfirmation = true
        } else {
            navigateToDestination(destination)
        }
    }

    fun confirmExit(musicPlayer: MusicPlayer) {
        showExitConfirmation = false
        val dest = pendingDestination
        navigateToDestination(dest)

        if (dest == "menu") {
            resetPlaylistSelection(musicPlayer)
        }
    }

    fun dismissExit() {
        showExitConfirmation = false
    }

    private fun navigateToDestination(destination: String) {
        currentScreen = destination
    }
}