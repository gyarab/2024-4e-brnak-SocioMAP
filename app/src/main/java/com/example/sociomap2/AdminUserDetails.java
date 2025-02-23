package com.example.sociomap2;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminUserDetails extends AppCompatActivity {

    private static final String TAG = "AdminUserDetails";

    private TextView txtName, txtSurname, txtUsername, txtEmail, txtFamousStatus, txtBanStatus;
    private Button btnToggleBan, btnToggleFamous;
    private FirebaseFirestore firestore;
    private String userId;
    private boolean isBanned = false;
    private boolean isFamous = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_details);

        firestore = FirebaseFirestore.getInstance();

        txtName = findViewById(R.id.txt_name);
        txtSurname = findViewById(R.id.txt_surname);
        txtUsername = findViewById(R.id.txt_username);
        txtEmail = findViewById(R.id.txt_email);
        txtFamousStatus = findViewById(R.id.txt_famous_status);
        txtBanStatus = findViewById(R.id.txt_ban_status);

        btnToggleBan = findViewById(R.id.btn_toggle_ban);
        btnToggleFamous = findViewById(R.id.btn_toggle_famous);

        userId = getIntent().getStringExtra("USER_ID");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserDetails();

        btnToggleBan.setOnClickListener(v -> toggleBanStatus());
        btnToggleFamous.setOnClickListener(v -> toggleFamousStatus());
    }

    private void loadUserDetails() {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        txtName.setText(document.getString("name"));
                        txtSurname.setText(document.getString("surname"));
                        txtUsername.setText(document.getString("username"));
                        txtEmail.setText(document.getString("email"));

                        isBanned = document.contains("ban") && Boolean.TRUE.equals(document.getBoolean("ban"));
                        isFamous = document.contains("isFamous") && Boolean.TRUE.equals(document.getBoolean("isFamous"));

                        updateButtonText();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }

    private void updateButtonText() {
        txtBanStatus.setText(isBanned ? "Status: Banned" : "Status: Not Banned");
        txtFamousStatus.setText(isFamous ? "Famous: Yes" : "Famous: No");

        btnToggleBan.setText(isBanned ? "Unban User" : "Ban User");
        btnToggleFamous.setText(isFamous ? "Unfamous" : "Make Famous");
    }

    private void toggleBanStatus() {
        isBanned = !isBanned;
        firestore.collection("users").document(userId)
                .update("ban", isBanned)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isBanned ? "User Banned" : "User Unbanned", Toast.LENGTH_SHORT).show();
                    updateButtonText();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating ban status", e));
    }

    private void toggleFamousStatus() {
        isFamous = !isFamous;

        firestore.collection("users").document(userId)
                .update("isFamous", isFamous)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isFamous ? "User is now Famous!" : "User is no longer Famous", Toast.LENGTH_SHORT).show();
                    updateButtonText();

                    if (isFamous) {
                        // Fetch user email and send a fame notification email
                        firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists() && document.contains("email")) {
                                        String email = document.getString("email");
                                        if (email != null && !email.isEmpty()) {
                                            sendFameEmail(email);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error fetching email for famous user", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating famous status", e));
    }

    private void sendFameEmail(String recipientEmail) {
        String subject = "Congratulations! You Are Now Famous 🎉";
        String messageBody = "Dear User,\n\nYou have been selected as a Famous User on SocioMap! 🎉\n\n" +
                "Your markers will now be highlighted, and you will receive special privileges.\n\n" +
                "Keep creating amazing events and enjoy your new status!\n\nBest regards,\nSocioMap Team";

        new EmailSender(recipientEmail, subject, messageBody).execute();
    }
}





