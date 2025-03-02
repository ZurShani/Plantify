package com.example.plantify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class PlantListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_plant_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewPlants)
        val fabAddPlant: FloatingActionButton = view.findViewById(R.id.fabAddPlant)
        fabAddPlant.setOnClickListener {
            findNavController().navigate(R.id.action_plantListFragment_to_addPlantFragment)
        }

        db = FirebaseFirestore.getInstance()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter and set it to RecyclerView
        plantAdapter = PlantAdapter(emptyList())
        recyclerView.adapter = plantAdapter

        loadPlants()

        return view
    }

    private fun loadPlants() {
        db.collection("plants").get().addOnSuccessListener { documents ->
            val plantList = documents.map { document ->
                val plant = document.toObject(Plant::class.java).apply {
                    createdBy = document.getString("createdBy") ?: "Unknown"
                }
                plant
            }

            plantAdapter.updatePlants(plantList)
        }.addOnFailureListener {
            Log.e("PlantListFragment", "Error loading plants", it)
        }
    }
}
