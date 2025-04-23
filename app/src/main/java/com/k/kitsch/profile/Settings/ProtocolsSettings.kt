package com.k.kitsch.profile.Settings

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.databinding.ActivityProtocolsBinding


class ProtocolsSettings : AppCompatActivity() {

    private lateinit var binding: ActivityProtocolsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProtocolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Going back to the settings menu
        binding.backToProfileButton.setOnClickListener {
            finish()
        }

        Prottocols()
    }

    // Setting up the text about the different protocols

    private fun Prottocols() {
        FirebaseFirestore.getInstance()
            .collection("AppProtocols")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Get the first document (assuming there's only one)
                    val document = querySnapshot.documents[0]

                    binding.accessibilityProtocol.text =
                        document.getString("accessibilityProtocol") ?: "a"
                    binding.displayProtocol.text = document.getString("displayProtocol") ?: "b"
                    binding.securityProtocol.text = document.getString("securityProtocol") ?: "c"

                    Log.d("ProtocolsSettings", "Successfully loaded protocol: ${document.data}")
                } else {
                    Log.e("ProtocolsSettings", "No documents found in AppProtocols")
                    setDefaultTexts()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProtocolsSettings", "Error loading protocols", e)
                setDefaultTexts()
            }
    }

    private fun setDefaultTexts() {
        binding.accessibilityProtocol.text = "Unable to load accessibility protocol"
        binding.displayProtocol.text = "Unable to load display protocol"
        binding.securityProtocol.text = "Unable to load security protocol"
    }

}
