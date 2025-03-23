package com.example.sociomap2.Main.Profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sociomap2.EmailSender;
import com.example.sociomap2.Login.Login;
import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;

    private TextView emailText, usernameText, nameText, birthYearText;
    private Button logoutButton, editButton, btnSelectThemes, btnContactAdmins;
    private Switch themeSwitch;
    private SharedPreferences sharedPreferences;
    private ImageView profileImage;

    int accentColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        // ✅ Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("Settings", 0);

        // ✅ Load theme from SharedPreferences and apply
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO);

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
        profileImage = view.findViewById(R.id.profile_image);
        logoutButton = view.findViewById(R.id.logout);
        editButton = view.findViewById(R.id.edit_profile);
        //themeSwitch = view.findViewById(R.id.theme_switch);
        btnSelectThemes = view.findViewById(R.id.btn_select_themes);
        btnSelectThemes.setOnClickListener(v -> openThemeSelectionDialog());
        btnContactAdmins = view.findViewById(R.id.btn_contact_admins);
        btnContactAdmins.setOnClickListener(v -> promptAndSendCustomMessageToAdmins());

        // OUT - Set the switch to the correct initial state
        /*
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        themeSwitch.setChecked(isDarkMode);

         */

        // OUT - accent color based on theme
        /*
        accentColor = isDarkMode ?
                ContextCompat.getColor(requireContext(), R.color.colorAccentDark) :
                ContextCompat.getColor(requireContext(), R.color.colorAccentLight);

         */

        editButton.setBackgroundColor(accentColor);
        logoutButton.setBackgroundColor(accentColor);

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
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

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        /*  listener to theme switch
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            applyTheme(isChecked);
        });
         */

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
                        // ✅ Check if the user is famous
                        Boolean isFamous = documentSnapshot.getBoolean("isFamous");
                        if (isFamous != null && isFamous) {
                            profileImage.setImageResource(R.drawable.crown_icon); // Replace with your crown drawable

                        } else {
                            profileImage.setImageResource(R.drawable.default_profile); // Default profile image
                            profileImage.clearColorFilter();
                        }

                    } else {
                        Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });

    }

    private void openThemeSelectionDialog() {
        String[] themes = {"Sports", "Music", "Festival", "Concert", "Custom"};
        boolean[] checkedItems = new boolean[themes.length]; // Track selections
        List<String> selectedThemes = new ArrayList<>();

        // Fetch existing preferredThemes from Firestore
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("preferredThemes")) {
                        List<String> existingThemes = (List<String>) document.get("preferredThemes");
                        for (int i = 0; i < themes.length; i++) {
                            if (existingThemes.contains(themes[i])) {
                                checkedItems[i] = true;
                                selectedThemes.add(themes[i]);
                            }
                        }
                    }

                    // Create AlertDialog with checkboxes
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Select Preferred Themes")
                            .setMultiChoiceItems(themes, checkedItems, (dialog, index, isChecked) -> {
                                if (isChecked) {
                                    selectedThemes.add(themes[index]);
                                } else {
                                    selectedThemes.remove(themes[index]);
                                }
                            })
                            .setPositiveButton("Submit", (dialog, which) -> {
                                savePreferredThemes(selectedThemes);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void savePreferredThemes(List<String> selectedThemes) {
        db.collection("users").document(firebaseUser.getUid())
                .update("preferredThemes", selectedThemes)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Preferences Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update preferences.", Toast.LENGTH_SHORT).show());
    }


    private void promptAndSendCustomMessageToAdmins() {
        // Vytvoř input pole
        final EditText input = new EditText(requireContext());
        input.setHint("Write your message here");
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Message to Admins")
                .setMessage("Write a message you'd like to send to the admin team (Famous status):")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String userMessage = input.getText().toString().trim();

                    if (userMessage.isEmpty()) {
                        Toast.makeText(getContext(), "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String me = "tobik.brnak@gmail.com";
                    String subject = "New User Message to Admins";
                    String messageBody = "Hello Admins,\n\nA user sent you a message from their profile:\n\n"
                            + "User: " + firebaseUser.getEmail() + "\n\n"
                            + "Message:\n" + userMessage + "\n\n"
                            + "- SocioMap System";

                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    firestore.collection("users")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<String> emailRecipients = new ArrayList<>();
                                emailRecipients.add(me); // Send to you always

                                for (DocumentSnapshot document : queryDocumentSnapshots) {
                                    Boolean isAdmin = document.getBoolean("isAdmin");
                                    String email = document.getString("email");

                                    if (Boolean.TRUE.equals(isAdmin) && email != null) {
                                        emailRecipients.add(email);
                                    }
                                }

                                String[] recipientsArray = emailRecipients.toArray(new String[0]);
                                new EmailSender(recipientsArray, subject, messageBody).execute();
                                Toast.makeText(getContext(), "Message sent to admins!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to fetch admin emails.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    /*
    private void applyTheme(boolean isDarkMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int newMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

        if (AppCompatDelegate.getDefaultNightMode() != newMode) {
            editor.putBoolean("dark_mode", isDarkMode);
            editor.apply();

            accentColor = isDarkMode ?
                    ContextCompat.getColor(requireContext(), R.color.colorAccentDark) :
                    ContextCompat.getColor(requireContext(), R.color.colorAccentLight);

            editButton.setBackgroundColor(accentColor);
            logoutButton.setBackgroundColor(accentColor);

            requireActivity().runOnUiThread(() -> AppCompatDelegate.setDefaultNightMode(newMode));
        }
    }

     */
}



