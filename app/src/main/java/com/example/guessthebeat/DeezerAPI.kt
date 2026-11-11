package com.example.guessthebeat

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class SongSuggestion(
    val id: Int,
    val title: String,
    val artist: String
)

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    suspend fun searchPlaylists(query: String): List<DeezerPlaylist>? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val jsonString = URL("https://api.deezer.com/search/playlist?q=$encodedQuery").readText()
                val jsonObject = JSONObject(jsonString)
                val data = jsonObject.getJSONArray("data")

                val results = mutableListOf<DeezerPlaylist>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
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
                Log.e("MusicPlayer", "Błąd sieci/wyszukiwania playlist: ${e.message}")
                null // Zwracamy null w przypadku błędu sieci
            }
        }
    }

    suspend fun searchTracks(query: String): List<SongSuggestion> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val jsonString = URL("https://api.deezer.com/search?q=$encodedQuery&limit=5").readText()
                val jsonObject = JSONObject(jsonString)
                val data = jsonObject.getJSONArray("data")

                val results = mutableListOf<SongSuggestion>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val trackId = item.getInt("id")
                    val title = item.getString("title")
                    val artist = item.getJSONObject("artist").getString("name")
                    results.add(SongSuggestion(trackId, title, artist))
                }
                results
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Błąd podpowiedzi utworów: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun loadPlaylist(playlistId: Long): List<Int>? {
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
                null // Błąd sieci
            }
        }
    }

    // Wyrzucamy Result z podziałem na sukces, brak podglądu audio lub błąd sieci
    suspend fun loadAndPlaySong(
        trackId: Int,
        durationMillis: Long,
        onSongFinished: () -> Unit,
        onNetworkError: () -> Unit
    ): Triple<String, String, String>? {
        stop()
        return try {
            val jsonString = withContext(Dispatchers.IO) {
                URL("https://api.deezer.com/track/$trackId?ts=${System.currentTimeMillis()}").readText()
            }
            val jsonObject = JSONObject(jsonString)
            val previewUrl = jsonObject.optString("preview", "")

            if (previewUrl.isEmpty()) {
                return null
            }

            val title = jsonObject.getString("title")
            val artist = jsonObject.getJSONObject("artist").getString("name")
            val album = jsonObject.getJSONObject("album")
            val coverUrl = album.optString("cover_medium", "")

            val newPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                setOnPreparedListener { player ->
                    player.start()
                    handler.postDelayed({
                        if (isPlaying) {
                            pause()
                            onSongFinished()
                        }
                    }, durationMillis)
                }
                setOnErrorListener { _, _, _ ->
                    onNetworkError()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = newPlayer
            Triple(title, artist, coverUrl)
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania/sieci: ${e.message}")
            onNetworkError()
            null
        }
    }

    fun playSnippet(durationMillis: Long) {
        try {
            mediaPlayer?.let { player ->
                handler.removeCallbacksAndMessages(null)
                player.seekTo(0)
                player.start()
                handler.postDelayed({
                    if (player.isPlaying) {
                        player.pause()
                    }
                }, durationMillis)
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania próbki: ${e.message}")
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

    fun stop() {
        try {
            handler.removeCallbacksAndMessages(null)
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd zatrzymywania: ${e.message}")
        }
    }
}