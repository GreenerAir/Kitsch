package com.k.kitsch.messages.chatModel

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clase AudioHelper que maneja la grabación y reproducción de audio en la aplicación
class AudioHelper(private val context: Context) {

    // Variables para gestionar el grabador, reproductor, archivo actual y estados
    private var mediaRecorder: MediaRecorder? = null // Grabador de audio, inicialmente nulo
    private var mediaPlayer: MediaPlayer? = null    // Reproductor de audio, inicialmente nulo
    private var currentAudioFile: File? = null      // Archivo donde se guarda el audio grabado
    private var isRecording = false                 // Indica si se está grabando
    private var isPlaying = false                   // Indica si se está reproduciendo
    private var playbackCallback: (() -> Unit)? =
        null // Callback para notificar cuando termina la reproducción

    // Función para establecer un callback que se ejecuta al finalizar la reproducción
    fun setPlaybackCallback(callback: () -> Unit) {
        playbackCallback = callback // Asigna el callback proporcionado
    }

    // Inicia la grabación de audio con un límite de tiempo (por defecto 50 segundos)
    fun startRecording(timeLimitInSeconds: Long = 50): File? {
        // Verifica si ya se está grabando para evitar conflictos
        if (isRecording) {
            Log.w("AudioHelper", "Already recording!") // Registra advertencia
            return null // Sale si ya está grabando
        }

        try {
            // Obtiene el directorio para almacenar archivos de música
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            // Crea el directorio si no existe
            if (storageDir?.exists() == false) storageDir.mkdirs()
            // Verifica que el directorio sea válido
            if (storageDir == null) {
                Log.e("AudioHelper", "Storage directory is null!") // Registra error
                return null // Sale si no hay directorio
            }

            // Genera un nombre único para el archivo basado en la fecha y hora
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            // Crea un nuevo archivo con extensión .m4a
            currentAudioFile = File(storageDir, "AUDIO_${timeStamp}.m4a").apply {
                if (!exists()) createNewFile() // Crea el archivo si no existe
            }

            // Configura el grabador de audio
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC) // Usa el micrófono como fuente
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Formato MPEG-4 para el archivo
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Codificador AAC para el audio
                setOutputFile(currentAudioFile?.absolutePath) // Define la ruta del archivo
                prepare() // Prepara el grabador
                start() // Inicia la grabación
                isRecording = true // Marca que está grabando
                Log.d(
                    "AudioHelper",
                    "Recording started: ${currentAudioFile?.absolutePath}"
                ) // Registra inicio
            }

            // Programa un temporizador para detener la grabación automáticamente
            Handler(Looper.getMainLooper()).postDelayed({
                if (isRecording) stopRecording() // Detiene la grabación si sigue activa
            }, timeLimitInSeconds * 1000) // Convierte segundos a milisegundos

            return currentAudioFile // Devuelve el archivo creado
        } catch (e: Exception) {
            // Maneja errores durante la configuración o inicio de la grabación
            Log.e("AudioHelper", "Failed to start recording: ${e.message}", e) // Registra el error
            mediaRecorder?.release() // Libera recursos del grabador
            mediaRecorder = null // Limpia la referencia
            return null // Devuelve nulo si falla
        }
    }

    // Detiene la grabación de audio
    fun stopRecording() {
        // Verifica si no se está grabando
        if (!isRecording) {
            Log.w("AudioHelper", "Not recording!") // Registra advertencia
            return // Sale si no hay grabación activa
        }

        try {
            // Detiene y libera el grabador
            mediaRecorder?.stop() // Para la grabación
            mediaRecorder?.release() // Libera recursos
            isRecording = false // Marca que no está grabando
            // Verifica el archivo grabado
            currentAudioFile?.let {
                Log.i(
                    "AudioHelper",
                    "Recording stopped, file size: ${it.length()} bytes, path: ${it.absolutePath}"
                ) // Registra detalles
                // Muestra mensaje si el archivo está vacío
                if (it.length() == 0L) {
                    Toast.makeText(context, "Recording failed: empty file", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (e: Exception) {
            // Maneja errores al detener la grabación
            Log.e("AudioHelper", "Failed to stop recording: ${e.message}", e) // Registra el error
            Toast.makeText(context, "Recording error", Toast.LENGTH_SHORT)
                .show() // Notifica al usuario
        } finally {
            mediaRecorder = null // Limpia el grabador en cualquier caso
        }
    }

    // Reproduce un archivo de audio
    fun playAudio(file: File) {
        // Detiene cualquier reproducción activa
        if (isPlaying) stopPlaying()

        // Verifica que el archivo exista y no esté vacío
        if (!file.exists() || file.length() == 0L) {
            Log.e(
                "AudioHelper",
                "File invalid: ${file.absolutePath}, exists=${file.exists()}, size=${file.length()}"
            ) // Registra error
            Toast.makeText(context, "Audio file missing or empty", Toast.LENGTH_SHORT)
                .show() // Notifica al usuario
            return // Sale si el archivo es inválido
        }

        Log.d("AudioHelper", "Attempting playback for: ${file.absolutePath}") // Registra intento
        try {
            // Libera cualquier reproductor anterior
            mediaPlayer?.release()
            // Configura un nuevo reproductor
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file)) // Establece el archivo a reproducir
                prepare() // Prepara el reproductor
                start() // Inicia la reproducción
                this@AudioHelper.isPlaying = true // Marca que está reproduciendo
                Log.d("AudioHelper", "Playback started: ${file.absolutePath}") // Registra inicio
                Toast.makeText(context, "Playing audio", Toast.LENGTH_SHORT)
                    .show() // Notifica al usuario
            }
            // Configura un listener para cuando termine la reproducción
            mediaPlayer?.setOnCompletionListener {
                Log.d(
                    "AudioHelper",
                    "Playback completed: ${file.absolutePath}"
                ) // Registra finalización
                stopPlaying() // Detiene la reproducción
                Toast.makeText(context, "Playback completed", Toast.LENGTH_SHORT)
                    .show() // Notifica al usuario
            }
            // Configura un listener para errores durante la reproducción
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("AudioHelper", "Playback error ($what, $extra)") // Registra el error
                stopPlaying() // Detiene la reproducción
                Toast.makeText(context, "Playback error ($what)", Toast.LENGTH_SHORT)
                    .show() // Notifica al usuario
                true // Indica que el error fue manejado
            }
        } catch (e: Exception) {
            // Maneja errores al intentar reproducir
            Log.e("AudioHelper", "Playback failed: ${e.message}", e) // Registra el error
            Toast.makeText(context, "Failed to play audio: ${e.message}", Toast.LENGTH_SHORT)
                .show() // Notifica al usuario
            stopPlaying() // Asegura que se detenga la reproducción
        }
    }

    // Detiene la reproducción de audio
    fun stopPlaying() {
        try {
            // Verifica si hay un reproductor activo
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop() // Detiene la reproducción si está activa
                player.release() // Libera recursos
            }
        } catch (e: Exception) {
            // Maneja errores al detener la reproducción
            Log.e("AudioHelper", "Error stopping playback: ${e.message}", e) // Registra el error
        } finally {
            mediaPlayer = null // Limpia el reproductor
            isPlaying = false // Marca que no está reproduciendo
            playbackCallback?.invoke() // Ejecuta el callback si existe
        }
    }

    // Limpia todos los recursos activos
    fun cleanup() {
        stopRecording() // Detiene cualquier grabación
        stopPlaying() // Detiene cualquier reproducción
        Log.i("AudioHelper", "Cleanup complete.") // Registra que la limpieza está completa
    }
}