package com.k.kitsch.wardrobe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.k.kitsch.R
import com.k.kitsch.wardrobe.categoriesW.AccessoriesActivity
import com.k.kitsch.wardrobe.categoriesW.BottomsActivity
import com.k.kitsch.wardrobe.categoriesW.OuterwearActivity
import com.k.kitsch.wardrobe.categoriesW.ShoesActivity
import com.k.kitsch.wardrobe.categoriesW.SportswearActivity
import com.k.kitsch.wardrobe.categoriesW.TopsActivity

class WardrobeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wardrobe, container, false)

        // Find the uploadClothesButton
        val uploadClothesButton = view.findViewById<Button>(R.id.uploadClothesButton)
        // Find the ImageButtons for each section
        val topsButton = view.findViewById<ImageButton>(R.id.topsButton)
        val bottomsButton = view.findViewById<ImageButton>(R.id.bottomsButton)
        val shoesButton = view.findViewById<ImageButton>(R.id.shoesButton)
        val accessoriesButton = view.findViewById<ImageButton>(R.id.accessoriesButton)
        val outerwearButton = view.findViewById<ImageButton>(R.id.outerwearButton)
        val sportswearButton = view.findViewById<ImageButton>(R.id.sportswearButton)

        // Set an OnClickListener for uploadClothesButton to navigate to UploadWardrobeActivity
        uploadClothesButton.setOnClickListener {
            val intent = Intent(requireContext(), UploadWardrobeActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Tops button to navigate to TopsActivity
        topsButton.setOnClickListener {
            val intent = Intent(requireContext(), TopsActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Bottoms button to navigate to BottomsActivity
        bottomsButton.setOnClickListener {
            val intent = Intent(requireContext(), BottomsActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Shoes button to navigate to ShoesActivity
        shoesButton.setOnClickListener {
            val intent = Intent(requireContext(), ShoesActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Accessories button to navigate to AccessoriesActivity
        accessoriesButton.setOnClickListener {
            val intent = Intent(requireContext(), AccessoriesActivity::class.java)
            startActivity(intent)
        }

        outerwearButton.setOnClickListener {
            val intent = Intent(requireContext(), OuterwearActivity::class.java)
            startActivity(intent)
        }

        sportswearButton.setOnClickListener {
            val intent = Intent(requireContext(), SportswearActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
