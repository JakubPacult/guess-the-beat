package com.example.guessthebeat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DeezerPlaylist(
    val id: Long,
    val name: String,
    val description: String,
    val imageUrl: String = ""
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
    var isLoadingSong by mutableStateOf(false)
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

    val chartPlaylists = mutableStateListOf<ChartItem>()
    val chartArtists = mutableStateListOf<ChartItem>()
    val chartAlbums = mutableStateListOf<ChartItem>()
    var isChartsLoading by mutableStateOf(false)
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

    fun loadCharts(musicPlayer: MusicPlayer) {
        if (chartPlaylists.isNotEmpty()) return
        isChartsLoading = true
        viewModelScope.launch {
            val playlists = musicPlayer.fetchChartPlaylists()
            val artists = musicPlayer.fetchChartArtists()
            val albums = musicPlayer.fetchChartAlbums()

            chartPlaylists.clear()
            chartPlaylists.addAll(playlists)

            chartArtists.clear()
            chartArtists.addAll(artists)

            chartAlbums.clear()
            chartAlbums.addAll(albums)

            isChartsLoading = false
        }
    }

    fun selectChartPlaylist(item: ChartItem, musicPlayer: MusicPlayer) {
        selectedPlaylist = DeezerPlaylist(item.id, item.title, "Najpopularniejsza playlista", item.imageUrl)
    }

    fun selectChartArtist(item: ChartItem, musicPlayer: MusicPlayer) {
        selectedPlaylist = DeezerPlaylist(item.id, item.title, "Top Artysta", item.imageUrl)
    }

    fun selectChartAlbum(item: ChartItem, musicPlayer: MusicPlayer) {
        selectedPlaylist = DeezerPlaylist(item.id, item.title, "Top Album", item.imageUrl)
    }

    private var playlistTrackIds = listOf<Int>()
    private var gameTrackIds = listOf<Int>()
    var correctTitle = ""
    private var currentTrackId = 0

    // 6 poziomów czasu odsłuchu (dla 6 prób)
    private val attemptDurations = listOf(200L, 1000L, 3000L, 6000L, 10000L, 15000L)

    var totalScore by mutableStateOf(0)
        private set

    // Słownik punktacji za poszczególne próby (indeks 0 = 1. próba, indeks 5 = 6. próba)
    private val attemptScores = listOf(5000, 4000, 3000, 2000, 1000, 500)

    val maxScore = 25000

    fun getCurrentSnippetDuration(): Long {
        val attemptIndex = 6 - attemptsLeft
        return if (attemptIndex in attemptDurations.indices) {
            attemptDurations[attemptIndex]
        } else {
            15000L
        }
    }

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
        isLoadingSong = false
        incorrectGuesses.clear()
        songSuggestions.clear()
        showSuggestions = false
        playlistTrackIds = emptyList()
        gameTrackIds = emptyList()
    }

    fun loadPlaylistAndPlay(musicPlayer: MusicPlayer) {
        val playlist = selectedPlaylist ?: return
        if (isLoadingSong) return

        viewModelScope.launch {
            musicPlayer.stop()
            message = "Pobieranie utworów: ${playlist.name}..."
            isLoadingSong = true
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
            playlistTrackIds = emptyList()
            gameTrackIds = emptyList()

            val timeoutJob = launch {
                delay(6000L)
                if (isLoadingSong) {
                    message = "⏳ Ładowanie trwa dłużej niż zwykle. Sprawdź połączenie z internetem..."
                }
            }

            try {
                val ids = when {
                    playlist.description.contains("Artysta", ignoreCase = true) -> musicPlayer.loadArtistTracks(playlist.id)
                    playlist.description.contains("Album", ignoreCase = true) -> musicPlayer.loadAlbumTracks(playlist.id)
                    else -> musicPlayer.loadPlaylist(playlist.id)
                }

                timeoutJob.cancel()

                if (ids.isNullOrEmpty()) {
                    isLoadingSong = false
                    message = "⚠️ Nie udało się pobrać utworów z tej pozycji. Wybierz inną."
                    return@launch
                }

                if (ids.size < 5) {
                    isLoadingSong = false
                    selectedPlaylist = null
                    message = "⚠️ Wybrana pozycja zawiera za mało utworów (${ids.size}/5). Wybierz inną."
                    return@launch
                }

                playlistTrackIds = ids
                gameTrackIds = ids.shuffled().take(totalRounds)
                timeoutJob.cancel()
                isLoadingSong = false
                playRandomSong(musicPlayer)

            } catch (e: Exception) {
                timeoutJob.cancel()
                isLoadingSong = false
                message = "⚠️ Błąd ładowania: ${e.localizedMessage ?: "Nieznany błąd"}"
            }
        }
    }

    fun nextRound(musicPlayer: MusicPlayer) {
        if (isLoadingSong) return
        if (currentRound >= totalRounds) {
            isGameSessionFinished = true
            musicPlayer.stop()
        } else {
            currentRound++
            playRandomSong(musicPlayer)
        }
    }

    fun playRandomSong(musicPlayer: MusicPlayer) {
        if (isLoadingSong) return

        if (gameTrackIds.isEmpty() || currentRound - 1 >= gameTrackIds.size) {
            if (playlistTrackIds.size >= 5) {
                gameTrackIds = playlistTrackIds.shuffled().take(totalRounds)
            } else {
                message = "Brak wystarczającej liczby utworów."
                return
            }
        }

        val trackIndex = (currentRound - 1).coerceIn(0, gameTrackIds.size - 1)
        currentTrackId = gameTrackIds[trackIndex]

        totalScore = 0
        isRoundOver = false
        isAnswerCorrect = false
        userGuess = ""
        attemptsLeft = 6
        incorrectGuesses.clear()
        songSuggestions.clear()
        showSuggestions = false
        isLoadingSong = true

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

            isLoadingSong = false

            if (result != null) {
                correctTitle = result.first
                correctArtist = result.second
                correctCoverUrl = result.third
                isSongLoaded = true
                isPlaying = true
                message = "Zgadnij tytuł (Próba 1: 0.2s)!"
            } else {
                val availableReplacements = playlistTrackIds.filter { it !in gameTrackIds && it != currentTrackId }
                if (availableReplacements.isNotEmpty()) {
                    val replacement = availableReplacements.random()
                    val mutableGameTracks = gameTrackIds.toMutableList()
                    mutableGameTracks[trackIndex] = replacement
                    gameTrackIds = mutableGameTracks
                    playRandomSong(musicPlayer)
                } else {
                    message = "Nie udało się załadować utworu (brak podglądu audio). Spróbuj innej playlisty."
                    isRoundOver = true
                }
            }
        }
    }

    fun checkAnswer(musicPlayer: MusicPlayer) {
        if (isRoundOver || attemptsLeft <= 0 || isLoadingSong) return

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
        if (isRoundOver || isLoadingSong) return
        val currentDuration = getCurrentSnippetDuration()
        musicPlayer.playSnippet(currentDuration)
        message = "Odtwarzanie próbki..."
    }

    fun skipSong(musicPlayer: MusicPlayer) {
        if (isRoundOver || isLoadingSong) return

        musicPlayer.stop()
        isRoundOver = true
        isPlaying = false
        attemptsLeft = 0
        isAnswerCorrect = false
        message = "⏭️ Koniec prób. Prawidłowy tytuł to: $correctTitle - $correctArtist."
    }

    fun skipCurrentAttempt(musicPlayer: MusicPlayer) {
        if (isRoundOver || isLoadingSong) return

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
