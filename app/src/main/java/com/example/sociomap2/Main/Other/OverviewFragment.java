package com.example.sociomap2.Main.Other;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sociomap2.Main.Map.MarkerInfoActivity;
import com.example.sociomap2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private static final String TAG = "OtherFragment";
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;

    private ListView listSignedEvents, listCreatedEvents, listArchivedEvents, listArchivedCreatedEvents;
    private ArrayAdapter<String> signedAdapter, createdAdapter, archivedAdapter, archivedCreatedAdapter;
    private Button btnManageFollowing; // ✅ New button for following feature

    private List<String> signedEvents = new ArrayList<>();
    private List<String> createdEvents = new ArrayList<>();
    private List<String> archivedEvents = new ArrayList<>();
    private List<String> archivedCreatedEvents = new ArrayList<>();

    private List<String> signedEventIds = new ArrayList<>();
    private List<String> createdEventIds = new ArrayList<>();
    private List<String> archivedEventIds = new ArrayList<>();
    private List<String> archivedCreatedEventIds = new ArrayList<>();

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

        // Initialize ListViews
        listSignedEvents = view.findViewById(R.id.list_signed_events);
        listCreatedEvents = view.findViewById(R.id.list_created_events);
        listArchivedEvents = view.findViewById(R.id.list_archived_events);
        listArchivedCreatedEvents = view.findViewById(R.id.list_archived_created_events);
        btnManageFollowing = view.findViewById(R.id.btn_manage_following); // ✅ Initialize button

        // Initialize Adapters
        signedAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, signedEvents);
        createdAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, createdEvents);
        archivedAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, archivedEvents);
        archivedCreatedAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, archivedCreatedEvents);

        // Set adapters to ListViews
        listSignedEvents.setAdapter(signedAdapter);
        listCreatedEvents.setAdapter(createdAdapter);
        listArchivedEvents.setAdapter(archivedAdapter);
        listArchivedCreatedEvents.setAdapter(archivedCreatedAdapter);

        // Load data
        loadSignedEvents();
        loadCreatedEvents();
        loadArchivedEvents();
        loadArchivedCreatedEvents();

        // Handle click events
        listSignedEvents.setOnItemClickListener((parent, view1, position, id) -> openMarkerInfo(signedEventIds.get(position)));
        listCreatedEvents.setOnItemClickListener((parent, view1, position, id) -> openMarkerInfo(createdEventIds.get(position)));
        listArchivedEvents.setOnItemClickListener((parent, view1, position, id) -> openMarkerInfoArch(archivedEventIds.get(position)));
        listArchivedCreatedEvents.setOnItemClickListener((parent, view1, position, id) -> openMarkerInfoArch(archivedCreatedEventIds.get(position)));

        // Open FollowingActivity when clicked
        btnManageFollowing.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FollowingActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadSignedEvents() {
        firestore.collection("user_events").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    signedEvents.clear();
                    signedEventIds.clear();

                    if (document.exists() && document.getData() != null) {
                        List<String> events = (List<String>) document.get("events");
                        if (events != null) {
                            for (String eventId : events) {
                                fetchEventDetails(eventId, signedEvents, signedEventIds, signedAdapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading signed events", e));
    }

    private void loadCreatedEvents() {
        firestore.collection("markers")
                .whereEqualTo("userId", userId)  // ✅ Find markers where the user is the owner
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    createdEvents.clear();
                    createdEventIds.clear();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String eventId = document.getId();
                        String eventName = document.getString("title");
                        String eventDate = document.getString("eventDateTime");

                        if (eventName != null && eventDate != null) {
                            createdEvents.add(eventName + " - " + eventDate);
                            createdEventIds.add(eventId);
                        }
                    }

                    createdAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading created events", e));
    }

    private void loadArchivedEvents() {
        firestore.collection("user_arch").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    archivedEvents.clear();
                    archivedEventIds.clear();

                    if (document.exists() && document.getData() != null) {
                        List<String> events = (List<String>) document.get("events");
                        if (events != null) {
                            for (String eventId : events) {
                                fetchArchivedEventDetails(eventId, archivedEvents, archivedEventIds, archivedAdapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading archived events", e));
    }


    private void loadArchivedCreatedEvents() {
        firestore.collection("user_owner_arch").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    archivedCreatedEvents.clear();
                    archivedCreatedEventIds.clear();

                    if (document.exists() && document.getData() != null) {
                        List<String> events = (List<String>) document.get("events");
                        if (events != null) {
                            for (String eventId : events) {
                                fetchArchivedEventDetails(eventId, archivedCreatedEvents, archivedCreatedEventIds, archivedCreatedAdapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading archived created events", e));
    }

    private void fetchArchivedEventDetails(String eventId, List<String> eventList, List<String> eventIdList, ArrayAdapter<String> adapter) {
        firestore.collection("markers_arch").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String eventName = document.getString("title");
                        String eventDate = document.getString("eventDateTime");

                        if (eventName != null && eventDate != null) {
                            eventList.add(eventName + " - " + eventDate);
                            eventIdList.add(eventId);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching archived event details", e));
    }



    private void fetchEventDetails(String eventId, List<String> eventList, List<String> eventIdList, ArrayAdapter<String> adapter) {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String eventName = document.getString("title");
                        String eventDate = document.getString("eventDateTime");

                        if (eventName != null && eventDate != null) {
                            eventList.add(eventName + " - " + eventDate);
                            eventIdList.add(eventId);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching event details", e));
    }

    private void openMarkerInfo(String eventId) {
        firestore.collection("markers").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
                        intent.putExtra("MARKER_ID", eventId);
                        intent.putExtra("TITLE", document.getString("title"));
                        intent.putExtra("DESCRIPTION", document.getString("description"));
                        intent.putExtra("LATITUDE", document.getDouble("latitude"));
                        intent.putExtra("LONGITUDE", document.getDouble("longitude"));

                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Event details not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching event details", e));
    }

    private void openMarkerInfoArch(String eventId) {
        firestore.collection("markers_arch").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
                        intent.putExtra("MARKER_ID", eventId);
                        intent.putExtra("TITLE", document.getString("title"));
                        intent.putExtra("DESCRIPTION", document.getString("description"));
                        intent.putExtra("LATITUDE", document.getDouble("latitude"));
                        intent.putExtra("LONGITUDE", document.getDouble("longitude"));

                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Event details not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching event details", e));
    }
}