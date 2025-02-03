package com.example.sociomap2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Profile extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseFirestore db;
    Button button;
    TextView textView;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

         */

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        //button = findViewById(R.id.logout);
        //textView = findViewById(R.id.user_details);
        firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            // Fetch and display user information
            fetchAndDisplayUserInfo(firebaseUser.getUid());
        }

        button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void fetchAndDisplayUserInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve and display user information
                        String email = documentSnapshot.getString("email");
                        String username = documentSnapshot.getString("username");
                        String name = documentSnapshot.getString("name");
                        String surname = documentSnapshot.getString("surname");
                        String birthyear = documentSnapshot.getString("birthyear");
                        Boolean isFamous = documentSnapshot.getBoolean("famous");
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");

                        // Set the user details in the TextView
                        textView.setText("Email: " + email + "\n"
                                + "Username: " + username + "\n"
                                + "Name: " + name + " " + surname + "\n"
                                + "Birth Year: " + birthyear + "\n"
                                + "Famous: " + isFamous + "\n"
                                + "Admin: " + isAdmin);
                    } else {
                        Toast.makeText(Profile.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Profile.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }
}
