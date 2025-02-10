package com.example.sociomap2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

public class MarkerInfoActivity extends AppCompatActivity {

    private static final String TAG = "MarkerInfoActivity";

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String eventId;
    private TextView eventName, eventDate, eventDescription;
    private Button signUpButton, deleteButton, editButton;
    private boolean isUserSignedUp = false;  // Track if the user is signed up

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        eventName = findViewById(R.id.event_name);
        eventDate = findViewById(R.id.event_date);
        eventDescription = findViewById(R.id.event_description);
        signUpButton = findViewById(R.id.btn_sign_up);
        deleteButton = findViewById(R.id.btn_delete_event);
        editButton = findViewById(R.id.btn_edit_event);

        // Get the event ID from the Intent
        eventId = getIntent().getStringExtra("MARKER_ID");

        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID is missing");
            Toast.makeText(this, "Error loading event: ID missing", Toast.LENGTH_SHORT).show();
            finish();  // Close the activity if event ID is missing
            return;
        }

        loadEventDetails();
    }

    private void loadEventDetails() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getData() != null) {
                        String title = document.getString("title");
                        String date = document.getString("date");
                        String description = document.getString("description");
                        String userId = document.getString("userId");  // owner of the marker

                        eventName.setText(title);
                        eventDate.setText(date);
                        eventDescription.setText(description);

                        // Check if the logged-in user is the owner of the marker
                        checkIfUserIsOwner(userId);
                        checkUserParticipation(); // Check if the user is already signed up for the event
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event details", e);
                    Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkIfUserIsOwner(String ownerId) {
        String loggedInUserId = firebaseAuth.getCurrentUser().getUid();

        if (ownerId != null && ownerId.equals(loggedInUserId)) {
            // User is the owner, so show delete and edit buttons
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            signUpButton.setVisibility(View.VISIBLE);  // Hide the sign up button for the owner

            // Delete and Edit button listeners
            deleteButton.setOnClickListener(v -> deleteEvent());
            editButton.setOnClickListener(v -> editEvent());
        } else {
            // Hide the delete and edit buttons for non-owners
            deleteButton.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            checkUserParticipation(); // Check if the user is signed up for the event
        }
    }

    private void checkUserParticipation() {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains(firebaseAuth.getCurrentUser().getUid())) {
                        // User is signed up
                        isUserSignedUp = true;
                        signUpButton.setText("Sign Out from Event");
                    } else {
                        // User is not signed up
                        isUserSignedUp = false;
                        signUpButton.setText("Sign Up for Event");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking user participation", e));
    }

    // Sign up or sign out when the user clicks the button
    public void onSignUpButtonClick(View view) {
        if (isUserSignedUp) {
            signOutFromEvent();
        } else {
            signUpForEvent();
        }
    }

    private void signUpForEvent() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String userFullName = firebaseAuth.getCurrentUser().getDisplayName(); // You can use this or fetch it from Firestore

        if (userFullName == null) {
            Log.e(TAG, "User's full name is missing");
            return;
        }

        // Add user to the event's guest list
        firestore.collection("event_guest_list").document(eventId)
                .update(userId, userFullName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MarkerInfoActivity.this, "Signed up for the event!", Toast.LENGTH_SHORT).show();
                    isUserSignedUp = true;
                    signUpButton.setText("Sign Out from Event");
                })
                .addOnFailureListener(e -> {
                    firestore.collection("event_guest_list").document(eventId)
                            .set(new java.util.HashMap<String, Object>() {{
                                put(userId, userFullName);
                            }})
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MarkerInfoActivity.this, "Signed up for the event!", Toast.LENGTH_SHORT).show();
                                isUserSignedUp = true;
                                signUpButton.setText("Sign Out from Event");
                            })
                            .addOnFailureListener(error -> {
                                Log.e(TAG, "Error signing up for event", error);
                                Toast.makeText(MarkerInfoActivity.this, "Error signing up for the event", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void signOutFromEvent() {
        // Remove user from the event's guest list
        firestore.collection("event_guest_list").document(eventId)
                .update(firebaseAuth.getCurrentUser().getUid(), FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MarkerInfoActivity.this, "Signed out from the event!", Toast.LENGTH_SHORT).show();
                    isUserSignedUp = false;
                    signUpButton.setText("Sign Up for Event");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error signing out from event", e);
                    Toast.makeText(MarkerInfoActivity.this, "Error signing out from event", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteEvent() {
        firestore.collection("markers").document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MarkerInfoActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after deletion
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    Toast.makeText(MarkerInfoActivity.this, "Error deleting event", Toast.LENGTH_SHORT).show();
                });
    }

    private void editEvent() {
        // Redirect to an edit screen or allow user to modify event details
        Intent intent = new Intent(MarkerInfoActivity.this, EditMarkerActivity.class);
        intent.putExtra("EVENT_ID", eventId);  // Pass eventId for editing
        startActivity(intent);
    }
}
