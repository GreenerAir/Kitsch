package com.k.kitsch.initiation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object AuthManager {
    private lateinit var sharedPref: SharedPreferences
    var currentUser: String? = null
        private set

    fun initialize(context: Context) {
        sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USER_EMAIL", null)
    }

    fun saveLoginState(email: String) {
        currentUser = email
        with(sharedPref.edit()) {
            putString("USER_EMAIL", email)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUser.toString())
                    .update("fcmToken", token)
                    .addOnFailureListener { e ->
                        Log.e("AuthManager", "Failed to save FCM token", e)
                    }
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPref.getBoolean("IS_LOGGED_IN", false) && currentUser != null

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUser.toString())
                    .update("fcmToken", token)
            }
        }
    }

    fun clearLogin() {
        currentUser = null
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }
}