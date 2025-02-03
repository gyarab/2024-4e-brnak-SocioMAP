package com.example.sociomap2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Button button;
    private TextView textView;
    private FirebaseUser firebaseUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        // Redirect to Login if no user is logged in
        if (firebaseUser == null) {
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize UI elements
        button = view.findViewById(R.id.logout);
        textView = view.findViewById(R.id.user_details);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Fetch and display user info
        if (firebaseUser != null) {
            fetchAndDisplayUserInfo(firebaseUser.getUid());
        }

        // Logout button logic
        button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
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
                        Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }
}
