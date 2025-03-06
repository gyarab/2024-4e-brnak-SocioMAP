package com.example.sociomap2.Main;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.sociomap2.Main.Map.MapsFragment;
import com.example.sociomap2.Main.Other.OtherFragment;
import com.example.sociomap2.Main.Profile.ProfileFragment;
import com.example.sociomap2.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isFamous = false; // Default: Not famous

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            checkIfFamous(user.getUid());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_map) {
                selectedFragment = new MapsFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.nav_other) {
                selectedFragment = new OtherFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MapsFragment())
                    .commit();

            bottomNav.setSelectedItemId(R.id.nav_map);

        }

    }

    private void checkIfFamous(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("isFamous")) {
                        isFamous = document.getBoolean("isFamous") != null && document.getBoolean("isFamous");

                        if (isFamous) {
                            applyFamousTheme();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching famous status", e));
    }

    private void applyFamousTheme() {
        // Change bottom navigation background
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setBackgroundColor(Color.parseColor("#ADD8E6")); // Light Blue

        // Update fragments
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MapsFragment())
                .commit();
    }
}