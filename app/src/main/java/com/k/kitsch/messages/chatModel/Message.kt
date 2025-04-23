package com.k.kitsch.messages.chatModel

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(), // ID único generado para el mensaje
    val text: String? = null, // Texto del mensaje, nulo si es un mensaje de audio
    val audioPath: String? = null, // Ruta del archivo de audio, nulo si es un mensaje de texto
    val timestamp: Long = System.currentTimeMillis(), // Marca de tiempo del mensaje
    val senderId: String, // ID del usuario que envió el mensaje
    val receiverId: String, // ID del usuario que recibió el mensaje
    var isPlaying: Boolean = false // Indica si el audio está reproduciéndose
)