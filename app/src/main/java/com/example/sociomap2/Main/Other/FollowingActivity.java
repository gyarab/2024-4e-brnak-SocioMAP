package com.example.sociomap2.Main.Other;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowingActivity extends AppCompatActivity {

    private static final String TAG = "FollowingActivity";

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;

    private ListView listFollowingSigned, listFollowingCreated;
    private ArrayAdapter<String> signedAdapter, createdAdapter;
    private List<String> followingSigned = new ArrayList<>();
    private List<String> followingCreated = new ArrayList<>();

    private Button btnSearchUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ListViews
        listFollowingSigned = findViewById(R.id.list_following_signed);
        listFollowingCreated = findViewById(R.id.list_following_created);
        btnSearchUsers = findViewById(R.id.btn_search_users);

        // Initialize Adapters
        signedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, followingSigned);
        createdAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, followingCreated);

        // Set adapters to ListViews
        listFollowingSigned.setAdapter(signedAdapter);
        listFollowingCreated.setAdapter(createdAdapter);

        // Load Following Lists
        loadFollowingSigned();
        loadFollowingCreated();

        // Button to Open SearchUsersActivity
        btnSearchUsers.setOnClickListener(v -> {
            Intent intent = new Intent(FollowingActivity.this, SearchUsersActivity.class);
            startActivity(intent);
        });
    }

    private void loadFollowingSigned() {
        firestore.collection("user_signup_follow").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    followingSigned.clear();

                    if (document.exists()) {
                        // ✅ Extract the "following" array correctly
                        List<String> followedUsers = (List<String>) document.get("following");
                        if (followedUsers != null) {
                            for (String followedUserId : followedUsers) {
                                fetchUserDetails(followedUserId, followingSigned, signedAdapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading following signed-up users", e));
    }

    private void loadFollowingCreated() {
        firestore.collection("user_create_follow").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    followingCreated.clear();

                    if (document.exists()) {
                        // Extract the "following" array correctly
                        List<String> followedUsers = (List<String>) document.get("following");
                        if (followedUsers != null) {
                            for (String followedUserId : followedUsers) {
                                fetchUserDetails(followedUserId, followingCreated, createdAdapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading following created users", e));
    }

    private void fetchUserDetails(String userId, List<String> userList, ArrayAdapter<String> adapter) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        String surname = userDoc.getString("surname");
                        String username = userDoc.getString("username");

                        String displayText = (name != null ? name + " " : "") +
                                (surname != null ? surname : "") +
                                (username != null ? " (@" + username + ")" : "");

                        if (displayText.isEmpty()) {
                            displayText = "Unknown User";
                        }

                        userList.add(displayText);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }
}