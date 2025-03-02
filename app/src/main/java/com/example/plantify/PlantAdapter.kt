package com.example.plantify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.io.File

class PlantAdapter(private var plantList: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get logged-in user ID

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantName: TextView = itemView.findViewById(R.id.tvPlantName)
        val plantDescription: TextView = itemView.findViewById(R.id.tvPlantDescription)
        val plantPrice: TextView = itemView.findViewById(R.id.tvPlantPrice)
        val plantImage: ImageView = itemView.findViewById(R.id.ivPlantImage)
        val btnEditPlant: Button = itemView.findViewById(R.id.btnEditPlant)
        val btnDeletePlant: Button = itemView.findViewById(R.id.btnDeletePlant)
        val editDeleteLayout: LinearLayout = itemView.findViewById(R.id.editDeleteLayout)
        val tvPlantType: TextView = itemView.findViewById(R.id.tvPlantType)
        val tvCreatedBy: TextView = itemView.findViewById(R.id.tvCreatedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plantList[position]
        holder.plantName.text = plant.name
        holder.plantDescription.text = plant.description
        holder.plantPrice.text = "Price: ${plant.price} ILS"
        holder.tvPlantType.text = "Type: ${plant.type}"
        holder.tvCreatedBy.text = "Created by: ${plant.createdBy ?: "Unknown"}"


        if (!plant.imagePath.isNullOrEmpty()) {
            val file = File(plant.imagePath)
            if (file.exists()) {
                Picasso.get().load(file).into(holder.plantImage)
            } else {
                holder.plantImage.setImageResource(R.drawable.ic_plant_placeholder)
            }
        } else {
            holder.plantImage.setImageResource(R.drawable.ic_plant_placeholder)
        }

        // Show edit/delete buttons only for the creator
        if (plant.userId == currentUserId) {
            holder.editDeleteLayout.visibility = View.VISIBLE
        } else {
            holder.editDeleteLayout.visibility = View.GONE
        }

        // Delete plant from Firestore
        holder.btnDeletePlant.setOnClickListener {
            FirebaseFirestore.getInstance().collection("plants").document(plant.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(holder.itemView.context, "Plant deleted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Failed to delete!", Toast.LENGTH_SHORT).show()
                }
        }

        // Edit plant details
        holder.btnEditPlant.setOnClickListener {
            val bundle = Bundle().apply {
                putString("plantId", plant.id)
                putString("name", plant.name)
                putString("description", plant.description)
                putString("price", plant.price)
                putString("imagePath", plant.imagePath)
            }
            it.findNavController().navigate(R.id.action_plantListFragment_to_editPlantFragment, bundle)
        }
    }

    override fun getItemCount() = plantList.size

    // Function to update adapter data
    fun updatePlants(newPlants: List<Plant>) {
        plantList = newPlants
        notifyDataSetChanged()
    }
}
