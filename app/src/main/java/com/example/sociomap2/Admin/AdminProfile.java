package com.example.sociomap2.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.sociomap2.Login.Login;
import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminProfile extends AppCompatActivity {

    private Button btnLogout, btnManageUsers;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        btnLogout = findViewById(R.id.btn_logout);
        btnManageUsers = findViewById(R.id.btn_manage_users);

        // Logout button functionality
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(AdminProfile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        // Redirect to AdminUserList activity
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminProfile.this, AdminUserList.class);
            startActivity(intent);
        });
    }
}





