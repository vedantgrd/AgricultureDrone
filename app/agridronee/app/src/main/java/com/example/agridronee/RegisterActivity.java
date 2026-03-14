package com.example.agridronee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        emailEditText = findViewById(R.id.registerEmail);
        passwordEditText = findViewById(R.id.registerPassword);
        registerButton = findViewById(R.id.registerButton);

        // Register button action
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), email);
                        }
                    } else {
                        // Check if error is because user already exists
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // Email already exists, try to sign in instead
                            signInExistingUser(email, password);
                        } else {
                            Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInExistingUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user already has data in Firestore
                            checkUserDataExists(user.getUid(), email);
                        }
                    } else {
                        Toast.makeText(this, "This email is already registered but the password is incorrect",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // In RegisterActivity.java, modify the saveUserData method:
    private void saveUserData(String userId, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", email.split("@")[0]);
        userData.put("email", email);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User profile created successfully!", Toast.LENGTH_SHORT).show();
                    // Direct to LoginActivity instead of MainActivity
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                });
    }

    // Also update the checkUserDataExists method for consistency
    private void checkUserDataExists(String userId, String email) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // User exists in Authentication but not in Firestore, create profile
                        saveUserData(userId, email);
                    } else {
                        // User already has a profile, redirect to LoginActivity
                        Toast.makeText(this, "Account already exists. Please log in.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Error checking user data", e);
                    // Assume user doesn't exist in Firestore and create profile
                    saveUserData(userId, email);
                });
    }
}