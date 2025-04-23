package com.k.kitsch.messages.chatModel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivityChatBinding
import java.io.File

// Clase ChatActivity que maneja la interfaz y lógica de un chat con mensajes de texto y audio
class ChatActivity : AppCompatActivity() {
    // Variables para la vinculación de vistas, adaptador, lista de mensajes y estado
    private lateinit var binding: ActivityChatBinding // Vinculación con el layout de la actividad
    private lateinit var adapter: MessagesAdapter // Adaptador para mostrar mensajes en la lista
    private val messageList = mutableListOf<Message>() // Lista de mensajes (texto o audio)
    private var currentUserId = "user1" // ID del usuario actual, fijo para este ejemplo
    private var otherUserId: String? = null // ID del otro usuario en el chat
    private lateinit var audioHelper: AudioHelper // Instancia para manejar grabación/reproducción de audio
    private var isRecording = false // Indica si se está grabando audio
    private var currentAudioFile: File? = null // Archivo del audio que se está grabando

    // Objeto companion para definir constantes usadas en la actividad
    companion object {
        const val EXTRA_USER_ID = "extra_user_id" // Clave para el ID del otro usuario
        const val EXTRA_USER_NAME = "extra_user_name" // Clave para el nombre del otro usuario
        private const val REQUEST_PERMISSIONS_CODE = 200 // Código para solicitudes de permisos
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater) // Infla el layout del chat
        setContentView(binding.root) // Establece el layout como contenido de la actividad

        audioHelper = AudioHelper(this) // Inicializa el ayudante de audio con el contexto actual
        Log.d("ChatActivity", "AudioHelper initialized") // Registra que AudioHelper está listo

        requestRequiredPermissions() // Solicita permisos necesarios (micrófono)

        // Obtiene el nombre y ID del otro usuario desde el Intent, con valores por defecto
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Chat User"
        otherUserId = intent.getStringExtra(EXTRA_USER_ID) ?: "user2"
        binding.tvUserName.text = userName // Muestra el nombre del otro usuario en la UI
        // Configura el botón de retroceso para cerrar la actividad
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Inicializa el adaptador para la lista de mensajes
        adapter = MessagesAdapter(messageList, currentUserId) { message, shouldPlay ->
            // Callback ejecutado al hacer clic en un mensaje de audio
            Log.d(
                "ChatActivity",
                "Audio clicked: ${message.audioPath}, shouldPlay=$shouldPlay"
            ) // Registra la acción
            message.audioPath?.let { path ->
                val audioFile = File(path) // Crea un objeto File con la ruta del audio
                // Verifica que el archivo exista y no esté vacío
                if (audioFile.exists() && audioFile.length() > 0) {
                    Log.d(
                        "ChatActivity",
                        "Attempting to ${if (shouldPlay) "play" else "stop"} audio: ${audioFile.absolutePath}"
                    ) // Registra la intención
                    if (shouldPlay) {
                        audioHelper.playAudio(audioFile) // Reproduce el audio
                        message.isPlaying = true // Marca el mensaje como reproduciendo
                    } else {
                        audioHelper.stopPlaying() // Detiene la reproducción
                        message.isPlaying = false // Marca el mensaje como detenido
                    }
                    // Actualiza la lista para reflejar el cambio en el estado
                    adapter.notifyItemChanged(messageList.indexOf(message))
                } else {
                    // Maneja el caso de archivo inválido
                    Log.e(
                        "ChatActivity",
                        "Audio file invalid: ${audioFile.absolutePath}, exists=${audioFile.exists()}, size=${audioFile.length()}"
                    ) // Registra error
                    Toast.makeText(this, "Audio file not found or empty", Toast.LENGTH_SHORT)
                        .show() // Notifica al usuario
                }
            } ?: Log.w("ChatActivity", "Audio path is null") // Registra si no hay ruta de audio
        }

        // Configura el RecyclerView para mostrar mensajes
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity) // Usa un layout lineal
            adapter = this@ChatActivity.adapter // Asigna el adaptador
        }

        // Configura el botón de enviar para mandar mensajes de texto
        binding.btnSend.setOnClickListener { sendMessage() }

        // Configura el botón de audio para grabar o detener grabación
        binding.audioBtn.setOnClickListener {
            if (isRecording) stopAudioRecording() else startAudioRecording() // Alterna entre grabar y detener
        }

        loadSampleMessages() // Carga mensajes de ejemplo al iniciar
    }

    // Solicita permisos necesarios para la grabación de audio
    private fun requestRequiredPermissions() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO) // Permiso de micrófono
        // Filtra los permisos que aún no están otorgados
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        // Si hay permisos por solicitar, los pide
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(
                "ChatActivity",
                "Requesting permissions: ${permissionsToRequest.joinToString()}"
            ) // Registra solicitud
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS_CODE)
        } else {
            Log.d(
                "ChatActivity",
                "All required permissions already granted"
            ) // Registra que no se necesitan permisos
        }
    }

    // Inicia la grabación de audio
    private fun startAudioRecording() {
        // Verifica si se tiene el permiso de grabación
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ChatActivity", "Record permission missing") // Registra falta de permiso
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT)
                .show() // Notifica al usuario
            requestRequiredPermissions() // Solicita permisos nuevamente
            return
        }

        // Inicia la grabación con un límite de 50 segundos
        currentAudioFile = audioHelper.startRecording(50)
        if (currentAudioFile != null) {
            isRecording = true // Marca que está grabando
            binding.audioBtn.setImageResource(R.drawable.stop_icon) // Cambia el ícono a "detener"
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT)
                .show() // Notifica que está grabando
            binding.btnSend.isEnabled = false // Desactiva el botón de enviar
            Log.d(
                "ChatActivity",
                "Recording started, file: ${currentAudioFile?.absolutePath}"
            ) // Registra inicio
        } else {
            Log.e("ChatActivity", "Failed to start recording") // Registra error
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT)
                .show() // Notifica al usuario
        }
    }

    // Detiene la grabación de audio
    private fun stopAudioRecording() {
        audioHelper.stopRecording() // Detiene la grabación usando AudioHelper
        isRecording = false // Marca que no está grabando
        binding.audioBtn.setImageResource(R.drawable.mic_icon) // Restaura el ícono de micrófono
        binding.btnSend.isEnabled = true // Reactiva el botón de enviar
        Log.d("ChatActivity", "Recording stopped") // Registra que la grabación terminó

        // Procesa el archivo grabado
        currentAudioFile?.let { audioFile ->
            // Verifica que el archivo exista y no esté vacío
            if (audioFile.exists() && audioFile.length() > 0) {
                // Crea un mensaje de audio para la lista
                val audioMessage = Message(
                    text = null, // Sin texto, es un mensaje de audio
                    audioPath = audioFile.absolutePath, // Ruta del archivo
                    senderId = currentUserId, // Emisor es el usuario actual
                    receiverId = otherUserId ?: "user2" // Receptor es el otro usuario
                )
                messageList.add(audioMessage) // Añade el mensaje a la lista
                adapter.notifyItemInserted(messageList.size - 1) // Notifica al adaptador
                binding.rvMessages.scrollToPosition(messageList.size - 1) // Desplaza la lista al final
                simulateAudioReply(audioFile.absolutePath) // Simula una respuesta de audio
                Log.d(
                    "ChatActivity",
                    "Audio message added: ${audioFile.absolutePath}"
                ) // Registra el mensaje
            } else {
                // Maneja el caso de archivo inválido
                Log.e(
                    "ChatActivity",
                    "Recorded file invalid: ${audioFile.absolutePath}, size=${audioFile.length()}"
                ) // Registra error
                Toast.makeText(this, "Recording failed: file empty", Toast.LENGTH_SHORT)
                    .show() // Notifica al usuario
            }
            currentAudioFile = null // Limpia la referencia al archivo
        }
    }

    // Simula una respuesta de audio del otro usuario
    private fun simulateAudioReply(audioPath: String) {
        // Ejecuta la simulación después de 1.5 segundos
        binding.rvMessages.postDelayed({
            // Crea un mensaje de audio como respuesta
            val replyMessage = Message(
                text = null, // Sin texto, es audio
                audioPath = audioPath, // Usa la misma ruta de audio
                senderId = otherUserId ?: "user2", // Emisor es el otro usuario
                receiverId = currentUserId // Receptor es el usuario actual
            )
            messageList.add(replyMessage) // Añade la respuesta a la lista
            adapter.notifyItemInserted(messageList.size - 1) // Notifica al adaptador
            binding.rvMessages.scrollToPosition(messageList.size - 1) // Desplaza la lista al final
            Log.d("ChatActivity", "Simulated audio reply added") // Registra la acción
        }, 1500)
    }

    // Envía un mensaje de texto
    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim() // Obtiene el texto ingresado
        if (messageText.isEmpty()) return // Sale si el texto está vacío

        // Crea un nuevo mensaje de texto
        val newMessage = Message(
            text = messageText, // Texto del mensaje
            senderId = currentUserId, // Emisor es el usuario actual
            receiverId = otherUserId ?: "user2" // Receptor es el otro usuario
        )

        messageList.add(newMessage) // Añade el mensaje a la lista
        adapter.notifyItemInserted(messageList.size - 1) // Notifica al adaptador
        binding.rvMessages.scrollToPosition(messageList.size - 1) // Desplaza la lista al final
        binding.etMessage.text.clear() // Limpia el campo de texto

        simulateReply(messageText) // Simula una respuesta al mensaje
    }

    // Simula una respuesta de texto del otro usuario
    private fun simulateReply(originalMessage: String) {
        // Ejecuta la simulación después de 1 segundo
        binding.rvMessages.postDelayed({
            // Crea un mensaje de texto como respuesta
            val replyMessage = Message(
                text = "Reply to: $originalMessage", // Responde al mensaje original
                senderId = otherUserId ?: "user2", // Emisor es el otro usuario
                receiverId = currentUserId // Receptor es el usuario actual
            )
            messageList.add(replyMessage) // Añade la respuesta a la lista
            adapter.notifyItemInserted(messageList.size - 1) // Notifica al adaptador
            binding.rvMessages.scrollToPosition(messageList.size - 1) // Desplaza la lista al final
        }, 1000)
    }

    // Carga mensajes de ejemplo al iniciar el chat
    private fun loadSampleMessages() {
        // Lista de mensajes iniciales para simular una conversación
        val sampleMessages = listOf(
            Message(
                text = "Hello there!", // Mensaje del otro usuario
                senderId = otherUserId ?: "user2",
                receiverId = currentUserId
            ),
            Message(
                text = "Hi! How are you?", // Mensaje del usuario actual
                senderId = currentUserId,
                receiverId = otherUserId ?: "user2"
            )
        )

        messageList.addAll(sampleMessages) // Añade los mensajes a la lista
        adapter.notifyDataSetChanged() // Actualiza el adaptador
        binding.rvMessages.scrollToPosition(messageList.size - 1) // Desplaza al último mensaje
    }

    // Maneja el resultado de las solicitudes de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // Verifica si todos los permisos fueron otorgados
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("ChatActivity", "All permissions granted") // Registra éxito
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT)
                    .show() // Notifica al usuario
            } else {
                // Maneja permisos denegados
                val deniedPermissions =
                    permissions.filterIndexed { i, _ -> grantResults[i] != PackageManager.PERMISSION_GRANTED }
                Log.e(
                    "ChatActivity",
                    "Permissions denied: ${deniedPermissions.joinToString()}"
                ) // Registra error
                Toast.makeText(
                    this,
                    "Permissions denied: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show() // Notifica al usuario
            }
        }
    }

    // Limpia recursos cuando la actividad se destruye
    override fun onDestroy() {
        super.onDestroy()
        audioHelper.cleanup() // Libera recursos de AudioHelper
        Log.d("ChatActivity", "Activity destroyed, resources cleaned up") // Registra limpieza
    }
}