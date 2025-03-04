package com.example.sociomap2.Login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.Main.MainActivity;
import com.example.sociomap2.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GoogleRegister extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText editTextUsername, editTextSurname, editTextBirthday;
    private Button btnCompleteProfile;
    private ProgressBar progressBar;

    private String userId, email, name;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextUsername = findViewById(R.id.username);
        editTextSurname = findViewById(R.id.surname);
        editTextBirthday = findViewById(R.id.birthday);
        btnCompleteProfile = findViewById(R.id.btn_complete_profile);
        progressBar = findViewById(R.id.progressBar);

        // Get data from intent
        userId = getIntent().getStringExtra("userId");
        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");

        btnCompleteProfile.setOnClickListener(v -> saveUserData());
    }

    private void saveUserData() {
        String username = editTextUsername.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();
        String birthday = editTextBirthday.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(birthday)) {
            Toast.makeText(GoogleRegister.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("name", name);
        userData.put("surname", surname);
        userData.put("birthday", birthday);
        userData.put("email", email);
        userData.put("famous", false);
        userData.put("isAdmin", false);

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GoogleRegister.this, "Profile completed successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GoogleRegister.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


