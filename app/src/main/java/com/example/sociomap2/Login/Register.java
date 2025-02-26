package com.example.sociomap2.Login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sociomap2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Register extends AppCompatActivity {

    // Define UI elements
    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputEditText editTextUsername, editTextName, editTextSurname, editTextBirthday;
    private Button btnReg;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView textView;
    private FirebaseFirestore db; // Firestore instance

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            //Intent intent = new Intent(getApplicationContext(), Profile.class);
            //startActivity(intent);
            //finish();
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize FirebaseAuth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirm_password);
        editTextUsername = findViewById(R.id.username);
        editTextName = findViewById(R.id.name);
        editTextSurname = findViewById(R.id.surname);
        editTextBirthday = findViewById(R.id.birthday);
        btnReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);

        // Click listener for "Already have an account?" text
        textView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        // Click listener for the registration button
        btnReg.setOnClickListener(v -> {
            registerUser();
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();
        String birthday = editTextBirthday.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        // ✅ **Validate Email Format**
        if (!isValidEmail(email)) {
            Toast.makeText(Register.this, "Invalid email format!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) ||
                TextUtils.isEmpty(username) || TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(birthday)) {
            Toast.makeText(Register.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(Register.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Toast.makeText(Register.this, "Verification email sent. Check your inbox!", Toast.LENGTH_LONG).show();
                                            saveTempUserData(username, name, surname, birthday, email);
                                        } else {
                                            Toast.makeText(Register.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            // Prevent automatic login
                            Toast.makeText(Register.this, "Account created! Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            finish();
                        }
                    } else {
                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void saveTempUserData(String username, String name, String surname, String birthday, String email) {
        getSharedPreferences("USER_DATA", MODE_PRIVATE).edit()
                .putString("username", username)
                .putString("name", name)
                .putString("surname", surname)
                .putString("birthday", birthday)
                .putString("email", email)
                .apply();
    }



    private void saveUserInfo(String username, String name, String surname, String birthday, String email) {
        // Create a map to hold user info
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("name", name);
        user.put("surname", surname);
        user.put("birthday", birthday); // e.g., "YYYY-MM-DD"
        user.put("email", email);

        // Additional fields
        user.put("famous", false);  // Default to false unless specified otherwise
        user.put("isAdmin", false); // Default to false unless specified otherwise
        user.put("password", Objects.requireNonNull(editTextPassword.getText()).toString().trim()); // Storing password; consider secure handling

        // Obtain current user's UID and set document in Firestore
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    // Success feedback to user
                    Toast.makeText(Register.this, "Account created successfully.", Toast.LENGTH_SHORT).show();

                    // Move to login screen
                    Intent intent = new Intent(getApplicationContext(), Login.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Error logging for debugging purposes
                    Log.e("Register", "Error saving user info", e);
                    Toast.makeText(Register.this, "Error saving user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}