package com.example.sociomap2.Main.Other;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.R;
import com.example.sociomap2.User;
import com.example.sociomap2.UserListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchUsersActivity extends AppCompatActivity {

    private static final String TAG = "SearchUsersActivity";

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;

    private EditText edtSearch;
    private ListView listViewUsers;
    private UserListAdapter adapter;
    private List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

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

        edtSearch = findViewById(R.id.edt_search);
        listViewUsers = findViewById(R.id.list_users);
        adapter = new UserListAdapter(this, userList);
        listViewUsers.setAdapter(adapter);

        loadAllUsers();

        // Search Functionality
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllUsers() {
        firestore.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!document.getId().equals(userId)) { // Exclude current user
                            String id = document.getId();
                            String name = document.getString("name");
                            String surname = document.getString("surname");
                            String username = document.getString("username");

                            userList.add(new User(id, name, surname, username));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching users", e));
    }

    private void filterUsers(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : userList) {
            if (user.matchesSearch(query)) {
                filteredList.add(user);
            }
        }
        adapter.updateList(filteredList);
    }
}







