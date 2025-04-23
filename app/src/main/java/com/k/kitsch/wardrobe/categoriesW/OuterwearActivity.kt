package com.k.kitsch.wardrobe.categoriesW

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.k.kitsch.R
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.wardrobe.WardrobeAdapter
import com.k.kitsch.wardrobe.WardrobeItem

class OuterwearActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var WardrobeAdapter: WardrobeAdapter
    private val wardrobeItems = mutableListOf<WardrobeItem>()
    private val db = FirebaseFirestore.getInstance()
    val currentUser = AuthManager.currentUser
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishWithTransition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_outerwear)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        loadWardrobeItems()
    }

    override fun onResume() {
        super.onResume()
        loadWardrobeItems()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.outwearRecyclerView)
        findViewById<Button>(R.id.backToWardrobeButton).setOnClickListener {
            finishWithTransition()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        WardrobeAdapter = WardrobeAdapter(wardrobeItems) { position ->
            deleteItem(position)
        }
        recyclerView.adapter = WardrobeAdapter
    }

    private fun loadWardrobeItems() {
        wardrobeItems.clear()
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .collection("Wardrobe")
            .whereEqualTo("category", "Outerwear")
            .get()
            .addOnSuccessListener { documents ->
                processFirestoreDocuments(documents)
            }
            .addOnFailureListener { e ->
                Log.e("OuterwearActivity", "Error loading posts", e)
            }
    }

    private fun processFirestoreDocuments(documents: QuerySnapshot) {
        try {
            wardrobeItems.clear()
            documents.forEach { document ->
                wardrobeItems.add(
                    WardrobeItem(
                        idWardrobeItem = document.getString("idWardrobeItem").toString(),
                        wardrobeImageData = document.getString("wardrobeImageData"),
                        category = document.getString("category") ?: "",
                        timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                    )
                )
            }
            WardrobeAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("OuterwearActivity", "Error processing documents", e)
        }
    }

    private fun deleteItem(position: Int) {
        if (position !in wardrobeItems.indices) return

        val item = wardrobeItems[position]

        db.collection("Users")
            .document(currentUser.toString())
            .collection("Wardrobe")
            .document(item.idWardrobeItem)
            .delete()
            .addOnSuccessListener {
                db.collection("Users")
                    .document(currentUser.toString())
                    .update("wardrobeItemCount", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        showToast("Item deleted")
                        wardrobeItems.removeAt(position)
                        WardrobeAdapter.notifyItemRemoved(position)
                    }
                    .addOnFailureListener { e ->
                        Log.e("OuterwearActivity", "Failed to update count", e)
                        showToast("Item deleted but count not updated")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("OuterwearActivity", "Error deleting item", e)
                showToast("Failed to delete item")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun finishWithTransition() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}