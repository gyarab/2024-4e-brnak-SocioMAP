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
    private List<String> userIdList = new ArrayList<>();
    private List<String> fullUserDisplayList = new ArrayList<>(); // Full copy of the list
    private List<String> fullUserIdList = new ArrayList<>(); // Full copy of user IDs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        firestore = FirebaseFirestore.getInstance();

        userListView = findViewById(R.id.list_users);
        searchBar = findViewById(R.id.search_bar);

        userAdapter = new ArrayAdapter<>(this, R.layout.item_user_admin, userDisplayList);
        userListView.setAdapter(userAdapter);

        loadUsers();

        // Search bar listener
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
            String selectedUserId = userIdList.get(position);
            openUserDetails(selectedUserId);
        });
    }

    private void loadUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userDisplayList.clear();
                    userIdList.clear();
                    fullUserDisplayList.clear();
                    fullUserIdList.clear(); // Reset full lists

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();
                        String name = document.getString("name");
                        String surname = document.getString("surname");
                        String username = document.getString("username");

                        String displayText = formatUserDisplay(name, surname, username);
                        userDisplayList.add(displayText);
                        userIdList.add(userId);

                        // Save copies for resetting the search
                        fullUserDisplayList.add(displayText);
                        fullUserIdList.add(userId);
                    }

                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching users", e);
                    Toast.makeText(AdminUserList.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {

            userDisplayList.clear();
            userDisplayList.addAll(fullUserDisplayList);
            userIdList.clear();
            userIdList.addAll(fullUserIdList);
        } else {
            List<String> filteredList = new ArrayList<>();
            List<String> filteredIds = new ArrayList<>();

            for (int i = 0; i < fullUserDisplayList.size(); i++) {
                if (fullUserDisplayList.get(i).toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(fullUserDisplayList.get(i));
                    filteredIds.add(fullUserIdList.get(i));
                }
            }

            userDisplayList.clear();
            userDisplayList.addAll(filteredList);
            userIdList.clear();
            userIdList.addAll(filteredIds);
        }

        userAdapter.notifyDataSetChanged();
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