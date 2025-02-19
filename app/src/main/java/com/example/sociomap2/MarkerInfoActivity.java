package com.example.sociomap2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerInfoActivity extends AppCompatActivity {

    private static final String TAG = "MarkerInfoActivity";

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String eventId;
    private TextView eventName, eventDate, eventDescription, eventTheme;
    private Button signUpButton, deleteButton, editButton;
    private ListView listAttendees;
    private boolean isUserSignedUp = false;
    private ArrayAdapter<String> attendeesAdapter;
    private List<String> attendeesList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        eventName = findViewById(R.id.event_name);
        eventDate = findViewById(R.id.event_date);
        eventDescription = findViewById(R.id.event_description);
        eventTheme = findViewById(R.id.event_theme); // Added event theme TextView
        signUpButton = findViewById(R.id.btn_sign_up);
        deleteButton = findViewById(R.id.btn_delete_event);
        editButton = findViewById(R.id.btn_edit_event);
        listAttendees = findViewById(R.id.list_attendees);

        // Set up ListView Adapter
        attendeesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendeesList);
        listAttendees.setAdapter(attendeesAdapter);

        // Get event ID from Intent
        eventId = getIntent().getStringExtra("MARKER_ID");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Enable Firestore network
        firestore.enableNetwork()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firestore network re-enabled.");
                    } else {
                        Log.e(TAG, "Failed to enable Firestore network", task.getException());
                    }
                });

        loadEventDetails();
        loadAttendeesList();
        checkIfUserIsOwner();


        // ✅ Fix: Set click listener for the sign-up button
        signUpButton.setOnClickListener(v -> {
            if (isUserSignedUp) {
                signOutFromEvent();
            } else {
                signUpForEvent();
            }
        });
    }

    private void checkIfUserIsOwner() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String ownerId = document.getString("userId"); // Get event owner ID
                        String currentUserId = firebaseAuth.getCurrentUser().getUid();

                        if (ownerId != null && ownerId.equals(currentUserId)) {
                            // ✅ Show buttons if the user is the owner
                            deleteButton.setVisibility(View.VISIBLE);
                            editButton.setVisibility(View.VISIBLE);

                            // Set button listeners
                            deleteButton.setOnClickListener(v -> deleteEvent());
                            editButton.setOnClickListener(v -> editEvent());
                        } else {
                            // ❌ Hide buttons if not the owner
                            deleteButton.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking event ownership", e));
    }

    private void deleteEvent() {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> userIds = (List<String>) document.get("users");
                        if (userIds != null) {
                            for (String userId : userIds) {
                                removeEventFromUserEvents(userId);
                            }
                        }
                    }

                    // ✅ Delete the event from Firestore collections
                    firestore.collection("markers").document(eventId).delete();
                    firestore.collection("event_guest_list").document(eventId).delete();

                    Toast.makeText(this, "Event deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting event", e));
    }

    public void editEvent() {
        //To new activity for editing the marker
    }

    private void removeEventFromUserEvents(String userId) {
        firestore.collection("user_events").document(userId)
                .update("events", FieldValue.arrayRemove(eventId))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing event from user_events", e));
    }

    private void loadEventDetails() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        eventName.setText(document.getString("title"));
                        eventDate.setText(document.getString("eventDateTime"));
                        eventDescription.setText(document.getString("description"));

                        // Load and display the event theme
                        String theme = document.getString("theme");
                        if (theme != null) {
                            eventTheme.setText("Theme: " + theme);
                        } else {
                            eventTheme.setText("Theme: Not specified");
                        }

                        checkUserParticipation();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading event details.", Toast.LENGTH_SHORT).show());
    }

    private void loadAttendeesList() {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    attendeesList.clear();
                    if (document.exists()) {
                        List<String> userIds = (List<String>) document.get("users");
                        if (userIds != null) {
                            for (String userId : userIds) {
                                fetchUserDetails(userId); // Fetch name, surname, and username
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading attendees", e));
    }

    // ✅ Fetch `name`, `surname`, and `username` from Firestore
    private void fetchUserDetails(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        String surname = userDoc.getString("surname");
                        String username = userDoc.getString("username");

                        // Format: "Name Surname (@username)"
                        String displayText = "";
                        if (name != null && !name.isEmpty()) {
                            displayText += name + " ";
                        }
                        if (surname != null && !surname.isEmpty()) {
                            displayText += surname;
                        }
                        if (username != null && !username.isEmpty()) {
                            displayText += " (@" + username + ")";
                        }

                        if (displayText.isEmpty()) {
                            displayText = "Unknown User"; // Fallback if missing
                        }

                        attendeesList.add(displayText);
                        attendeesAdapter.notifyDataSetChanged(); // Refresh ListView
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }

    private void checkUserParticipation() {
        firestore.collection("user_events").document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> joinedEvents = (List<String>) document.get("events");
                        if (joinedEvents != null && joinedEvents.contains(eventId)) {
                            isUserSignedUp = true;
                            signUpButton.setText("Sign Out from Event");
                        } else {
                            isUserSignedUp = false;
                            signUpButton.setText("Sign Up for Event");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking user participation", e));
    }

    private void signUpForEvent() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String userName = firebaseAuth.getCurrentUser().getDisplayName();

        if (userName == null) {
            Toast.makeText(this, "Error: User name is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Add event ID to user's `user_events` document
        firestore.collection("user_events").document(userId)
                .update("events", FieldValue.arrayUnion(eventId))
                .addOnFailureListener(e -> {
                    Map<String, Object> userEventsData = new HashMap<>();
                    userEventsData.put("events", new ArrayList<String>() {{
                        add(eventId);
                    }});
                    firestore.collection("user_events").document(userId).set(userEventsData);
                });

        // ✅ Add user ID to event's `event_guest_list` document
        firestore.collection("event_guest_list").document(eventId)
                .update("users", FieldValue.arrayUnion(userId))
                .addOnFailureListener(e -> {
                    Map<String, Object> eventGuestData = new HashMap<>();
                    eventGuestData.put("users", new ArrayList<String>() {{
                        add(userId);
                    }});
                    firestore.collection("event_guest_list").document(eventId).set(eventGuestData);
                });

        Toast.makeText(this, "Signed up for the event!", Toast.LENGTH_SHORT).show();
        loadAttendeesList();
        checkUserParticipation();
    }

    private void signOutFromEvent() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        // ✅ Remove event ID from user's `user_events` document
        firestore.collection("user_events").document(userId)
                .update("events", FieldValue.arrayRemove(eventId));

        // ✅ Remove user ID from event's `event_guest_list` document
        firestore.collection("event_guest_list").document(eventId)
                .update("users", FieldValue.arrayRemove(userId));

        Toast.makeText(this, "Signed out from event!", Toast.LENGTH_SHORT).show();
        loadAttendeesList();
        checkUserParticipation();
    }
}