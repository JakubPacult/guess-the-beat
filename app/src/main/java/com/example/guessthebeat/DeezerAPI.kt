package com.example.guessthebeat

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    // NOWE: Funkcja wyszukująca playlisty
    suspend fun searchPlaylists(query: String): List<DeezerPlaylist> {
        return withContext(Dispatchers.IO) {
            try {
                // Kodujemy zapytanie, żeby uniknąć błędów przy spacjach
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                // Wywołujemy endpoint wyszukiwania playlist w Deezer API
                val jsonString = URL("https://api.deezer.com/search/playlist?q=$encodedQuery").readText()
                val jsonObject = JSONObject(jsonString)
                val data = jsonObject.getJSONArray("data")

                val results = mutableListOf<DeezerPlaylist>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    // Niektóre wyniki wyszukiwania mogą nie mieć pola opisu (description) - używamy tytułu jako opisu
                    results.add(
                        DeezerPlaylist(
                            id = item.getLong("id"),
                            name = item.getString("title"),
                            description = if (item.has("description")) item.getString("description") else "Playlista z Deezer"
                        )
                    )
                }
                results
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Błąd wyszukiwania playlist: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun loadPlaylist(playlistId: Long): List<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/playlist/$playlistId").readText()
                val jsonObject = JSONObject(jsonString)
                val tracksData = jsonObject.getJSONObject("tracks").getJSONArray("data")
                val trackIds = mutableListOf<Int>()

                for (i in 0 until tracksData.length()) {
                    val trackObj = tracksData.getJSONObject(i)
                    trackIds.add(trackObj.getInt("id"))
                }
                trackIds
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Błąd pobierania playlisty: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun loadAndPlaySong(trackId: Int, onSongFinished: () -> Unit): Pair<String, String>? {
        stop()
        return try {
            val jsonString = withContext(Dispatchers.IO) {
                URL("https://api.deezer.com/track/$trackId?ts=${System.currentTimeMillis()}").readText()
            }
            val jsonObject = JSONObject(jsonString)

            // Pobieramy link, ale jeśli go nie ma, używamy pustego stringa
            val previewUrl = jsonObject.optString("preview", "")

            // ZABEZPIECZENIE: Jeśli utwór nie ma próbki, od razu przerywamy i zwracamy null
            if (previewUrl.isEmpty()) {
                Log.e("MusicPlayer", "Utwór ID $trackId nie posiada podglądu audio.")
                return null
            }

            val title = jsonObject.getString("title")
            val artistObject = jsonObject.getJSONObject("artist")
            val artist = artistObject.getString("name")

            mediaPlayer?.release()
            mediaPlayer = null

            val newPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                setOnPreparedListener { player ->
                    player.start()
                }
                setOnCompletionListener {
                    onSongFinished()
                }
                // Dodajemy obsługę błędów sieci/buforowania
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicPlayer", "Błąd MediaPlayer: $what, $extra")
                    onSongFinished() // Kończymy odtwarzanie, żeby UI nie wisiało
                    true
                }
                prepareAsync()
            }
            mediaPlayer = newPlayer
            Pair(title, artist)
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania: ${e.message}")
            null
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd pauzowania: ${e.message}")
        }
    }

    fun play() {
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd wznawiania: ${e.message}")
        }
    }

    fun restart() {
        try {
            mediaPlayer?.let { player ->
                player.seekTo(0)
                player.start()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd restartu: ${e.message}")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd zatrzymywania: ${e.message}")
        }
    }
}