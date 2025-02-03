package com.example.sociomap2;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MarkerInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        // Get marker data from intent
        String title = getIntent().getStringExtra("TITLE");
        String description = getIntent().getStringExtra("DESCRIPTION");

        // Set data to views
        ((TextView) findViewById(R.id.txt_title)).setText(title);
        ((TextView) findViewById(R.id.txt_description)).setText(description);

        // Add logic for "assign" button
        findViewById(R.id.btn_assign).setOnClickListener(v -> {
            // Perform the assign logic (e.g., update Firestore)
            finish();
        });
    }
}