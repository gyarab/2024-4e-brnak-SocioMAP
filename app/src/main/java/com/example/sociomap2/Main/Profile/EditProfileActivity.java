package com.example.sociomap2.Main.Profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;

    private EditText editUsername, editName, editSurname, editBirthYear;
    private Button saveButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        editUsername = findViewById(R.id.edit_username);
        editName = findViewById(R.id.edit_name);
        editSurname = findViewById(R.id.edit_surname);
        editBirthYear = findViewById(R.id.edit_birthyear);
        editBirthYear.setFocusable(false);
        editBirthYear.setOnClickListener(v -> showDatePickerDialog());
        saveButton = findViewById(R.id.save_changes);
        cancelButton = findViewById(R.id.cancel_changes);

        if (firebaseUser != null) {
            loadUserData(firebaseUser.getUid());
        }

        saveButton.setOnClickListener(v -> saveChanges());

        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -10); // Prevent selecting future dates, assumes min 10 years old

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    editBirthYear.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR) - 18, // Default selection (18 years ago)
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Prevent future dates
        datePickerDialog.show();
    }


    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editUsername.setText(documentSnapshot.getString("username"));
                        editName.setText(documentSnapshot.getString("name"));
                        editSurname.setText(documentSnapshot.getString("surname"));
                        editBirthYear.setText(documentSnapshot.getString("birthyear"));
                    } else {
                        Toast.makeText(EditProfileActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges() {
        String username = editUsername.getText().toString().trim();
        String name = editName.getText().toString().trim();
        String surname = editSurname.getText().toString().trim();
        String birthYear = editBirthYear.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(birthYear)) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedUser = new HashMap<>();
        updatedUser.put("username", username);
        updatedUser.put("name", name);
        updatedUser.put("surname", surname);
        updatedUser.put("birthyear", birthYear);

        db.collection("users").document(firebaseUser.getUid())
                .update(updatedUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to ProfileFragment
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error updating profile.", Toast.LENGTH_SHORT).show();
                });
    }
}

