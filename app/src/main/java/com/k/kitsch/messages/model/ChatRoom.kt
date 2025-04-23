package com.k.kitsch.messages.model

data class ChatRoom(
    val id: String, // Identificador único del chat
    val chatName: String, // Nombre del chat ( si es solo un usuario, solo el nombre de ese usuario)
    val lastMessage: String, // Último mensaje enviado/recibido
    val imageChatRoom: Int // Imagen para el chat
)