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

data class ChartItem(
    val id: Long,
    val title: String,
    val imageUrl: String
)

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
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
                            description = if (item.has("description")) item.getString("description") else "Playlista z Deezer",
                            imageUrl = item.optString("picture_medium", "")
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
                val tracksObj = jsonObject.optJSONObject("tracks") ?: jsonObject
                val data = tracksObj.optJSONArray("data") ?: return@withContext emptyList()
                val trackIds = mutableListOf<Int>()

                for (i in 0 until data.length()) {
                    val trackObj = data.optJSONObject(i) ?: continue
                    val id = trackObj.optInt("id", 0)
                    if (id != 0) {
                        trackIds.add(id)
                    }
                }
                trackIds.distinct()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Błąd pobierania playlisty: ${e.message}")
                null // Błąd sieci
            }
        }
    }

    suspend fun fetchChartPlaylists(): List<ChartItem> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/chart").readText()
                val jsonObject = JSONObject(jsonString)
                val playlistsObj = jsonObject.optJSONObject("playlists") ?: return@withContext emptyList()
                val data = playlistsObj.optJSONArray("data") ?: return@withContext emptyList()
                val results = mutableListOf<ChartItem>()
                for (i in 0 until minOf(data.length(), 6)) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optLong("id", 0L)
                    val title = item.optString("title", "Playlista")
                    val imageUrl = item.optString("picture_medium", item.optString("cover_medium", ""))
                    if (id != 0L) {
                        results.add(ChartItem(id, title, imageUrl))
                    }
                }
                results
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error fetching chart playlists: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun fetchChartArtists(): List<ChartItem> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/chart").readText()
                val jsonObject = JSONObject(jsonString)
                val artistsObj = jsonObject.optJSONObject("artists") ?: return@withContext emptyList()
                val data = artistsObj.optJSONArray("data") ?: return@withContext emptyList()
                val results = mutableListOf<ChartItem>()
                for (i in 0 until minOf(data.length(), 6)) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optLong("id", 0L)
                    val name = item.optString("name", "Artysta")
                    val imageUrl = item.optString("picture_medium", item.optString("picture", ""))
                    if (id != 0L) {
                        results.add(ChartItem(id, name, imageUrl))
                    }
                }
                results
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error fetching chart artists: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun fetchChartAlbums(): List<ChartItem> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/chart").readText()
                val jsonObject = JSONObject(jsonString)
                val albumsObj = jsonObject.optJSONObject("albums") ?: return@withContext emptyList()
                val data = albumsObj.optJSONArray("data") ?: return@withContext emptyList()
                val results = mutableListOf<ChartItem>()
                for (i in 0 until minOf(data.length(), 6)) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optLong("id", 0L)
                    val title = item.optString("title", "Album")
                    val imageUrl = item.optString("cover_medium", item.optString("cover", ""))
                    if (id != 0L) {
                        results.add(ChartItem(id, title, imageUrl))
                    }
                }
                results
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error fetching chart albums: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun loadAlbumTracks(albumId: Long): List<Int>? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/album/$albumId/tracks").readText()
                val jsonObject = JSONObject(jsonString)
                val data = jsonObject.optJSONArray("data") ?: jsonObject.optJSONObject("tracks")?.optJSONArray("data") ?: return@withContext emptyList()
                val trackIds = mutableListOf<Int>()
                for (i in 0 until data.length()) {
                    val trackObj = data.optJSONObject(i) ?: continue
                    val id = trackObj.optInt("id", 0)
                    if (id != 0) trackIds.add(id)
                }
                trackIds.distinct()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error loading album tracks: ${e.message}")
                null
            }
        }
    }

    suspend fun loadArtistTracks(artistId: Long): List<Int>? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/artist/$artistId/top?limit=50").readText()
                val jsonObject = JSONObject(jsonString)
                val data = jsonObject.optJSONArray("data") ?: jsonObject.optJSONObject("tracks")?.optJSONArray("data") ?: return@withContext emptyList()
                val trackIds = mutableListOf<Int>()
                for (i in 0 until data.length()) {
                    val trackObj = data.optJSONObject(i) ?: continue
                    val id = trackObj.optInt("id", 0)
                    if (id != 0) trackIds.add(id)
                }
                trackIds.distinct()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error loading artist tracks: ${e.message}")
                null
            }
        }
    }

    suspend fun testLoadSong(trackId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/track/$trackId?ts=${System.currentTimeMillis()}").readText()
                val jsonObject = JSONObject(jsonString)
                val previewUrl = jsonObject.optString("preview", "")
                previewUrl.isNotEmpty()
            } catch (e: Exception) {
                false
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

            isPrepared = false
            val newPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                setOnPreparedListener { player ->
                    isPrepared = true
                    player.start()
                    handler.postDelayed({
                        if (isPlaying) {
                            pause()
                            onSongFinished()
                        }
                    }, durationMillis)
                }
                setOnErrorListener { _, _, _ ->
                    isPrepared = false
                    onNetworkError()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = newPlayer
            Triple(title, artist, coverUrl)
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania/sieci: ${e.message}")
            isPrepared = false
            onNetworkError()
            null
        }
    }

    fun playSnippet(durationMillis: Long) {
        try {
            if (!isPrepared) return
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
            if (!isPrepared) return
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd pauzowania: ${e.message}")
        }
    }

    fun stop() {
        try {
            isPrepared = false
            handler.removeCallbacksAndMessages(null)
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd zatrzymywania: ${e.message}")
        }
    }
}
