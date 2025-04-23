package com.k.kitsch.mainFragments

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import com.k.kitsch.R

// Clase MusicService que gestiona la reproducción de música de fondo como un servicio
class MusicService : Service() {
    // Variables para el reproductor y preferencias
    private var mediaPlayer: MediaPlayer? = null // Reproductor de medios, inicialmente nulo
    private val TAG = "MusicService" // Etiqueta para registros de log
    private val sharedPrefs by lazy {
        getSharedPreferences("AppPrefs", MODE_PRIVATE) // Acceso a preferencias compartidas
    }

    // Objeto companion para constantes y variables compartidas
    companion object {
        const val ACTION_PLAY = "com.example.kitsch.ACTION_PLAY" // Acción para iniciar la música
        const val ACTION_STOP = "com.example.kitsch.ACTION_STOP" // Acción para detener la música
        const val ACTION_SET_VOLUME =
            "com.example.kitsch.ACTION_SET_VOLUME" // Acción para ajustar el volumen
        const val EXTRA_VOLUME_LEVEL = "volume_level" // Clave para el nivel de volumen en el Intent

        var isMusicEnabled = true // Indica si la música está habilitada, por defecto sí
        var currentVolume = 1.0f // Volumen actual, por defecto al máximo
    }

    // Maneja los comandos enviados al servicio
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Evalúa la acción recibida en el Intent
        when (intent?.action) {
            ACTION_PLAY -> {
                // Si la música está habilitada, inicia la reproducción
                if (isMusicEnabled) {
                    startMusic() // Llama a la función para reproducir
                }
                return START_STICKY // Servicio se reinicia si se termina inesperadamente
            }

            ACTION_STOP -> {
                // Detiene la música y el servicio
                stopMusic() // Llama a la función para detener
                stopSelf() // Finaliza el servicio
                return START_NOT_STICKY // No reinicia el servicio
            }

            ACTION_SET_VOLUME -> {
                // Ajusta el volumen según el valor recibido
                val volumeLevel =
                    intent.getFloatExtra(EXTRA_VOLUME_LEVEL, 1.0f) // Obtiene el nivel de volumen
                setVolume(volumeLevel) // Aplica el nuevo volumen
                return START_STICKY // Mantiene el servicio activo
            }

            else -> {
                // Por defecto, inicia la música si está habilitada
                if (isMusicEnabled) {
                    startMusic() // Llama a la función para reproducir
                }
                return START_STICKY // Servicio se reinicia si es necesario
            }
        }
    }

    // Inicia la reproducción de la música seleccionada
    private fun startMusic() {
        try {
            mediaPlayer?.release() // Libera cualquier reproductor existente
            // Obtiene la canción seleccionada desde las preferencias
            val selectedSong = sharedPrefs.getString("selected_song", "allnight")
            // Selecciona el recurso de audio según la preferencia
            val resourceId = when (selectedSong) {
                "allnight" -> R.raw.allnight // Canción "allnight"
                "higher" -> R.raw.higher // Canción "higher"
                else -> R.raw.allnight // Por defecto, "allnight"
            }
            // Crea y configura el reproductor de medios
            mediaPlayer = MediaPlayer.create(this, resourceId).apply {
                // Configura un listener para manejar errores
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra") // Registra el error
                    stopSelf() // Detiene el servicio
                    true // Indica que el error fue manejado
                }
                isLooping = true // Repite la música en bucle
                setVolume(currentVolume, currentVolume) // Aplica el volumen actual
                start() // Inicia la reproducción
            }
            // Verifica si el reproductor se creó correctamente
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer") // Registra fallo
                stopSelf() // Detiene el servicio
            }
        } catch (e: Exception) {
            // Maneja cualquier error al inicializar el reproductor
            Log.e(TAG, "Error initializing MediaPlayer", e) // Registra el error
            stopSelf() // Detiene el servicio
        }
    }

    // Ajusta el volumen del reproductor
    private fun setVolume(volumeLevel: Float) {
        currentVolume = volumeLevel // Actualiza el volumen actual
        mediaPlayer?.setVolume(volumeLevel, volumeLevel) // Aplica el volumen al reproductor
    }

    // Detiene la reproducción de la música
    private fun stopMusic() {
        mediaPlayer?.let {
            // Si está reproduciendo, detiene la música
            if (it.isPlaying) {
                it.stop()
            }
            it.release() // Libera los recursos del reproductor
        }
        mediaPlayer = null // Limpia la referencia al reproductor
    }

    // Limpia recursos cuando el servicio se destruye
    override fun onDestroy() {
        stopMusic() // Detiene la música
        super.onDestroy() // Llama al metodo padre
    }

    // Este servicio no permite vinculación, por lo que retorna null
    override fun onBind(intent: Intent?): IBinder? = null
}