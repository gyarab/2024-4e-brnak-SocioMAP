package com.example.sociomap2;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarkerInfoActivity extends AppCompatActivity {

    private static final String TAG = "MarkerInfoActivity";
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;
    private String userFullName;
    private String markerId;

    private Button btnAction;
    private ListView attendeesListView;
    private ArrayAdapter<String> attendeesAdapter;
    private List<String> attendeesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        // Enable back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            fetchUserFullName(); // Get user's full name
        } else {
            Log.e(TAG, "User is not logged in.");
            finish();
            return;
        }

        // Get marker data from intent
        markerId = getIntent().getStringExtra("MARKER_ID");
        String title = getIntent().getStringExtra("TITLE");
        String description = getIntent().getStringExtra("DESCRIPTION");

        // Set data to views
        ((TextView) findViewById(R.id.txt_title)).setText(title);
        ((TextView) findViewById(R.id.txt_description)).setText(description);

        btnAction = findViewById(R.id.btn_assign);
        attendeesListView = findViewById(R.id.list_attendees);

        // Set up adapter for attendees
        attendeesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendeesList);
        attendeesListView.setAdapter(attendeesAdapter);

        checkUserParticipation();
        loadAttendees();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchUserFullName() {
        if (userId == null) return;

        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String name = document.getString("name");

                        if (username != null && name != null) {
                            userFullName = username + " (" + name + ")";
                        } else {
                            userFullName = "Unknown User";
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user name", e));
    }

    private void checkUserParticipation() {
        if (markerId == null || userId == null) {
            Log.e(TAG, "Marker ID or User ID is missing.");
            return;
        }

        firestore.collection("user_events").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains(markerId)) {
                        btnAction.setText("Sign Out from Event");
                        btnAction.setOnClickListener(v -> signOutFromEvent());
                    } else {
                        btnAction.setText("Sign Up for Event");
                        btnAction.setOnClickListener(v -> signUpForEvent());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking user participation", e));
    }

    private void signUpForEvent() {
        if (markerId == null || userId == null || userFullName == null) return;

        firestore.collection("user_events").document(userId)
                .update(markerId, true)
                .addOnFailureListener(e -> firestore.collection("user_events")
                        .document(userId)
                        .set(new java.util.HashMap<String, Object>() {{
                            put(markerId, true);
                        }})
                );

        firestore.collection("event_guest_list").document(markerId)
                .update(userId, userFullName)
                .addOnFailureListener(e -> firestore.collection("event_guest_list")
                        .document(markerId)
                        .set(new java.util.HashMap<String, Object>() {{
                            put(userId, userFullName);
                        }})
                );

        Toast.makeText(this, "Signed up for the event!", Toast.LENGTH_SHORT).show();
        checkUserParticipation();
        loadAttendees();
    }

    private void signOutFromEvent() {
        if (markerId == null || userId == null) return;

        firestore.collection("user_events").document(userId)
                .update(markerId, null)
                .addOnFailureListener(e -> Log.e(TAG, "Error signing out", e));

        firestore.collection("event_guest_list").document(markerId)
                .update(userId, null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You left the event!", Toast.LENGTH_SHORT).show();
                    checkUserParticipation();
                    loadAttendees();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error removing user from guest list", e));
    }

    private void loadAttendees() {
        if (markerId == null) return;

        firestore.collection("event_guest_list").document(markerId)
                .get()
                .addOnSuccessListener(document -> {
                    attendeesList.clear();
                    if (document.exists()) {
                        Map<String, Object> attendees = document.getData();
                        if (attendees != null) {
                            for (Object name : attendees.values()) {
                                if (name instanceof String) {
                                    attendeesList.add((String) name);
                                }
                            }
                        }
                    }
                    attendeesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading attendees", e));
    }

}
