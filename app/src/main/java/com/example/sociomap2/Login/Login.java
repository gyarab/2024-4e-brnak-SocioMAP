package com.example.sociomap2.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.sociomap2.Admin.AdminProfile;
import com.example.sociomap2.Main.MainActivity;
import com.example.sociomap2.R;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//Google login
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;



public class Login extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button btnLog;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ProgressBar progressBar;
    TextView textView;

    //Google login
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private Button btnGoogleLogin;

    //Google login video
    private GoogleSignInClient googleSignInClient;
    private ShapeableImageView shapeableImageView;
    private TextView nameT, mail;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount googleSignInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(),null);
                    mAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                mAuth = FirebaseAuth.getInstance();
                                Glide.with(Login.this).load(Objects.requireNonNull(mAuth.getCurrentUser()).getPhotoUrl()).into(shapeableImageView);
                                nameT.setText(mAuth.getCurrentUser().getDisplayName());
                                mail.setText(mAuth.getCurrentUser().getEmail());
                                Toast.makeText(Login.this, "Signed In", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(Login.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(Login.this, "Failed to sign in " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !isFinishing()) {
            checkBanStatus(currentUser.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        btnLog = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.registerNow);

        textView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Register.class);
            startActivity(intent);
            finish();
        });

        btnLog.setOnClickListener(v -> {
            String email = String.valueOf(editTextEmail.getText());
            String password = String.valueOf(editTextPassword.getText());

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Login.this, "No email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Login.this, "No password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(Login.this, "Invalid email format!", Toast.LENGTH_SHORT).show();
                return;
            }


            progressBar.setVisibility(View.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    checkBanStatus(user.getUid());
                                } else {
                                    Toast.makeText(Login.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            Toast.makeText(Login.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Firebase OAuth Client ID
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //  Google Sign-In button
        //btnGoogleLogin = findViewById(R.id.btn_google_login);
        //btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());



        //Google login video
        FirebaseApp.initializeApp(this);
        //shapeableImageView = findViewById(R.id.profileImage);
        //nameT = findViewById(R.id.nameTV);
        //mail = findViewById(R.id.mailTV);

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(Login.this, options);
        mAuth = FirebaseAuth.getInstance();

        //SignInButton signInButton = findViewById(R.id.signIn);
        /*
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });

         */

    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                authenticateWithFirebase(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void authenticateWithFirebase(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkIfUserExists(user);
                        }
                    } else {
                        Toast.makeText(Login.this, "Firebase Authentication failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUserExists(FirebaseUser user) {
        String userId = user.getUid();

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // 🔹 User Exists → Go to MainActivity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // 🔹 New User → Redirect to GoogleRegister
                    Intent intent = new Intent(getApplicationContext(), GoogleRegister.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("email", user.getEmail());
                    intent.putExtra("name", user.getDisplayName());
                    startActivity(intent);
                    finish();
                }
            }
        });
    }




    private void showResendVerificationDialog(FirebaseUser user) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Would you like to resend the verification email?")
                .setPositiveButton("Resend", (dialog, which) -> {
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(Login.this, "Verification email resent! Check your inbox.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(Login.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Check if the user is banned before allowing login.
     */
    private void checkBanStatus(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Boolean isBanned = document.getBoolean("ban");
                            if (isBanned != null && isBanned) {
                                Toast.makeText(Login.this, "Your account has been banned.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                return;
                            }
                            checkAdminStatus(userId);
                        } else {
                            Log.d("Login", "User not found in Firestore, creating user...");
                            createUserInFirestore(userId);
                        }
                    } else {
                        Log.d("Login", "Firestore check failed", task.getException());
                    }
                });
    }

    private void createUserInFirestore(String userId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Retrieve stored user data
        SharedPreferences prefs = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        String username = prefs.getString("username", "NewUser");
        String name = prefs.getString("name", "");
        String surname = prefs.getString("surname", "");
        String birthday = prefs.getString("birthday", "");
        String email = user.getEmail();

        // Store in Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("name", name);
        userData.put("surname", surname);
        userData.put("birthday", birthday);
        userData.put("email", email);
        userData.put("famous", false);
        userData.put("isAdmin", false);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User data created in Firestore.");
                    // ✅ Clear stored user data after saving -------------------------------------
                    ((SharedPreferences) prefs).edit().clear().apply();
                    checkAdminStatus(userId);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error creating user in Firestore", e));
    }


    /**
     * Check if the user is an admin or a regular user.
     */
    private void checkAdminStatus(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Boolean isAdmin = document.getBoolean("isAdmin");
                            Intent intent;
                            if (isAdmin != null && isAdmin) {
                                // 👑 Redirect to Admin Interface
                                intent = new Intent(getApplicationContext(), AdminProfile.class);
                            } else {
                                // 🔓 Redirect to Main Activity for regular users
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("Login", "No such document");
                        }
                    } else {
                        Log.d("Login", "get failed with ", task.getException());
                    }
                });
    }
}