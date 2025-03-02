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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var etEditUsername: EditText
    private lateinit var btnUpdateUsername: Button
    private lateinit var btnChangeProfilePicture: Button
    private lateinit var btnLogout: Button

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        tvUsername = view.findViewById(R.id.tvUsername)
        etEditUsername = view.findViewById(R.id.etEditUsername)
        btnUpdateUsername = view.findViewById(R.id.btnUpdateUsername)
        btnChangeProfilePicture = view.findViewById(R.id.btnChangeProfilePicture)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadUserProfile()

        btnUpdateUsername.setOnClickListener { updateUsername() }
        btnChangeProfilePicture.setOnClickListener { pickImageFromGallery() }
        btnLogout.setOnClickListener { logout() }

        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: "Unknown"
                        tvUsername.text = username
                    }
                }

            val imageFile = File(requireContext().filesDir, "profile_picture.jpg")
            if (imageFile.exists()) {
                ivProfilePicture.setImageURI(Uri.fromFile(imageFile))
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun updateUsername() {
        val newUsername = etEditUsername.text.toString().trim()
        if (newUsername.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        user?.uid?.let { uid ->
            val userRef = db.collection("users").document(uid)

            // Store username in Firestore under "users" collection
            val userData = hashMapOf("username" to newUsername)
            userRef.set(userData) // If the document doesn't exist, it creates it
                .addOnSuccessListener {
                    tvUsername.text = newUsername
                    Toast.makeText(requireContext(), "Username updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating username: ${e.message}", Toast.LENGTH_SHORT).show()
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
            val imageUri = data.data
            ivProfilePicture.setImageURI(imageUri)
            saveImageLocally(imageUri)
        }
    }

    private fun saveImageLocally(imageUri: Uri?) {
        if (imageUri == null) return

        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
        val file = File(requireContext().filesDir, "profile_picture.jpg")

        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }

        Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        auth.signOut()
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }
}
