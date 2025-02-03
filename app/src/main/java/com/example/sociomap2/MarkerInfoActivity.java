package com.example.sociomap2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MarkerInfoActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String markerId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        // Get marker data from intent with null checks
        markerId = getIntent().getStringExtra("MARKER_ID"); // Ensure marker ID is passed
        String title = getIntent().getStringExtra("TITLE");
        String description = getIntent().getStringExtra("DESCRIPTION");

        if (title == null || description == null || markerId == null) {
            Toast.makeText(this, "Error: Missing marker data.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if data is missing
            return;
        }

        // Set data to views
        ((TextView) findViewById(R.id.txt_title)).setText(title);
        ((TextView) findViewById(R.id.txt_description)).setText(description);

        // Add logic for "Join Event" button
        Button btnAssign = findViewById(R.id.btn_assign);
        btnAssign.setOnClickListener(v -> assignUserToEvent());
    }

    private void assignUserToEvent() {
        if (markerId == null || userId.equals("anonymous")) {
            Toast.makeText(this, "Error: Cannot join event.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user ID to event_guest_list
        firestore.collection("event_guest_list").document(markerId)
                .update("users", FieldValue.arrayUnion(userId))
                .addOnFailureListener(e -> {
                    // If document doesn't exist, create it
                    Map<String, Object> guestList = new HashMap<>();
                    guestList.put("users", FieldValue.arrayUnion(userId));
                    firestore.collection("event_guest_list").document(markerId).set(guestList);
                });

        // Add event ID to user's joined events
        firestore.collection("user_events").document(userId)
                .update("joinedEvents", FieldValue.arrayUnion(markerId))
                .addOnFailureListener(e -> {
                    // If document doesn't exist, create it
                    Map<String, Object> userEvents = new HashMap<>();
                    userEvents.put("joinedEvents", FieldValue.arrayUnion(markerId));
                    firestore.collection("user_events").document(userId).set(userEvents);
                });

        Toast.makeText(this, "Successfully joined the event!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
