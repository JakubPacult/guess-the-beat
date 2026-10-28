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
            val jsonString = withContext(Dispatchers.IO) {URL("https://api.deezer.com/track/$trackId?ts=${System.currentTimeMillis()}").readText()
            }

            val jsonObject = JSONObject(jsonString)
            val previewUrl = jsonObject.getString("preview")
            val title = jsonObject.getString("title")
            val artistObject = jsonObject.getJSONObject("artist")
            val artist = artistObject.getString("name")

            // Bezpieczne zwolnienie starego odtwarzacza przed stworzeniem nowego
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
            // Sprawdzamy, czy odtwarzacz w ogóle istnieje przed uruchomieniem
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