package com.example.sociomap2.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminUserList extends AppCompatActivity {

    private static final String TAG = "AdminUserList";

    private ListView userListView;
    private EditText searchBar;
    private FirebaseFirestore firestore;
    private ArrayAdapter<String> userAdapter;
    private List<String> userDisplayList = new ArrayList<>();
    private List<String> userIdList = new ArrayList<>(); // Store user IDs for navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        firestore = FirebaseFirestore.getInstance();

        userListView = findViewById(R.id.list_users);
        searchBar = findViewById(R.id.search_bar);

        userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userDisplayList);
        userListView.setAdapter(userAdapter);

        loadUsers();

        // Filter users when typing in the search bar
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle user click
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUserId = userIdList.get(position); // Get the corresponding user ID
            openUserDetails(selectedUserId);
        });
    }

    private void loadUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userDisplayList.clear();
                    userIdList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();
                        String name = document.getString("name");
                        String surname = document.getString("surname");
                        String username = document.getString("username");

                        String displayText = formatUserDisplay(name, surname, username);
                        userDisplayList.add(displayText);
                        userIdList.add(userId); // Store user ID

                        userAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching users", e);
                    Toast.makeText(AdminUserList.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        List<String> filteredList = new ArrayList<>();
        List<String> filteredIds = new ArrayList<>();

        for (int i = 0; i < userDisplayList.size(); i++) {
            if (userDisplayList.get(i).toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(userDisplayList.get(i));
                filteredIds.add(userIdList.get(i));
            }
        }

        userAdapter.clear();
        userAdapter.addAll(filteredList);
        userAdapter.notifyDataSetChanged();

        userIdList = filteredIds; // Update filtered user IDs
    }

    private void openUserDetails(String userId) {
        Intent intent = new Intent(AdminUserList.this, AdminUserDetails.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private String formatUserDisplay(String name, String surname, String username) {
        String displayText = "";
        if (name != null && !name.isEmpty()) {
            displayText += name + " ";
        }
        if (surname != null && !surname.isEmpty()) {
            displayText += surname;
        }
        if (username != null && !username.isEmpty()) {
            displayText += " (@" + username + ")";
        }

        return displayText.isEmpty() ? "Unknown User" : displayText;
    }
}






