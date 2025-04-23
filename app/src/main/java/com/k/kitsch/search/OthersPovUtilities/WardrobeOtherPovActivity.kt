package com.k.kitsch.wardrobeotherpov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.k.kitsch.databinding.ActivityWardrobeOtherPovBinding
import com.k.kitsch.wardrobe.categoriesW.AccessoriesActivity
import com.k.kitsch.wardrobe.categoriesW.BottomsActivity
import com.k.kitsch.wardrobe.categoriesW.OuterwearActivity
import com.k.kitsch.wardrobe.categoriesW.ShoesActivity
import com.k.kitsch.wardrobe.categoriesW.SportswearActivity
import com.k.kitsch.wardrobe.categoriesW.TopsActivity

class WardrobeOtherPovActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWardrobeOtherPovBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            binding = ActivityWardrobeOtherPovBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Apply window insets
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val username = intent.getStringExtra("USERNAME") ?: ""

            // Initialize all wardrobe category buttons
            initButton(binding.topsButton, TopsActivity::class.java, username)
            initButton(binding.bottomsButton, BottomsActivity::class.java, username)
            initButton(binding.shoesButton, ShoesActivity::class.java, username)
            initButton(binding.accessoriesButton, AccessoriesActivity::class.java, username)
            initButton(binding.outerwearButton, OuterwearActivity::class.java, username)
            initButton(binding.sportswearButton, SportswearActivity::class.java, username)

            // Back button navigation
            binding.backToProfileOtherPOVBtn.setOnClickListener {
                finish()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading wardrobe: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initButton(button: View?, targetActivity: Class<*>, username: String) {
        button?.setOnClickListener {
            try {
                Intent(this, targetActivity).apply {
                    putExtra("USERNAME", username)
                    putExtra("VIEW_ONLY", true) // Add this flag to indicate view-only mode
                    startActivity(this)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Couldn't open ${targetActivity.simpleName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}