package com.example.plantify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditPlantFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var etEditPlantName: EditText
    private lateinit var etEditPlantDescription: EditText
    private lateinit var etEditPlantPrice: EditText
    private lateinit var ivEditPlantImage: ImageView
    private lateinit var btnChangePlantImage: Button
    private lateinit var btnSaveChanges: Button

    private var plantId: String? = null
    private var currentImageUrl: String? = null
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_plant, container, false)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        etEditPlantName = view.findViewById(R.id.etEditPlantName)
        etEditPlantDescription = view.findViewById(R.id.etEditPlantDescription)
        etEditPlantPrice = view.findViewById(R.id.etEditPlantPrice)
        ivEditPlantImage = view.findViewById(R.id.ivEditPlantImage)
        btnChangePlantImage = view.findViewById(R.id.btnChangePlantImage)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)

        plantId = arguments?.getString("plantId")

        loadPlantDetails()

        btnChangePlantImage.setOnClickListener { pickImageFromGallery() }
        btnSaveChanges.setOnClickListener { saveChanges() }

        return view
    }

    private fun loadPlantDetails() {
        plantId?.let { id ->
            db.collection("plants").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        etEditPlantName.setText(document.getString("name"))
                        etEditPlantDescription.setText(document.getString("description"))
                        etEditPlantPrice.setText(document.getString("price"))

                        val imagePath = document.getString("imagePath")
                        if (!imagePath.isNullOrEmpty()) {
                            Picasso.get().load(File(imagePath))
                                .into(ivEditPlantImage)
                        } else {
                            ivEditPlantImage.setImageResource(R.drawable.ic_plant_placeholder)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to load plant: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivEditPlantImage.setImageURI(selectedImageUri)
        }
    }

    private fun saveChanges() {
        val name = etEditPlantName.text.toString()
        val description = etEditPlantDescription.text.toString()
        val priceText = etEditPlantPrice.text.toString()


        if (priceText.isEmpty() || !priceText.matches(Regex("\\d+"))) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid numeric price!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val price = priceText.toInt()
        val imagePath = selectedImageUri?.let { saveImageLocally(it) } ?: currentImageUrl

        updatePlantDetails(name, description, price, imagePath)
    }


    private fun saveImageLocally(imageUri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        val file = File(requireContext().filesDir, "plant_${UUID.randomUUID()}.jpg")

        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }

        return file.absolutePath
    }


    private fun updatePlantDetails(
        name: String,
        description: String,
        price: Int,
        imagePath: String?
    ) {
        plantId?.let { id ->
            db.collection("plants").document(id).get()
                .addOnSuccessListener { document ->
                    val createdBy =
                        document.getString("createdBy") ?: "Unknown"

                    val updatedPlant = hashMapOf(
                        "name" to name,
                        "description" to description,
                        "price" to price.toString(),
                        "imagePath" to (imagePath ?: currentImageUrl ?: ""),
                        "createdBy" to createdBy //
                    )

                    db.collection("plants").document(id).update(updatedPlant as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Plant updated!", Toast.LENGTH_SHORT)
                                .show()
                            findNavController().navigate(R.id.action_editPlantFragment_to_plantListFragment)
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Failed to update plant!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
        }
    }
}

