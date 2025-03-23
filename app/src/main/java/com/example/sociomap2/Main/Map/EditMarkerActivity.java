package com.example.sociomap2.Main.Map;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class EditMarkerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String eventId;

    private EditText edtTitle, edtDescription, edtDate, edtTime, edtMaxCapacity, edtCustomTheme;
    private Spinner spnTheme, spnAgeLimit;
    private String selectedTheme = "";
    private int selectedAgeLimit = 3;
    private Calendar selectedDateTime = Calendar.getInstance(); // for setting time & date

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_marker);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        eventId = getIntent().getStringExtra("EVENT_ID");

        // UI Elements
        edtTitle = findViewById(R.id.edt_title);
        edtDescription = findViewById(R.id.edt_description);
        edtDate = findViewById(R.id.edt_date);
        edtTime = findViewById(R.id.edt_time);
        edtMaxCapacity = findViewById(R.id.edt_max_capacity);
        edtCustomTheme = findViewById(R.id.edt_custom_theme);
        spnTheme = findViewById(R.id.spn_theme);
        spnAgeLimit = findViewById(R.id.spn_age_limit);
        Button btnSave = findViewById(R.id.btn_save);

        edtDate.setOnClickListener(v -> showDatePicker());
        edtTime.setOnClickListener(v -> showTimePicker());

        // THEME SPINNER
        ArrayAdapter<CharSequence> themeAdapter = new ArrayAdapter<CharSequence>(
                this,
                R.layout.spinner_selected_item,
                getResources().getStringArray(R.array.event_themes)
        ) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                if (text != null) {
                    text.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };
        themeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_marker);
        spnTheme.setAdapter(themeAdapter);

        spnTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTheme = parent.getItemAtPosition(position).toString();
                edtCustomTheme.setVisibility(selectedTheme.equals("Custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // AGE LIMIT SPINNER
        ArrayAdapter<CharSequence> ageAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_selected_item,
                getResources().getStringArray(R.array.age_limits)
        );
        ageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_marker);
        spnAgeLimit.setAdapter(ageAdapter);

        spnAgeLimit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAgeLimit = Integer.parseInt(parent.getItemAtPosition(position).toString().replace("+", ""));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveEvent());

        loadEventDetails();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(year, month, dayOfMonth);
            edtDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            edtTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDateTime.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void loadEventDetails() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        edtTitle.setText(document.getString("title"));
                        edtDescription.setText(document.getString("description"));

                        String eventDateTime = document.getString("eventDateTime");
                        if (eventDateTime != null && eventDateTime.contains(" ")) {
                            String[] split = eventDateTime.split(" ");
                            edtDate.setText(split[0]);
                            edtTime.setText(split[1]);
                        }

                        int ageLimit = document.contains("ageLimit") ? document.getLong("ageLimit").intValue() : 3;
                        spnAgeLimit.setSelection(ageLimit / 3 - 1);

                        int capacity = document.contains("maxCapacity") ? document.getLong("maxCapacity").intValue() : 0;
                        edtMaxCapacity.setText(String.valueOf(capacity));

                        String theme = document.getString("theme");
                        if (theme != null) {
                            String[] themes = getResources().getStringArray(R.array.event_themes);
                            boolean found = false;
                            for (int i = 0; i < themes.length; i++) {
                                if (themes[i].equals(theme)) {
                                    spnTheme.setSelection(i);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // Not found → it's a custom theme
                                spnTheme.setSelection(themes.length - 1); // "Custom"
                                edtCustomTheme.setText(theme);
                                edtCustomTheme.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void saveEvent() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String eventDate = edtDate.getText().toString().trim();
        String eventTime = edtTime.getText().toString().trim();
        String maxCapacityStr = edtMaxCapacity.getText().toString().trim();

        String finalTheme = selectedTheme.equals("Custom") && !edtCustomTheme.getText().toString().trim().isEmpty()
                ? edtCustomTheme.getText().toString().trim()
                : selectedTheme;

        if (title.isEmpty() || description.isEmpty() || eventDate.isEmpty() || eventTime.isEmpty() || finalTheme.isEmpty() || maxCapacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxCapacity;
        try {
            maxCapacity = Integer.parseInt(maxCapacityStr);
            if (maxCapacity < 0) {
                Toast.makeText(this, "Capacity cannot be negative!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number for capacity!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedEvent = new HashMap<>();
        updatedEvent.put("title", title);
        updatedEvent.put("description", description);
        updatedEvent.put("eventDateTime", eventDate + " " + eventTime);
        updatedEvent.put("theme", finalTheme);
        updatedEvent.put("maxCapacity", maxCapacity);
        updatedEvent.put("ageLimit", selectedAgeLimit);

        firestore.collection("markers").document(eventId)
                .update(updatedEvent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating event", Toast.LENGTH_SHORT).show());
    }
}