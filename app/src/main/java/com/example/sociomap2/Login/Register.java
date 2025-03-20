package com.example.sociomap2.Login;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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


import com.example.sociomap2.Login.Login;
import com.example.sociomap2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Register extends AppCompatActivity {

    // Define UI elements
    private TextInputLayout usernameLayout, nameLayout, surnameLayout, birthdayLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputEditText editTextUsername, editTextName, editTextSurname, editTextBirthday;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView registerTitle, loginNow;
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
        editTextBirthday.setFocusable(false);
        editTextBirthday.setOnClickListener(v -> showDatePickerDialog());
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);


        registerTitle = findViewById(R.id.registerTitle);
        usernameLayout = findViewById(R.id.usernameLayout);
        nameLayout = findViewById(R.id.nameLayout);
        surnameLayout = findViewById(R.id.surnameLayout);
        birthdayLayout = findViewById(R.id.birthdayLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        btnRegister = findViewById(R.id.btn_register);
        loginNow = findViewById(R.id.loginNow);


        // Click listener for "Already have an account?" text
        loginNow.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        // Click listener for the registration button
        btnRegister.setOnClickListener(v -> {
            registerUser();
        });

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        Animator shadowAnim = AnimatorInflater.loadAnimator(this, R.animator.button_shadow);

        // Ensure views start as invisible
        registerTitle.setVisibility(View.INVISIBLE);
        usernameLayout.setVisibility(View.INVISIBLE);
        nameLayout.setVisibility(View.INVISIBLE);
        surnameLayout.setVisibility(View.INVISIBLE);
        birthdayLayout.setVisibility(View.INVISIBLE);
        emailLayout.setVisibility(View.INVISIBLE);
        passwordLayout.setVisibility(View.INVISIBLE);
        confirmPasswordLayout.setVisibility(View.INVISIBLE);
        btnRegister.setVisibility(View.INVISIBLE);
        loginNow.setVisibility(View.INVISIBLE);

        // Apply animations in sequence with delays
        new Handler().postDelayed(() -> {
            registerTitle.startAnimation(slideIn);
            registerTitle.setVisibility(View.VISIBLE);
        }, 100);

        new Handler().postDelayed(() -> {
            usernameLayout.startAnimation(slideIn);
            usernameLayout.setVisibility(View.VISIBLE);
        }, 800);

        new Handler().postDelayed(() -> {
            nameLayout.startAnimation(slideIn);
            nameLayout.setVisibility(View.VISIBLE);
        }, 1500);

        new Handler().postDelayed(() -> {
            surnameLayout.startAnimation(slideIn);
            surnameLayout.setVisibility(View.VISIBLE);

        }, 2200);

        new Handler().postDelayed(() -> {
            birthdayLayout.startAnimation(slideIn);
            birthdayLayout.setVisibility(View.VISIBLE);

        }, 2900);

        new Handler().postDelayed(() -> {
            emailLayout.startAnimation(slideIn);
            emailLayout.setVisibility(View.VISIBLE);

        }, 3600);

        new Handler().postDelayed(() -> {
            passwordLayout.startAnimation(slideIn);
            passwordLayout.setVisibility(View.VISIBLE);

        }, 4400);

        new Handler().postDelayed(() -> {
            confirmPasswordLayout.startAnimation(slideIn);
            confirmPasswordLayout.setVisibility(View.VISIBLE);

        }, 5100);

        new Handler().postDelayed(() -> {
            btnRegister.startAnimation(slideIn);
            shadowAnim.setTarget(btnRegister);
            shadowAnim.start();
            btnRegister.setVisibility(View.VISIBLE);
        }, 5800);

        new Handler().postDelayed(() -> {
            loginNow.startAnimation(fadeIn);
            loginNow.setVisibility(View.VISIBLE);

        }, 6500);

    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        // Set the max selectable date to today (no future dates)
        long maxDate = System.currentTimeMillis();

        // Calculate the minimum selectable date (3 years old)
        calendar.add(Calendar.YEAR, -3);
        long minDate = calendar.getTimeInMillis();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    editTextBirthday.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR) - 10, // Default selection (10 years ago)
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Prevent selecting future dates
        datePickerDialog.getDatePicker().setMaxDate(maxDate);

        // Prevent selecting a date younger than 3 years old
        datePickerDialog.getDatePicker().setMinDate(minDate);

        datePickerDialog.show();
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

        // Check for empty fields
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) ||
                TextUtils.isEmpty(username) || TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(birthday)) {
            Toast.makeText(Register.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Email Format
        if (!isValidEmail(email)) {
            Toast.makeText(Register.this, "Invalid email format!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Password Length Check
        if (!isValidPassword(password)) {
            Toast.makeText(Register.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirm Password Match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(Register.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Create User in Firebase
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

                            // Prevent automatic login after registration
                            Toast.makeText(Register.this, "Account created! Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            finish();
                        }
                    } else {
                        // Handle Errors (E.g., email already in use, network error)
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

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            Toast.makeText(Register.this, "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            Toast.makeText(Register.this, "Password must contain at least one uppercase letter.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            Toast.makeText(Register.this, "Password must contain at least one lowercase letter.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            Toast.makeText(Register.this, "Password must contain at least one number.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()].*")) {
            Toast.makeText(Register.this, "Password must contain at least one special character (!@#$%^&*()).", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Preparation for Google_Signin
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