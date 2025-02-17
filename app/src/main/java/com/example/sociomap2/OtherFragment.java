package com.example.sociomap2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OtherFragment extends Fragment {

    private static final String TAG = "OtherFragment";
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;

    private ListView listSignedEvents;
    private ArrayAdapter<String> signedAdapter;

    private List<String> signedEvents = new ArrayList<>();
    private List<String> eventIds = new ArrayList<>(); // Store event IDs for navigation

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other, container, false);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize ListView
        listSignedEvents = view.findViewById(R.id.list_signed_events);
        signedAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, signedEvents);
        listSignedEvents.setAdapter(signedAdapter);

        loadSignedEvents();

        // Handle click on signed events to open MarkerInfoActivity
        listSignedEvents.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedEventId = eventIds.get(position); // Get event ID
            openMarkerInfo(selectedEventId);
        });

        return view;
    }

    private void loadSignedEvents() {
        firestore.collection("user_events").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    signedEvents.clear();
                    eventIds.clear();

                    if (document.exists() && document.getData() != null) {
                        Map<String, Object> events = document.getData();

                        for (Map.Entry<String, Object> entry : events.entrySet()) {
                            String eventId = entry.getKey();
                            Object value = entry.getValue();

                            if (value instanceof Boolean && (Boolean) value) { // Ensure it's a true signup
                                fetchEventDetails(eventId);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading signed events", e));
    }

    private void fetchEventDetails(String eventId) {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getData() != null) {
                        String eventName = document.getString("name");
                        String eventDate = document.getString("date");

                        if (eventName != null && eventDate != null) {
                            signedEvents.add(eventName + " - " + eventDate);
                            eventIds.add(eventId); // Store event ID for navigation
                            signedAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching event details", e));
    }

    private void openMarkerInfo(String eventId) {
        Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }
}
