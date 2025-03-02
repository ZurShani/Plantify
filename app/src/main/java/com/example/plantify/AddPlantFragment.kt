package com.example.plantify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class AddPlantFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etPlantName: EditText
    private lateinit var etPlantDescription: EditText
    private lateinit var etPlantPrice: EditText
    private lateinit var spPlantType: Spinner
    private lateinit var ivPlantImage: ImageView
    private lateinit var btnUploadImage: Button
    private lateinit var btnSubmitPlant: Button
    private lateinit var progressBar: ProgressBar

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private var localImagePath: String? = null
    private val plantTypes = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_plant, container, false)

        // Initialize UI elements
        db = FirebaseFirestore.getInstance()
        progressBar = view.findViewById(R.id.progressBar)
        etPlantName = view.findViewById(R.id.etPlantName)
        etPlantDescription = view.findViewById(R.id.etPlantDescription)
        etPlantPrice = view.findViewById(R.id.etPlantPrice)
        spPlantType = view.findViewById(R.id.spPlantType)
        ivPlantImage = view.findViewById(R.id.ivPlantImage)
        btnUploadImage = view.findViewById(R.id.btnUploadImage)
        btnSubmitPlant = view.findViewById(R.id.btnSubmitPlant)

        fetchPlantSpecies()

        btnUploadImage.setOnClickListener { pickImageFromGallery() }
        btnSubmitPlant.setOnClickListener { submitPlant() }

        return view
    }

    private fun fetchPlantSpecies() {
        val url = "https://perenual.com/api/species-list?key=sk-PA6d67c1e835952858891"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load plant types!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body()
                    if (responseBody != null) {
                        val responseData = responseBody.string()

                        Log.d("API_RESPONSE", responseData)

                        requireActivity().runOnUiThread {
                            try {
                                val jsonObject = JSONObject(responseData)
                                val jsonArray = jsonObject.getJSONArray("data")

                                plantTypes.clear()
                                for (i in 0 until jsonArray.length()) {
                                    val plant = jsonArray.getJSONObject(i)
                                    val name = plant.optString("common_name", "Unknown")
                                    if (name.isNotEmpty() && name != "Unknown") {
                                        plantTypes.add(name)
                                    }
                                }

                                Log.d("PLANT_TYPES", "Fetched: $plantTypes")

                                // Populate Spinner
                                val adapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    plantTypes
                                )
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                spPlantType.adapter = adapter
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Error parsing data: ${e.message}")
                                Toast.makeText(
                                    requireContext(),
                                    "Error parsing data!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Response body is null!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            ivPlantImage.setImageURI(imageUri)

            // Save image locally
            localImagePath = saveImageLocally(imageUri!!)
        }
    }

    private fun saveImageLocally(imageUri: Uri): String {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
        val file = File(requireContext().filesDir, "plant_${UUID.randomUUID()}.jpg")

        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }

        return file.absolutePath
    }

    private fun submitPlant() {
        val name = etPlantName.text.toString()
        val description = etPlantDescription.text.toString()
        val priceText = etPlantPrice.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val selectedType = spPlantType.selectedItem?.toString() ?: ""


        if (priceText.isEmpty() || !priceText.matches(Regex("\\d+"))) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid numeric price!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val price = priceText.toInt()

        if (name.isEmpty() || description.isEmpty() || localImagePath == null || userId == null) {
            Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Fetch Username Before Adding Plant
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Unknown"

                val newPlantRef = db.collection("plants").document()
                val plant = hashMapOf(
                    "id" to newPlantRef.id,
                    "name" to name,
                    "description" to description,
                    "price" to price.toString(),
                    "imagePath" to (localImagePath ?: ""),
                    "userId" to userId,
                    "type" to selectedType,
                    "createdBy" to username
                )

                newPlantRef.set(plant)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Plant added!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_addPlantFragment_to_plantListFragment)
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to add plant!", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to fetch user info!", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}