package com.example.agridronee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // UI Components
    private TextView userName, userEmail, versionNumber;
    private Button btnEditProfile, btnLogout;
    private ImageButton btnBack, btnEditPicture;
    private CircleImageView profilePicture;

    // Firebase Components
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestoreDb;
    private DatabaseReference realtimeDb;
    private DocumentReference userDocRef;

    // State variables
    private boolean isLoadingProfile = false;
    private String currentName = null;
    private long lastUpdateTime = 0;
    private String userId;

    // Activity Result Launcher for Edit Profile
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("updated_name")) {
                        String updatedName = data.getStringExtra("updated_name");
                        Log.d(TAG, "Received updated name from result: " + updatedName);
                        updateUserName(updatedName);

                        // Update Firestore with the new name
                        if (userId != null) {
                            updateUserNameInDatabase(userId, updatedName);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate called");

        // Initialize Firebase
        initializeFirebase();

        // Initialize UI Components
        initializeUI();

        // Set click listeners
        setupClickListeners();

        // Set app version
        versionNumber.setText(getAppVersion());
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestoreDb = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference("users");

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            // User not logged in, redirect to login
            redirectToLogin();
        }
    }

    private void initializeUI() {
        // Profile info
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        profilePicture = findViewById(R.id.profilePicture);

        // Buttons
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        btnEditPicture = findViewById(R.id.btnEditPicture);

        // App info
        versionNumber = findViewById(R.id.versionNumber);
    }

    private void setupClickListeners() {
        // Edit Profile button
        btnEditProfile.setOnClickListener(v -> {
            Log.d(TAG, "Edit Profile button clicked");
            Intent editIntent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            // Add current name to intent
            if (currentName != null && !currentName.equals("User Profile Not Set")) {
                editIntent.putExtra("current_name", currentName);
            }
            editProfileLauncher.launch(editIntent);
        });

        // Logout button
        btnLogout.setOnClickListener(v -> logoutUser());

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Edit Picture button
        btnEditPicture.setOnClickListener(v -> openImageSelector());

        // Setup other clickable items
        setupNavigationListeners();
    }

    private void setupNavigationListeners() {
        // Account Settings Items
        findViewById(R.id.changePassword).setOnClickListener(v -> showChangePasswordDialog());

        findViewById(R.id.notificationSettings).setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "Notifications settings will be available soon ", Toast.LENGTH_SHORT).show();
        });


        // Drone Information Items
        findViewById(R.id.droneSettings).setOnClickListener(v ->{
            Toast.makeText(ProfileActivity.this, "Drone Settings will be available soon ", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.flightHistory).setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "Flight History will be available soon ", Toast.LENGTH_SHORT).show();
        });

        // Support Items
        findViewById(R.id.helpCenter).setOnClickListener(v -> openHtmlPage("faq.html", "Help Center / FAQs"));
        findViewById(R.id.contactSupport).setOnClickListener(v -> openHtmlPage("contact_support.html", "Contact Support"));

        // App Info Items
        findViewById(R.id.privacyPolicy).setOnClickListener(v -> openHtmlPage("privacy_policy.html", "Privacy Policy"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        if (!isLoadingProfile) {
            loadUserProfile();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        // Firestore listeners are managed automatically
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Error: User not logged in!", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        Log.d(TAG, "loadUserProfile started");
        isLoadingProfile = true;
        userId = user.getUid();
        userEmail.setText(user.getEmail());

        // Use real-time listener for Firestore
        userDocRef = firestoreDb.collection("users").document(userId);
        userDocRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "❌ Error loading user data from Firestore", e);
                checkRealtimeDatabase(userId);
                isLoadingProfile = false;
                return;
            }

            Log.d(TAG, "Firestore snapshot received");
            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Try to get the name field, fallback to username field if name doesn't exist
                String name = documentSnapshot.getString("name");
                if (name == null) {
                    name = documentSnapshot.getString("username");
                }
                updateUserName(name);

                // Check if profile picture URL exists
                String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    // Load profile picture using a library like Glide or Picasso
                    // Example with Glide:
                    // Glide.with(this).load(profilePicUrl).into(profilePicture);
                }
            } else {
                Log.d(TAG, "Firestore document does not exist, checking Realtime DB");
                checkRealtimeDatabase(userId);
            }
            isLoadingProfile = false;
        });
    }

    private void checkRealtimeDatabase(String userId) {
        realtimeDb.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Realtime DB query succeeded");
                if (dataSnapshot.exists()) {
                    String username = dataSnapshot.child("name").getValue(String.class);
                    updateUserName(username);

                    // Check if profile picture URL exists
                    String profilePicUrl = dataSnapshot.child("profilePicUrl").getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        // Load profile picture using a library like Glide or Picasso
                        // Example with Glide:
                        // Glide.with(ProfileActivity.this).load(profilePicUrl).into(profilePicture);
                    }

                    // Migrate user data to Firestore for future use
                    migrateUserToFirestore(userId, dataSnapshot);
                } else {
                    Log.d(TAG, "No data in Realtime DB");
                    updateUserName(null);
                    Toast.makeText(ProfileActivity.this, "User profile not found in database", Toast.LENGTH_SHORT).show();
                }
                isLoadingProfile = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading user data from Realtime DB", databaseError.toException());
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                isLoadingProfile = false;
            }
        });
    }

    private void updateUserName(String name) {
        long currentTime = System.currentTimeMillis();
        // Prevent rapid duplicate updates
        if (currentTime - lastUpdateTime < 2000) {
            Log.d(TAG, "Ignoring rapid duplicate update: " + name);
            return;
        }

        if (name != null && !name.isEmpty() && !name.equals(currentName)) {
            Log.d(TAG, "Updating UI with name: " + name);
            currentName = name;
            userName.setText(name);
            lastUpdateTime = currentTime;
        } else if (currentName == null) {
            Log.d(TAG, "Setting default name: User Profile Not Set");
            currentName = "User Profile Not Set";
            userName.setText("User Profile Not Set");
            lastUpdateTime = currentTime;
        } else {
            Log.d(TAG, "No UI update needed, name unchanged: " + name);
        }
    }

    private void updateUserNameInDatabase(String userId, String name) {
        // Update in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);

        firestoreDb.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User name updated in Firestore");
                    // Show success message
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user name in Firestore", e);
                    // Try updating in Realtime DB as fallback
                    realtimeDb.child(userId).child("name").setValue(name)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User name updated in Realtime DB instead");
                                Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to update user name in both databases", e2);
                                Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void migrateUserToFirestore(String userId, DataSnapshot dataSnapshot) {
        try {
            Map<String, Object> userData = new HashMap<>();
            for (DataSnapshot child : dataSnapshot.getChildren()) {
                userData.put(child.getKey(), child.getValue());
            }

            // Add timestamp for tracking when migration happened
            userData.put("migratedAt", System.currentTimeMillis());

            firestoreDb.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User data migrated successfully to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to migrate user data to Firestore", e));
        } catch (Exception e) {
            Log.e(TAG, "Error during migration to Firestore", e);
        }
    }

    private void logoutUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        } else {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImageSelector() {
        // Implement image selection logic here
        Toast.makeText(this, "Image selection not yet implemented", Toast.LENGTH_SHORT).show();

        // When implemented, it would look something like:
        /*
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
        */
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(ProfileActivity.this, destinationClass);
        startActivity(intent);
    }

    private String getAppVersion() {
        try {
            return "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version", e);
            return "v1.0.0";
        }
    }

    /**
     * Opens HTML page from assets in a WebView dialog
     */
    private void openHtmlPage(String htmlFileName, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.dialog_webview, null);

        WebView webView = customView.findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/" + htmlFileName);

        builder.setView(customView)
                .setTitle(title)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows password reset dialog similar to LoginActivity functionality
     */
    private void showChangePasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get email from current user
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: No email associated with account!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Send password reset email to " + email + "?")
                .setPositiveButton("Yes", (dialog, which) -> sendPasswordResetEmail(email))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Send password reset email - Same as LoginActivity
     */
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}