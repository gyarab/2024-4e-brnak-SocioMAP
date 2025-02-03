package com.example.sociomap2;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the initial fragment when activity starts
        if (savedInstanceState == null) {
            loadFragment(new ProfileFragment());
        }

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.nav_map) {
                selectedFragment = new MapsFragment();
            } else if (item.getItemId() == R.id.nav_other) {
                selectedFragment = new OtherFragment();
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });


    }

    private void loadFragment(Fragment fragment) {
        // Load fragment into the fragment_container
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
