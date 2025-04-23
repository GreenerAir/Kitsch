package com.k.kitsch.wardrobe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivityUploadWardrobeBinding
import com.k.kitsch.initiation.AuthManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class UploadWardrobeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadWardrobeBinding
    private var selectedImageUri: Uri? = null
    private var cameraBitmap: Bitmap? = null
    private val db = FirebaseFirestore.getInstance()
    private val photoStorageHelper = PhotoStorageHelper(this)
    val currentUser = AuthManager.currentUser

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    binding.imageView.setImageURI(it)
                    // Clear camera bitmap when selecting from gallery
                    cameraBitmap = null
                }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                cameraBitmap?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)
                    // Clear URI when taking new photo
                    selectedImageUri = null
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadWardrobeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorySpinner()

        binding.openCameraButton.setOnClickListener { openCamera() }
        binding.backToWardrobeButton.setOnClickListener { finish() }
        binding.openGalleryButton.setOnClickListener { openGallery() }
        binding.postButton.setOnClickListener { uploadWardrobe() }
    }

    private fun setupCategorySpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.clothing_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = adapter
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            takePictureLauncher.launch(takePictureIntent)
        } ?: run {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadWardrobe() {
        if (selectedImageUri == null && cameraBitmap == null) {
            showError("Please select an image or take a photo")
            return
        }

        val category = binding.categorySpinner.selectedItem.toString()
        if (category.isEmpty()) {
            showError("Please select a category")
            return
        }

        val base64Image = if (cameraBitmap != null) {
            // Use the camera bitmap if available
            photoStorageHelper.bitmapToBase64(cameraBitmap!!)
        } else {
            // Otherwise use the selected URI
            selectedImageUri?.let { uri ->
                photoStorageHelper.uriToBase64(uri)
            }
        }

        base64Image?.let { imageData ->
            val wardrobeItemId = UUID.randomUUID().toString()

            val wardrobeItem = WardrobeItem(
                idWardrobeItem = wardrobeItemId,
                wardrobeImageData = imageData,
                category = category,
                timestamp = System.currentTimeMillis()
            )

            // Determine aura points based on category
            val auraPoints = when (category) {
                "Accessories" -> 100
                "Bottoms" -> 200
                "Outerwear" -> 140
                "Shoes" -> 75
                "Sportswear" -> 150
                "Tops" -> 250
                else -> 0 // default
            }

            // Add to the user's wardrobe subcollection
            db.collection("Users")
                .document(currentUser.toString())
                .collection("Wardrobe")
                .document(wardrobeItemId)
                .set(wardrobeItem)
                .addOnSuccessListener { documentReference ->

                    // Update both wardrobe count and aura points
                    val updates = mapOf(
                        "wardrobeItemCount" to FieldValue.increment(1),
                        "auraPoints" to FieldValue.increment(auraPoints.toLong())
                    )

                    db.collection("Users").document(currentUser.toString())
                        .update(updates)
                        .addOnSuccessListener {
                            showToast("Wardrobe item added successfully (+$auraPoints aura points)")
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Failed to update counts: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    showError("Failed to add wardrobe item: ${e.message}")
                }
        } ?: showError("Error processing image")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    inner class PhotoStorageHelper(private val context: Context) {
        fun uriToBase64(uri: Uri): String? {
            return try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                bitmapToBase64(bitmap)
            } catch (e: IOException) {
                Log.e("PhotoStorage", "Error converting Uri to Base64", e)
                null
            }
        }

        fun bitmapToBase64(bitmap: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }
}