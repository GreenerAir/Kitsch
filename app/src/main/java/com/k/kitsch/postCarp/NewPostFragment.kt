package com.k.kitsch.postCarp

import NotificationHelper
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.FragmentNewPostBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.mainFragments.HomeFragment
import com.k.kitsch.notifications.model.NotificationType
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!
    var selectedImageUri: Uri? = null
    lateinit var captionText: String
    private lateinit var photoStorageHelper: PhotoStorageHelper
    val currentUser = AuthManager.currentUser

    // Activity result launcher for opening the gallery
    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    binding.imageView.setImageURI(it)
                    binding.discardButton.visibility = View.VISIBLE
                }
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        photoStorageHelper = PhotoStorageHelper(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        binding.openGalleryButton.setOnClickListener {
            openGallery()
        }

        binding.discardButton.setOnClickListener {
            discardImage()
        }

        binding.postButton.setOnClickListener {
            lifecycleScope.launch {
                postImageAndNavigate()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        openGalleryLauncher.launch(intent)
    }

    private fun discardImage() {
        binding.imageView.setImageDrawable(null)
        binding.discardButton.visibility = View.GONE
        selectedImageUri = null
    }

    fun getUserData(callback: (userId: String?, pfpIconId: Int, isVerified: Boolean) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(
                        document.getString("id"),
                        (document.get("pfpIconId") as? Long)?.toInt() ?: 0,
                        document.getBoolean("isVerified") == true,
                    )
                } else {
                    Log.e("getUserData", "User document not found")
                    callback(null, 0, false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("getUserData", "Error fetching user data", e)
                callback(null, 0, false)
            }
    }

    // First, make sure your postImageAndNavigate function is a suspend function
    private suspend fun postImageAndNavigate() {
        captionText = binding.captionEditText.text.toString()

        if (selectedImageUri != null && captionText.isNotEmpty()) {
            // Get user data FIRST
            getUserData { userId, pfpIconId, isVerified ->
                // Convert image to Base64 ONCE
                photoStorageHelper.uriToBase64(selectedImageUri!!)?.let { base64Image ->
                    // Create complete post data
                    val postItem = PostItem(
                        postId = UUID.randomUUID().toString(),
                        profileImage = pfpIconId,
                        username = userId ?: "Unknown",
                        isVerified = isVerified,
                        postImageData = base64Image,
                        likesCount = 0,
                        caption = captionText,
                        likedBy = emptyList(),
                    )

                    FirebaseFirestore.getInstance().collection("Posts").document(postItem.postId)
                        .set(postItem)
                        .addOnSuccessListener {
                            FirebaseFirestore.getInstance()
                                .collection("Users").document(currentUser.toString())
                                .update("postCounter", FieldValue.increment(1))

                            FirebaseFirestore.getInstance().collection("Users")
                                .document(currentUser.toString()).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val list = document.get("followers") as? List<String>
                                            ?: emptyList()
                                        val username = document.getString("id") ?: "Unknown"
                                        val profileIndex =
                                            (document.getLong("pfpIconId") ?: 0L).toInt()

                                        lifecycleScope.launch {
                                            sendNotification(
                                                postItem.postId,
                                                list,
                                                username,
                                                profileIndex
                                            )
                                        }
                                    }
                                }

                            FirebaseFirestore.getInstance()
                                .collection("Users").document(currentUser.toString())
                                .update("auraPoints", FieldValue.increment(450))
                            navigateToHome()
                        }
                        .addOnFailureListener { e ->
                            showError("Failed to upload post: ${e.message}")
                        }
                } ?: showError("Failed to process image")
            }
        } else {
            showError("Please select an image and write a caption")
        }
    }

    private fun navigateToHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private suspend fun sendNotification(
        postId: String,
        followers: List<String>,
        username: String,
        profileIndex: Int
    ) {
        try {
            if (followers.isNotEmpty()) {
                NotificationHelper.createAndSendNotification(
                    idItem = postId,
                    recipientIds = followers,  // Changed from recipientId to recipientIds
                    type = NotificationType.new_post,
                    profileImage = profileIndex,
                    creatorUsername = username,
                    notificationText = "$username made a new post"
                )
            }
        } catch (e: Exception) {
            Log.e("Notification", "Failed to send notification", e)
            // You might want to retry or log this to analytics
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }

    inner class PhotoStorageHelper(private val context: Context) {
        private val db = FirebaseFirestore.getInstance()

        fun uriToBase64(uri: Uri): String? {
            return try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                bitmapToBase64(bitmap)
            } catch (e: IOException) {
                Log.e("PhotoStorage", "Error converting Uri to Base64", e)
                null
            }
        }

        private fun bitmapToBase64(bitmap: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

        fun storePhoto(
            userId: String, photoName: String, imageUri: Uri,
            onSuccess: () -> Unit, onFailure: (Exception) -> Unit
        ) {
            val base64Image = uriToBase64(imageUri) ?: run {
                onFailure(IOException("Could not convert image to Base64"))
                return
            }

            val photoData = hashMapOf(
                "userId" to userId,
                "name" to photoName,
                "imageData" to base64Image,
            )

            db.collection("userPhotos")
                .add(photoData)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        }
    }
}