package com.example.sociomap2.Main.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sociomap2.Login.Login;
import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;

    private TextView emailText, usernameText, nameText, birthYearText, famousText, adminText;
    private Button logoutButton, editButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        emailText = view.findViewById(R.id.text_email);
        usernameText = view.findViewById(R.id.text_username);
        nameText = view.findViewById(R.id.text_name);
        birthYearText = view.findViewById(R.id.text_birthyear);
        famousText = view.findViewById(R.id.text_famous);
        adminText = view.findViewById(R.id.text_admin);

        logoutButton = view.findViewById(R.id.logout);
        editButton = view.findViewById(R.id.edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        if (firebaseUser != null) {
            fetchAndDisplayUserInfo(firebaseUser.getUid());
        }

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Edit button logic
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void fetchAndDisplayUserInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        emailText.setText(documentSnapshot.getString("email"));
                        usernameText.setText(documentSnapshot.getString("username"));
                        nameText.setText(documentSnapshot.getString("name") + " " + documentSnapshot.getString("surname"));
                        birthYearText.setText(documentSnapshot.getString("birthyear"));
                        famousText.setText(documentSnapshot.getBoolean("famous") ? "Yes" : "No");
                        adminText.setText(documentSnapshot.getBoolean("isAdmin") ? "Yes" : "No");
                    } else {
                        Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }
}