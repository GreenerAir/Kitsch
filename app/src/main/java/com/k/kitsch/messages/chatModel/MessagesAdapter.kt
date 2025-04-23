package com.k.kitsch.messages.chatModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clase MessagesAdapter que maneja la lista de mensajes en un RecyclerView
class MessagesAdapter(
    private val messages: MutableList<Message>, // Lista mutable de mensajes para permitir actualizaciones
    private val currentUserId: String, // ID del usuario actual para distinguir mensajes enviados/recibidos
    private val onAudioClick: (Message, Boolean) -> Unit // Callback para manejar clics en mensajes de audio (reproducir/detener)
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    // Clase interna que representa la vista de cada mensaje en la lista
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a los elementos del layout para mensajes enviados y recibidos
        private val sentMessageLayout: View =
            itemView.findViewById(R.id.sentMessageLayout) // Contenedor para mensajes enviados
        private val receivedMessageLayout: View =
            itemView.findViewById(R.id.receivedMessageLayout) // Contenedor para mensajes recibidos
        private val tvSentMessage: TextView =
            itemView.findViewById(R.id.tvSentMessage) // Texto del mensaje enviado
        private val tvReceivedMessage: TextView =
            itemView.findViewById(R.id.tvReceivedMessage) // Texto del mensaje recibido
        private val tvSentTime: TextView =
            itemView.findViewById(R.id.tvSentTime) // Hora del mensaje enviado
        private val tvReceivedTime: TextView =
            itemView.findViewById(R.id.tvReceivedTime) // Hora del mensaje recibido
        private val sentAudioBtn: ImageButton =
            itemView.findViewById(R.id.sentAudioBtn) // Botón para audio enviado
        private val receivedAudioBtn: ImageButton =
            itemView.findViewById(R.id.receivedAudioBtn) // Botón para audio recibido

        // Función que vincula los datos de un mensaje con la vista
        fun bind(message: Message) {
            // Determina si el mensaje fue enviado por el usuario actual
            val isSent = message.senderId == currentUserId

            // Muestra el layout correcto según si el mensaje es enviado o recibido
            sentMessageLayout.visibility = if (isSent) View.VISIBLE else View.GONE
            receivedMessageLayout.visibility = if (!isSent) View.VISIBLE else View.GONE

            // Maneja mensajes de texto
            if (message.text != null) {
                if (isSent) {
                    tvSentMessage.text = message.text // Asigna el texto al mensaje enviado
                    tvSentMessage.visibility = View.VISIBLE // Muestra el texto
                    sentAudioBtn.visibility = View.GONE // Oculta el botón de audio
                } else {
                    tvReceivedMessage.text = message.text // Asigna el texto al mensaje recibido
                    tvReceivedMessage.visibility = View.VISIBLE // Muestra el texto
                    receivedAudioBtn.visibility = View.GONE // Oculta el botón de audio
                }
            }

            // Maneja mensajes de audio
            if (message.audioPath != null) {
                if (isSent) {
                    tvSentMessage.visibility = View.GONE // Oculta el texto
                    sentAudioBtn.visibility = View.VISIBLE // Muestra el botón de audio
                    // Cambia el ícono según si el audio está reproduciéndose
                    sentAudioBtn.setImageResource(
                        if (message.isPlaying) R.drawable.stop_icon else R.drawable.ic_play_audio
                    )
                    // Configura el listener para alternar reproducción/detención
                    sentAudioBtn.setOnClickListener {
                        onAudioClick(
                            message,
                            !message.isPlaying
                        ) // Llama al callback con el estado opuesto
                    }
                } else {
                    tvReceivedMessage.visibility = View.GONE // Oculta el texto
                    receivedAudioBtn.visibility = View.VISIBLE // Muestra el botón de audio
                    // Cambia el ícono según si el audio está reproduciéndose
                    receivedAudioBtn.setImageResource(
                        if (message.isPlaying) R.drawable.stop_icon else R.drawable.ic_play_audio
                    )
                    // Configura el listener para alternar reproducción/detención
                    receivedAudioBtn.setOnClickListener {
                        onAudioClick(
                            message,
                            !message.isPlaying
                        ) // Llama al callback con el estado opuesto
                    }
                }
            }

            // Muestra la hora del mensaje
            if (isSent) {
                tvSentTime.text =
                    formatTime(message.timestamp) // Formatea y asigna la hora para mensajes enviados
            } else {
                tvReceivedTime.text =
                    formatTime(message.timestamp) // Formatea y asigna la hora para mensajes recibidos
            }
        }

        // Formatea la marca de tiempo en un formato legible (hora:minutos AM/PM)
        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat(
                "h:mm a",
                Locale.getDefault()
            ).format(Date(timestamp)) // Devuelve la hora formateada
        }
    }

    // Crea una nueva vista para un mensaje
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Infla el layout definido para cada mensaje (item_message)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false) // Crea la vista desde el XML
        return MessageViewHolder(view) // Devuelve un nuevo ViewHolder con la vista
    }

    // Vincula los datos de un mensaje específico con su vista en la posición dada
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position]) // Llama a bind con el mensaje en la posición actual
    }

    // Devuelve el número total de mensajes en la lista
    override fun getItemCount(): Int = messages.size // Retorna el tamaño de messages
}