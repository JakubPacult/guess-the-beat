package com.example.guessthebeat

import androidx.compose.runtime.getValue
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
        private set
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

    private val attemptDurations = listOf(200L, 1000L, 3000L, 6000L, 15000L)

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
            score += 1
            isAnswerCorrect = true
            isRoundOver = true
            isPlaying = false
            musicPlayer.stop()
            message = "🎉 Brawo! Poprawna odpowiedź."
        } else {
            attemptsLeft -= 1
            incorrectGuesses.add(currentGuess)
            if (incorrectGuesses.size > 5) {
                incorrectGuesses.removeAt(0)
            }
            userGuess = ""

            if (attemptsLeft > 0) {
                val attemptIndex = 6 - attemptsLeft
                val nextDuration = if (attemptIndex < attemptDurations.size) attemptDurations[attemptIndex] else 15000L

                musicPlayer.playSnippet(nextDuration)
                val secondsText = if (nextDuration < 1000) "${nextDuration}ms" else "${nextDuration / 1000}s"
                message = "❌ Błędnie! Odsłuch (${secondsText}). Pozostało prób: $attemptsLeft"
            } else {
                isAnswerCorrect = false
                isRoundOver = true
                isPlaying = false
                musicPlayer.stop()
                message = "Koniec prób! Prawidłowy tytuł to: $correctTitle\n$correctArtist."
            }
        }
    }

    fun togglePlayPause(musicPlayer: MusicPlayer) {
        if (isRoundOver) return
        val attemptIndex = 6 - attemptsLeft
        val currentDuration = if (attemptIndex < attemptDurations.size) attemptDurations[attemptIndex] else 15000L
        musicPlayer.playSnippet(currentDuration)
        message = "Odtwarzanie próbki..."
    }
}