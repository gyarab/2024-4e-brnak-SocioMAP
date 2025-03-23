package com.example.sociomap2.Main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;

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
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            checkIfFamous(user.getUid());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String fragmentTag = "MapsFragment"; // Default

            if (item.getItemId() == R.id.nav_map) {
                selectedFragment = new MapsFragment();
                fragmentTag = "MapsFragment";
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                fragmentTag = "ProfileFragment";
            } else if (item.getItemId() == R.id.nav_other) {
                selectedFragment = new OtherFragment();
                fragmentTag = "OtherFragment";
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

                // Save selected fragment
                sharedPreferences.edit().putString("last_fragment", fragmentTag).apply();
            }
            return true;
        });

        // Restore last opened fragment after theme change
        if (savedInstanceState == null) {
            String lastFragment = sharedPreferences.getString("last_fragment", "MapsFragment");
            Fragment fragmentToLoad;

            switch (lastFragment) {
                case "ProfileFragment":
                    fragmentToLoad = new ProfileFragment();
                    bottomNav.setSelectedItemId(R.id.nav_profile);
                    break;
                case "OtherFragment":
                    fragmentToLoad = new OtherFragment();
                    bottomNav.setSelectedItemId(R.id.nav_other);
                    break;
                default:
                    fragmentToLoad = new MapsFragment();
                    bottomNav.setSelectedItemId(R.id.nav_map);
                    break;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragmentToLoad)
                    .commit();
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
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // background color to Light Blue
        bottomNav.setBackgroundColor(Color.parseColor("#ADD8E6"));

        // icon and text color to Black
        bottomNav.setItemIconTintList(ColorStateList.valueOf(Color.BLACK));
        bottomNav.setItemTextColor(ColorStateList.valueOf(Color.BLACK));

        // item color to a visible one
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked}, // Selected state
                        new int[]{-android.R.attr.state_checked} // Default state
                },
                new int[]{
                        Color.parseColor("#FFFFFF"), // 🔹 Dark Blue when selected
                        Color.BLACK // ⚫ Black for unselected items
                }
        );

        bottomNav.setItemIconTintList(colorStateList);
    }
}