package com.example.guessthebeat

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

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