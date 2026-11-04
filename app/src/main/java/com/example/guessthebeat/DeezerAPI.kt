package com.example.guessthebeat

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Klasa odpowiedzialna za zarządzanie pobieraniem playlist oraz odtwarzaniem utworów
 * (wraz z podglądem audio) za pomocą API Deezer oraz Androidowego MediaPlayer.
 */
class MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Pobiera listę ID utworów z określonej playlisty z API Deezer.
     *
     * @param playlistId Unikalne ID playlisty w serwisie Deezer.
     * @return Lista identyfikatorów utworów (Int). W przypadku błędu zwraca pustą listę.
     */
    suspend fun loadPlaylist(playlistId: Long): List<Int> {
        // Przenosimy operację sieciową do wątku I/O, aby nie blokować głównego wątku UI
        return withContext(Dispatchers.IO) {
            try {
                // Pobieranie danych JSON z endpointu playlisty Deezer
                val jsonString = URL("https://api.deezer.com/playlist/$playlistId").readText()
                val jsonObject = JSONObject(jsonString)

                val tracksData = jsonObject.getJSONObject("tracks").getJSONArray("data")
                val trackIds = mutableListOf<Int>()

                // Parsowanie listy utworów i wyciąganie ich ID
                for (i in 0 until tracksData.length()) {
                    val trackObj = tracksData.getJSONObject(i)
                    trackIds.add(trackObj.getInt("id"))
                }
                trackIds
            } catch (e: Exception) {
                // Logowanie błędu i zabezpieczenie przed awarią aplikacji
                Log.e("MusicPlayer", "Błąd pobierania playlisty: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Pobiera metadane utworu oraz link do próbki audio (preview) po jego ID,
     * a następnie inicjalizuje i uruchamia odtwarzacz.
     *
     * @param trackId Unikalne ID utworu.
     * @param onSongFinished Callback wywoływany w momencie, gdy utwór dobiegnie końca.
     * @return Para zawierająca tytuł utworu oraz wykonawcę (Pair<Title, Artist>) lub null w przypadku błędu.
     */
    suspend fun loadAndPlaySong(trackId: Int, onSongFinished: () -> Unit): Pair<String, String>? {
        // Zatrzymujemy poprzednie odtwarzanie przed załadowaniem nowego utworu
        stop()
        return try {
            // Pobieranie szczegółów utworu w wątku I/O (parametr ts zapobiega buforowaniu przez sieć)
            val jsonString = withContext(Dispatchers.IO) {
                URL("https://api.deezer.com/track/$trackId?ts=${System.currentTimeMillis()}").readText()
            }

            val jsonObject = JSONObject(jsonString)
            val previewUrl = jsonObject.getString("preview")
            val title = jsonObject.getString("title")
            val artistObject = jsonObject.getJSONObject("artist")
            val artist = artistObject.getString("name")

            // Bezpieczne zwolnienie starego odtwarzacza przed stworzeniem nowego
            mediaPlayer?.release()
            mediaPlayer = null

            // Konfiguracja nowego instancjonowanego obiektu MediaPlayer
            val newPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                // Uruchomienie odtwarzania automatycznie po pełnym przygotowaniu (zabuforowaniu) strumienia
                setOnPreparedListener { player ->
                    player.start()
                }
                // Nasłuchiwanie końca utworu i wywołanie przekazanej akcji zwrotnej
                setOnCompletionListener {
                    onSongFinished()
                }
                prepareAsync() // Asynchroniczne przygotowanie, aby nie blokować wątku
            }

            mediaPlayer = newPlayer

            Pair(title, artist)
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd odtwarzania: ${e.message}")
            null
        }
    }

    /**
     * Tymczasowo wstrzymuje (pauzuje) odtwarzanie bieżącego utworu.
     */
    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd pauzowania: ${e.message}")
        }
    }

    /**
     * Wznawia odtwarzanie utworu od momentu wstrzymania.
     */
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

    /**
     * Przewija utwór do samego początku (0 ms) i uruchamia go ponownie.
     */
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

    /**
     * Całkowicie zatrzymuje odtwarzacz, zwalnia zasoby systemowe i usuwa instancję.
     */
    fun stop() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Błąd zatrzymywania: ${e.message}")
        }
    }
}