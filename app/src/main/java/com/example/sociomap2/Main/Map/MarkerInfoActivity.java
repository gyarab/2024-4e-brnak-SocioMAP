package com.example.sociomap2.Main.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sociomap2.EmailSender;
import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkerInfoActivity extends AppCompatActivity {

    private static final String TAG = "MarkerInfoActivity";

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String eventId;
    private TextView eventName, eventDate, eventDescription, eventTheme, eventAgeLimit;
    private Button signUpButton, deleteButton, editButton, reportButton;
    private ListView listAttendees;
    private boolean isUserSignedUp = false;
    private ArrayAdapter<String> attendeesAdapter;
    private List<String> attendeesList = new ArrayList<>();

    private TextView eventCapacity;
    private int maxCapacity = 0;
    private int currentAttendees = 0;
    private boolean hasReported = false;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        eventName = findViewById(R.id.event_name);
        eventDate = findViewById(R.id.event_date);
        eventDescription = findViewById(R.id.event_description);
        eventTheme = findViewById(R.id.event_theme);
        eventCapacity = findViewById(R.id.event_capacity);
        signUpButton = findViewById(R.id.btn_sign_up);
        deleteButton = findViewById(R.id.btn_delete_event);
        editButton = findViewById(R.id.btn_edit_event);
        listAttendees = findViewById(R.id.list_attendees);
        reportButton = findViewById(R.id.btn_report_event);
        reportButton.setOnClickListener(v -> showReportConfirmationDialog());


        attendeesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, attendeesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }
        };
        listAttendees.setAdapter(attendeesAdapter);

        eventAgeLimit = findViewById(R.id.event_age_limit);

        eventId = getIntent().getStringExtra("MARKER_ID");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.enableNetwork()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firestore network re-enabled.");
                    } else {
                        Log.e(TAG, "Failed to enable Firestore network", task.getException());
                    }
                });

        loadEventDetails();
        loadAttendeesList();
        checkIfUserIsOwner();

        signUpButton.setOnClickListener(v -> {
            if (isUserSignedUp) {
                signOutFromEvent();
            } else {
                signUpForEvent();
            }
        });
    }

    private void checkIfUserIsOwner() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String ownerId = document.getString("userId"); // Get event owner ID
                        String currentUserId = firebaseAuth.getCurrentUser().getUid();

                        if (ownerId != null && ownerId.equals(currentUserId)) {
                            //  Show buttons if the user is the owner
                            deleteButton.setVisibility(View.VISIBLE);
                            editButton.setVisibility(View.VISIBLE);

                            // Set button listeners
                            deleteButton.setOnClickListener(v -> deleteEvent());
                            editButton.setOnClickListener(v -> editEvent());
                        } else {
                            //  Hide buttons if not the owner
                            deleteButton.setVisibility(View.GONE);
                            editButton.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking event ownership", e));
    }


    private void deleteEvent() {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> userIds = (List<String>) document.get("users");
                        if (userIds != null) {
                            for (String userId : userIds) {
                                removeEventFromUserEvents(userId);
                            }
                        }
                    }

                    // Delete the event from Firestore collections
                    firestore.collection("markers").document(eventId).delete();
                    firestore.collection("event_guest_list").document(eventId).delete();

                    Toast.makeText(this, "Event deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting event", e));
    }

    public void editEvent() {
        if (eventId != null && !eventId.isEmpty()) {
            Intent intent = new Intent(this, EditMarkerActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeEventFromUserEvents(String userId) {
        firestore.collection("user_events").document(userId)
                .update("events", FieldValue.arrayRemove(eventId))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing event from user_events", e));
    }

    private void loadEventDetails() {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        eventName.setText(document.getString("title"));
                        eventDate.setText(document.getString("eventDateTime"));
                        eventDescription.setText(document.getString("description"));

                        // Age limit
                        int ageLimit = document.contains("ageLimit") ? document.getLong("ageLimit").intValue() : 3;
                        eventAgeLimit.setText("Minimum Age: " + ageLimit + "+");

                        // Load and display the event theme
                        String theme = document.getString("theme");
                        if (theme != null) {
                            eventTheme.setText("Theme: " + theme);
                        } else {
                            eventTheme.setText("Theme: Not specified");
                        }

                        // Get capacity details
                        maxCapacity = document.contains("maxCapacity") ? document.getLong("maxCapacity").intValue() : 0;
                        currentAttendees = document.contains("currentAttendees") ? document.getLong("currentAttendees").intValue() : 0;

                        // Display capacity info
                        eventCapacity.setText("Capacity: " + currentAttendees + "/" + maxCapacity);

                        // Button disable
                        String eventDateTime = document.getString("eventDateTime");

                        if (eventDateTime != null && eventDateTime.contains(" ")) {
                            String[] parts = eventDateTime.split(" ");
                            String datePart = parts[0];
                            String timePart = parts[1];

                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                Date eventDateParsed = sdf.parse(eventDateTime);
                                if (eventDateParsed != null && eventDateParsed.before(new Date())) {
                                    // Událost je v minulosti – skrýt tlačítka kromě report
                                    signUpButton.setVisibility(View.GONE);
                                    deleteButton.setVisibility(View.GONE);
                                    editButton.setVisibility(View.GONE);
                                }
                            } catch (Exception e) {
                                Log.e("MarkerInfo", "Date parsing error", e);
                            }

                            eventDate.setText(eventDateTime);
                        }

                        checkUserParticipation(); // Check if user is signed up
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading event details.", Toast.LENGTH_SHORT).show());
    }


    private void loadAttendeesList() {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    attendeesList.clear();
                    if (document.exists()) {
                        List<String> userIds = (List<String>) document.get("users");
                        if (userIds != null) {
                            for (String userId : userIds) {
                                fetchUserDetails(userId); // Fetch name, surname, and username
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading attendees", e));
    }

    //  Fetch `name`, `surname`, and `username` from Firestore
    private void fetchUserDetails(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        String surname = userDoc.getString("surname");
                        String username = userDoc.getString("username");

                        // Format: "Name Surname (@username)"
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

                        if (displayText.isEmpty()) {
                            displayText = "Unknown User"; // Fallback if missing
                        }

                        attendeesList.add(displayText);
                        attendeesAdapter.notifyDataSetChanged(); // Refresh ListView
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }

    private void checkUserParticipation() {
        firestore.collection("user_events").document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> joinedEvents = (List<String>) document.get("events");
                        if (joinedEvents != null && joinedEvents.contains(eventId)) {
                            isUserSignedUp = true;
                            signUpButton.setText("Sign Out from Event");
                        } else {
                            isUserSignedUp = false;
                            signUpButton.setText("Sign Up for Event");
                        }
                    }

                    // Disable button if full
                    if (currentAttendees >= maxCapacity) {
                        signUpButton.setEnabled(false);
                        signUpButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking user participation", e));
    }


    private void signUpForEvent() {
        if (currentAttendees >= maxCapacity) {
            Toast.makeText(this, "This event is already full!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        // Fetch the username from Firestore (instead of using getDisplayName())
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String username = userDoc.getString("username");

                        if (username == null || username.trim().isEmpty()) {
                            Toast.makeText(this, "Error: Username is missing.", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        //  Add event ID to user's `user_events` document
                        firestore.collection("user_events").document(userId)
                                .update("events", FieldValue.arrayUnion(eventId))
                                .addOnFailureListener(e -> {
                                    Map<String, Object> userEventsData = new HashMap<>();
                                    userEventsData.put("events", new ArrayList<String>() {{
                                        add(eventId);
                                    }});
                                    firestore.collection("user_events").document(userId).set(userEventsData);
                                });

                        // Add user ID & username to `event_guest_list` document
                        firestore.collection("event_guest_list").document(eventId)
                                .update("users", FieldValue.arrayUnion(userId), userId, username) // Store both ID & username
                                .addOnFailureListener(e -> {
                                    Map<String, Object> eventGuestData = new HashMap<>();
                                    eventGuestData.put("users", new ArrayList<String>() {{
                                        add(userId);
                                    }});
                                    eventGuestData.put(userId, username); // Store the username
                                    firestore.collection("event_guest_list").document(eventId).set(eventGuestData);
                                });

                        // Update attendee count in Firestore
                        firestore.collection("markers").document(eventId)
                                .update("currentAttendees", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Signed up for the event!", Toast.LENGTH_SHORT).show();
                                    currentAttendees++;
                                    eventCapacity.setText("Capacity: " + currentAttendees + "/" + maxCapacity);
                                    checkUserParticipation();
                                    loadAttendeesList();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating attendee count", e));

                    } else {
                        Toast.makeText(this, "Error: User details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }

    private void signOutFromEvent() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        // Remove event ID from users `user_events` document
        firestore.collection("user_events").document(userId)
                .update("events", FieldValue.arrayRemove(eventId));

        // Remove user ID from events `event_guest_list` document
        firestore.collection("event_guest_list").document(eventId)
                .update("users", FieldValue.arrayRemove(userId));

        // Decrease attendee count in Firestore
        firestore.collection("markers").document(eventId)
                .update("currentAttendees", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Signed out from event!", Toast.LENGTH_SHORT).show();
                    currentAttendees--; // Update local variable
                    eventCapacity.setText("Capacity: " + currentAttendees + "/" + maxCapacity);
                    checkUserParticipation(); // Refresh button state
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating attendee count", e));
    }

    private void showReportConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Report Event")
                .setMessage("Are you sure you want to report this event?")
                .setPositiveButton("Yes, Report", (dialog, which) -> sendReportEmail())  // ✅ If confirmed, send email
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendReportEmail() {
        if (hasReported) {
            Toast.makeText(this, "You have already reported this event.", Toast.LENGTH_SHORT).show();
            return; // Prevents multiple reports
        }

        String me = "tobik.brnak@gmail.com";
        String eventTitle = eventName.getText().toString();
        String eventDateTime = eventDate.getText().toString();
        String reportReason = "User reported this event as inappropriate.";

        String subject = "⚠️ Event Report: " + eventTitle;
        String messageBody = "🚨 Reported Event Details:\n\n"
                + "📌 Event: " + eventTitle + "\n"
                + "📅 Date: " + eventDateTime + "\n"
                + "🆔 Event ID: " + eventId + "\n"
                + "📢 Reason: " + reportReason;

        // Fetch all admin emails from Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> emailRecipients = new ArrayList<>();
                    emailRecipients.add(me); // Always send to you

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Boolean isAdmin = document.getBoolean("isAdmin");
                        String email = document.getString("email");

                        if (Boolean.TRUE.equals(isAdmin) && email != null) {
                            emailRecipients.add(email);
                        }
                    }

                    // Convert email list to array
                    String[] recipientsArray = emailRecipients.toArray(new String[0]);

                    // Send email to all recipients
                    new EmailSender(recipientsArray, subject, messageBody).execute();
                    Toast.makeText(this, "Report sent successfully!", Toast.LENGTH_LONG).show();

                    // Mark as reported to prevent multiple clicks
                    hasReported = true;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch admin emails.", Toast.LENGTH_SHORT).show();
                    Log.e("sendReportEmail", "Error fetching admin emails", e);
                });
    }


}