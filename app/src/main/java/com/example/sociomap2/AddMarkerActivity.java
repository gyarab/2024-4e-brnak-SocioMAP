package com.example.sociomap2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

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
            String title = edtTitle.getText().toString();
            String description = edtDescription.getText().toString();
            String selectedTheme = spnTheme.getSelectedItem().toString();
            String customTheme = edtCustomTheme.getText().toString();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine the theme (either selected or custom)
            String theme = selectedTheme.equals("Custom") && !customTheme.isEmpty() ? customTheme : selectedTheme;

            // Save marker to Firestore
            HashMap<String, Object> marker = new HashMap<>();
            marker.put("latitude", latitude);
            marker.put("longitude", longitude);
            marker.put("title", title);
            marker.put("description", description);
            marker.put("userId", userId); // Owner of the marker
            marker.put("theme", theme); // Event theme
            marker.put("attendees", new ArrayList<String>()); // Initialize empty attendees list

            firestore.collection("markers").add(marker)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Marker saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving marker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}