package com.example.sociomap2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMarkerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private EditText edtTitle, edtDescription, edtDate, edtTime, edtCustomTheme;
    private Spinner spnTheme;
    private double latitude, longitude;
    private Calendar selectedDateTime = Calendar.getInstance(); // Stores Date & Time
    private String selectedTheme = ""; // Stores the selected theme

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marker);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Retrieve coordinates from intent
        latitude = getIntent().getDoubleExtra("LATITUDE", 0);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 0);

        // UI Elements
        edtTitle = findViewById(R.id.edt_title);
        edtDescription = findViewById(R.id.edt_description);
        edtDate = findViewById(R.id.edt_date);
        edtTime = findViewById(R.id.edt_time);
        spnTheme = findViewById(R.id.spn_theme);
        edtCustomTheme = findViewById(R.id.edt_custom_theme);
        Button btnSave = findViewById(R.id.btn_save);

        // Set Click Listeners for Date & Time Pickers
        edtDate.setOnClickListener(v -> showDatePicker());
        edtTime.setOnClickListener(v -> showTimePicker());

        // Theme Selection Logic
        spnTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTheme = parent.getItemAtPosition(position).toString();
                edtCustomTheme.setVisibility(selectedTheme.equals("Custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                edtCustomTheme.setVisibility(View.GONE);
            }
        });

        btnSave.setOnClickListener(v -> saveMarker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(year, month, dayOfMonth);
            edtDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            edtTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDateTime.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void saveMarker() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        String userName = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Unknown User";
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String eventDate = edtDate.getText().toString().trim();
        String eventTime = edtTime.getText().toString().trim();

        // Check if user chose a custom theme
        String finalTheme = selectedTheme.equals("Custom") && !edtCustomTheme.getText().toString().trim().isEmpty()
                ? edtCustomTheme.getText().toString().trim()
                : selectedTheme;

        if (title.isEmpty() || description.isEmpty() || eventDate.isEmpty() || eventTime.isEmpty() || finalTheme.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> marker = new HashMap<>();
        marker.put("latitude", latitude);
        marker.put("longitude", longitude);
        marker.put("title", title);
        marker.put("description", description);
        marker.put("userId", userId);
        marker.put("userName", userName);
        marker.put("eventDateTime", eventDate + " " + eventTime);
        marker.put("theme", finalTheme); // Store the theme in Firestore ✅

        firestore.collection("markers").add(marker)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving marker", Toast.LENGTH_SHORT).show());
    }
}