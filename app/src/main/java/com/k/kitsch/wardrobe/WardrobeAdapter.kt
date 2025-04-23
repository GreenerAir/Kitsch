package com.k.kitsch.wardrobe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import java.io.ByteArrayOutputStream

class WardrobeAdapter(
    private val wardrobe: MutableList<WardrobeItem>,
    private val onItemDeleted: (Int) -> Unit,
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    inner class WardrobeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wardrobeImage: ImageView = itemView.findViewById(R.id.imageView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteWardrobeItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wardrobe, parent, false)
        return WardrobeViewHolder(view)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = wardrobe[position]

        // Set wardrobe image
        when {
            !item.wardrobeImageData.isNullOrEmpty() -> {
                try {
                    val bitmap = item.wardrobeImageData.base64ToBitmap()
                    holder.wardrobeImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.wardrobeImage.setImageResource(R.drawable.defaultpfpicon)
                    Log.e("WardrobeAdapter", "Error loading Base64 image", e)
                }
            }

            else -> {
                holder.wardrobeImage.setImageResource(R.drawable.defaultpfpicon)
            }
        }

        // Set delete button click listener
        holder.deleteButton.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                onItemDeleted(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount() = wardrobe.size

    fun removeItem(position: Int) {
        wardrobe.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateList(newList: List<WardrobeItem>) {
        wardrobe.clear()
        wardrobe.addAll(newList)
        notifyDataSetChanged()
    }

    private fun String.base64ToBitmap(): Bitmap {
        val decodedBytes = Base64.decode(this, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun Bitmap.bitmapToBase64(): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}