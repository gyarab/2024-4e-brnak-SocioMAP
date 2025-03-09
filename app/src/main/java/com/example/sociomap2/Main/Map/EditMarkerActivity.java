package com.example.sociomap2.Main.Map;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditMarkerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String eventId;

    private EditText edtTitle, edtDescription, edtDate;
    private Button btnSave;
    private Spinner spnAgeLimit;
    private int selectedAgeLimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_marker);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        eventId = getIntent().getStringExtra("EVENT_ID");

        edtTitle = findViewById(R.id.edt_title);
        edtDescription = findViewById(R.id.edt_description);
        //edtDate = findViewById(R.id.edt_date);
        btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveEvent());

        loadEventDetails();



        // In EditMarkerActivity
        Spinner spnTheme = findViewById(R.id.spn_theme);
        EditText edtCustomTheme = findViewById(R.id.edt_custom_theme);

        // When a user selects a theme from the spinner
        spnTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (spnTheme.getSelectedItem().toString().equals("Custom")) {
                    edtCustomTheme.setVisibility(View.VISIBLE); // Show the custom theme field
                } else {
                    edtCustomTheme.setVisibility(View.GONE); // Hide the custom theme field
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing if nothing is selected
            }
        });

        spnAgeLimit = findViewById(R.id.spn_age_limit);

        // Spinner with age
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.age_limits, android.R.layout.simple_spinner_dropdown_item);
        spnAgeLimit.setAdapter(adapter);

    }

    private void loadEventDetails() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        edtTitle.setText(document.getString("title"));
                        edtDescription.setText(document.getString("description"));
                        edtDate.setText(document.getString("date"));
                        // Set age limit
                        int ageLimit = document.contains("ageLimit") ? document.getLong("ageLimit").intValue() : 3;
                        spnAgeLimit.setSelection(ageLimit / 3 - 1);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }



    private void saveEvent() {
        String title = edtTitle.getText().toString();
        String description = edtDescription.getText().toString();
        String date = edtDate.getText().toString();

        Map<String, Object> updatedEvent = new HashMap<>();
        updatedEvent.put("title", title);
        updatedEvent.put("description", description);
        updatedEvent.put("date", date);
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
