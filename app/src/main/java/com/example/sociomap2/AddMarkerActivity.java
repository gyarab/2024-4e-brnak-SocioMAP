package com.example.sociomap2;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddMarkerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marker);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        double latitude = getIntent().getDoubleExtra("LATITUDE", 0);
        double longitude = getIntent().getDoubleExtra("LONGITUDE", 0);

        EditText edtTitle = findViewById(R.id.edt_title);
        EditText edtDescription = findViewById(R.id.edt_description);
        Spinner spnTheme = findViewById(R.id.spn_theme);
        EditText edtCustomTheme = findViewById(R.id.edt_custom_theme);

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
            String title = edtTitle.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();

            // 🛑 Fix Null Pointer Issue for Spinner
            String selectedTheme = (spnTheme.getSelectedItem() != null) ? spnTheme.getSelectedItem().toString() : "";
            String customTheme = edtCustomTheme.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine the theme (either selected or custom)
            String theme = selectedTheme.equals("Custom") && !customTheme.isEmpty() ? customTheme : selectedTheme;

            // Create marker data
            Map<String, Object> marker = new HashMap<>();
            marker.put("latitude", latitude);
            marker.put("longitude", longitude);
            marker.put("title", title);
            marker.put("description", description);
            marker.put("userId", userId); // Owner of the marker
            marker.put("theme", theme); // Event theme
            marker.put("attendees", new ArrayList<String>()); // Initialize empty attendees list

            firestore.collection("markers").add(marker)
                    .addOnSuccessListener(documentReference -> {
                        String markerId = documentReference.getId(); // Get document ID

                        // Ensure event_guest_list is created
                        Map<String, Object> guestList = new HashMap<>();
                        guestList.put("users", new ArrayList<String>());
                        firestore.collection("event_guest_list").document(markerId).set(guestList);

                        // Ensure user_events is created
                        firestore.collection("user_events").document(userId)
                                .update("joinedEvents", FieldValue.arrayUnion(markerId))
                                .addOnFailureListener(e -> {
                                    // If document doesn't exist, create it
                                    Map<String, Object> newUserEvents = new HashMap<>();
                                    newUserEvents.put("joinedEvents", new ArrayList<String>());
                                    firestore.collection("user_events").document(userId).set(newUserEvents);
                                });

                        // Update the user's event list (for ownership)
                        firestore.collection("user_ownerofevent").document(userId)
                                .update(markerId, title) // Store event under user ID
                                .addOnFailureListener(e -> Log.e(TAG, "Error saving event to owner list", e));

                        Toast.makeText(this, "Marker saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving marker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

    }
}
