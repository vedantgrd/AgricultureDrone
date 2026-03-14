package com.example.agridronee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUserName, editUserEmail, editUserPhone;
    private Button btnSaveProfile;
    private ImageView editProfilePicture;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        Log.d("EditProfileActivity", "onCreate called");

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        editUserName = findViewById(R.id.editUserName);
        editUserEmail = findViewById(R.id.editUserEmail);
        editUserPhone = findViewById(R.id.editUserPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        editProfilePicture = findViewById(R.id.editProfilePicture);

        // Load User Data
        loadUserProfile();

        // Check if we have current data passed from ProfileActivity
        if (getIntent().hasExtra("current_name")) {
            String currentName = getIntent().getStringExtra("current_name");
            editUserName.setText(currentName);
        }

        // Save Profile Button Click
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Error: User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");

                        // Only set if not empty and not already set by intent
                        if (name != null && !name.isEmpty() && !getIntent().hasExtra("current_name")) {
                            editUserName.setText(name);
                        }

                        if (email != null && !email.isEmpty()) {
                            editUserEmail.setText(email);
                        } else if (user.getEmail() != null) {
                            // Fallback to auth email if available
                            editUserEmail.setText(user.getEmail());
                        }

                        if (phone != null && !phone.isEmpty()) {
                            editUserPhone.setText(phone);
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                        // Fallback to auth email if available
                        if (user.getEmail() != null) {
                            editUserEmail.setText(user.getEmail());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EditProfileActivity", "❌ Error loading user data", e);
                    Toast.makeText(EditProfileActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Error: User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String name = editUserName.getText().toString().trim();
        String email = editUserEmail.getText().toString().trim();
        String phone = editUserPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firestore User Data
        Map<String, Object> updatedProfile = new HashMap<>();
        updatedProfile.put("name", name);
        updatedProfile.put("email", email);

        if (!phone.isEmpty()) {
            updatedProfile.put("phone", phone);
        }

        db.collection("users").document(userId)
                .set(updatedProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "✅ Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Return result to ProfileActivity instead of starting a new one
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_name", name);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> {
                    Log.e("EditProfileActivity", "❌ Error updating profile", e);
                    Toast.makeText(EditProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }
}