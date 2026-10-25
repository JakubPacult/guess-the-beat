package com.example.guessthebeat

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun loadPlaylist(playlistId: Long): List<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("https://api.deezer.com/playlist/$playlistId").readText()
                val jsonObject = JSONObject(jsonString)

                // wyłuskanie nazwy piosenki z obiektu "tracks"
                val tracksData = jsonObject.getJSONObject("tracks").getJSONArray("data")
                val trackIds = mutableListOf<Int>()

                for (i in 0 until tracksData.length()) {
                    val trackObj = tracksData.getJSONObject(i)
                    val id = trackObj.getInt("id")
                    trackIds.add(id)
                }

                trackIds // Zwracamy listę ID utworów
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Błąd pobierania playlisty: ${e.message}")
                emptyList()
            }
        }
    }

    // Funkcja do pobierania i odtwarzania utworu
    suspend fun loadAndPlaySong(trackId: Int): Pair<String, String>? {
        return try {
            //url do piosenki
            val jsonString = withContext(Dispatchers.IO) {
                URL("https://api.deezer.com/track/$trackId").readText()
            }

            val jsonObject = JSONObject(jsonString)
            val previewUrl = jsonObject.getString("preview")
            val title = jsonObject.getString("title")

            val artistObject = jsonObject.getJSONObject("artist")
            val artist = artistObject.getString("name")

            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                }
            }

            Pair(title, artist)
        }
        catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania: ${e.message}")
            null
        }
    }

    // Funkcja do zatrzymywania muzyki (przydatna np. przy wychodzeniu z ekranu)
    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}