package com.example.sociomap2.Main.Map;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMarkerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private EditText edtTitle, edtDescription, edtDate, edtTime, edtCustomTheme, edtMaxCapacity;
    private Spinner spnTheme;
    private double latitude, longitude;
    private Calendar selectedDateTime = Calendar.getInstance(); // Stores Date & Time
    private String selectedTheme = ""; // Stores the selected theme
    private Spinner spnAgeLimit;
    private int selectedAgeLimit = 3;


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
        edtMaxCapacity = findViewById(R.id.edt_max_capacity);
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


        // Initialize UI Elements
        spnAgeLimit = findViewById(R.id.spn_age_limit);

        // Populate Spinner with age options (3+ to 18+)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.age_limits, android.R.layout.simple_spinner_dropdown_item);
        spnAgeLimit.setAdapter(adapter);

        spnAgeLimit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAgeLimit = Integer.parseInt(parent.getItemAtPosition(position).toString().replace("+", ""));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveMarker());
    }


            private void showDatePicker() {
        Calendar calendar = Calendar.getInstance(); // Get current date

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);
                    edtDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Restrict past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
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
        String maxCapacityStr = edtMaxCapacity.getText().toString().trim();

        // Check if user chose a custom theme
        String finalTheme = selectedTheme.equals("Custom") && !edtCustomTheme.getText().toString().trim().isEmpty()
                ? edtCustomTheme.getText().toString().trim()
                : selectedTheme;

        if (title.isEmpty() || description.isEmpty() || eventDate.isEmpty() || eventTime.isEmpty() || finalTheme.isEmpty() || maxCapacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate and convert max capacity
        int maxCapacity;
        try {
            maxCapacity = Integer.parseInt(maxCapacityStr);
            if (maxCapacity < 0) {
                Toast.makeText(this, "Capacity cannot be negative!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for capacity!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create marker data
        Map<String, Object> marker = new HashMap<>();
        marker.put("latitude", latitude);
        marker.put("longitude", longitude);
        marker.put("title", title);
        marker.put("description", description);
        marker.put("userId", userId);
        marker.put("userName", userName);
        marker.put("eventDateTime", eventDate + " " + eventTime);
        marker.put("theme", finalTheme); // Keep the theme storage
        marker.put("maxCapacity", maxCapacity); // Store the max capacity
        marker.put("currentAttendees", 0); // Start attendee count at 0
        marker.put("ageLimit", selectedAgeLimit);


        // Save marker to Firestore
        firestore.collection("markers").add(marker)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Redirect user back to `MapsFragment`
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving marker", Toast.LENGTH_SHORT).show());
    }
}