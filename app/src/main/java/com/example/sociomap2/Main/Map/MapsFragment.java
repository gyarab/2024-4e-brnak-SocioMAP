package com.example.sociomap2.Main.Map;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.sociomap2.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;


public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapsFragment";
    private GoogleMap googleMap;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;
    private Spinner spnFilter;
    private Button btnToggleMode;
    private String selectedTheme = "All";
    private boolean isEditMode = false; // Default: View Mode
    private FusedLocationProviderClient fusedLocationClient;
    private String selectedDate = null; // Stores selected date
    private boolean showOnlyFamous = false; // Track the famous filter state
    private FloatingActionButton btnToggleFamous;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Log.e(TAG, "User is not logged in.");
            userId = "UnknownUser";
        }

        // Set up the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Spinner (Filter)
        spnFilter = view.findViewById(R.id.spn_filter);
        setupFilterSpinner();

        // Toggle Edit Mode Button
        btnToggleMode = view.findViewById(R.id.btn_toggle_mode);
        btnToggleMode.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            updateMapMode();
        });

        // Star Button to Filter Famous Users
        btnToggleFamous = view.findViewById(R.id.btn_toggle_famous);
        btnToggleFamous.setImageResource(android.R.drawable.btn_star_big_on);
        btnToggleFamous.setOnClickListener(v -> {
            showOnlyFamous = !showOnlyFamous; // Toggle filter

            // 🔹 Change Star Icon Based on Filter State
            if (showOnlyFamous) {
                btnToggleFamous.setImageResource(android.R.drawable.btn_star); // Filled Star
            } else {
                btnToggleFamous.setImageResource(android.R.drawable.btn_star_big_on); // Outline Star
            }

            loadMarkers(); // Refresh markers with filter applied
        });

        // Calendar Filter Button
        FloatingActionButton btnCalendarFilter = view.findViewById(R.id.btn_calendar_filter);
        btnCalendarFilter.setOnClickListener(v -> showDatePickerDialog());

        updateMapMode();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setMapToUserLocation(); // Set camera to user's location
        updateMapMode();
    }

    private void setMapToUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        googleMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12)); // Adjust zoom level
            } else {
                // Default location (Czech Republic) if user location is not available
                LatLng defaultLocation = new LatLng(49.8175, 15.4730);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 7));
            }
        });
    }

    private void updateMapMode() {
        if (googleMap == null) return;

        googleMap.clear(); // Clear map when switching

        if (isEditMode) {
            // Edit Mode: Hide markers, enable map clicks - adding events
            btnToggleMode.setText("Switch to View Mode");
            spnFilter.setVisibility(View.GONE); // Hide filter in Edit Mode

            googleMap.setOnMapClickListener(latLng -> {
                Intent intent = new Intent(getActivity(), AddMarkerActivity.class);
                intent.putExtra("LATITUDE", latLng.latitude);
                intent.putExtra("LONGITUDE", latLng.longitude);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            });

            googleMap.setOnMarkerClickListener(null); // Disable marker clicks

        } else {
            // View Mode: Show markers, allow users to click on them
            btnToggleMode.setText("Switch to Edit Mode");
            spnFilter.setVisibility(View.VISIBLE); // Show filter in View Mode
            loadMarkers();
        }
    }

    private void loadMarkers() {
        firestore.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists() || !userDoc.contains("birthyear")) {
                Log.e(TAG, "User's birthDate not found in Firestore.");
                return;
            }

            // Get user's birthdate from Firestore
            String birthDateString = userDoc.getString("birthyear"); // Format: "YYYY-MM-DD"
            if (birthDateString == null || birthDateString.isEmpty()) {
                Log.e(TAG, "User birthDate is empty.");
                return;
            }

            // Calculate user's age
            int userAge = calculateAge(birthDateString);
            Log.d(TAG, "User age: " + userAge);

            // Now fetch markers
            firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
                googleMap.clear();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date currentDate = new Date();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String eventId = document.getId();
                    Double latitude = document.getDouble("latitude");
                    Double longitude = document.getDouble("longitude");
                    String title = document.getString("title");
                    String description = document.getString("description");
                    String theme = document.getString("theme");
                    String eventDateTime = document.getString("eventDateTime");
                    String ownerId = document.getString("userId");
                    int eventAgeLimit = document.contains("ageLimit") ? document.getLong("ageLimit").intValue() : 0; // Default to 0 if missing

                    if (latitude == null || longitude == null || title == null || description == null || theme == null || eventDateTime == null || ownerId == null) {
                        Log.e(TAG, "Missing field in document: " + document.getId());
                        continue;
                    }

                    try {
                        Date eventDate = sdf.parse(eventDateTime);
                        if (eventDate != null && eventDate.before(currentDate)) {
                            archiveEvent(document, eventId, ownerId);
                            continue;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing eventDateTime for event: " + eventId, e);
                        continue;
                    }

                    String eventDate = eventDateTime.split(" ")[0];

                    // Log event details for debugging
                    Log.d(TAG, "Checking event: " + title + " | Age Limit: " + eventAgeLimit);

                    // Age Limit Filter
                    if (userAge < eventAgeLimit) {
                        Log.d(TAG, "Skipping event: " + title + " (Age limit: " + eventAgeLimit + ", User age: " + userAge + ")");
                        continue;
                    }

                    // Theme Filter
                    if (!selectedTheme.equals("All") && !selectedTheme.equalsIgnoreCase(theme)) {
                        continue;
                    }

                    // Date Filter
                    if (selectedDate != null && !selectedDate.equals(eventDate)) {
                        continue;
                    }

                    LatLng position = new LatLng(latitude, longitude);

                    // 🔹 Fetch user data to check if the owner is famous
                    firestore.collection("users").document(ownerId).get()
                            .addOnSuccessListener(ownerDoc -> {
                                boolean isFamous = ownerDoc.contains("isFamous") && Boolean.TRUE.equals(ownerDoc.getBoolean("isFamous"));

                                // If famous filter is ON, skip non-famous markers
                                if (showOnlyFamous && !isFamous) {
                                    return;
                                }

                                float markerColor = getMarkerColor(theme);
                                float markerSize = 1.0f;

                                if (isFamous) {
                                    markerColor = BitmapDescriptorFactory.HUE_CYAN;
                                    markerSize = 1.5f;
                                }

                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(title)
                                        .snippet(description)
                                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                                        .anchor(0.5f, markerSize));

                                if (marker != null) {
                                    marker.setTag(eventId);
                                }
                            });
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error fetching markers", e));

        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user birthDate", e));

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();

            if (tag == null || tag.toString().isEmpty()) {
                Toast.makeText(getActivity(), "Error: Marker ID is invalid.", Toast.LENGTH_SHORT).show();
                return false;
            }

            Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
            intent.putExtra("MARKER_ID", tag.toString());
            intent.putExtra("TITLE", marker.getTitle());
            intent.putExtra("DESCRIPTION", marker.getSnippet());
            intent.putExtra("LATITUDE", marker.getPosition().latitude);
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);

            startActivity(intent);
            return true;
        });
    }

    private void archiveEvent(QueryDocumentSnapshot document, String eventId, String ownerId) {
        firestore.collection("event_guest_list").document(eventId)
                .get()
                .addOnSuccessListener(guestListDoc -> {
                    List<String> attendees = (List<String>) guestListDoc.get("users");

                    // ✅ Move event to `markers_arch`
                    firestore.collection("markers_arch").document(eventId)
                            .set(document.getData())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Event archived successfully: " + eventId));

                    // Remove event from markers
                    firestore.collection("markers").document(eventId)
                            .delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Event removed from active markers: " + eventId));

                    // Add event to each user's archive
                    if (attendees != null) {
                        for (String userId : attendees) {
                            firestore.collection("user_arch").document(userId)
                                    .update("events", FieldValue.arrayUnion(eventId))
                                    .addOnFailureListener(e -> {
                                        // If the document doesn't exist, create it
                                        firestore.collection("user_arch").document(userId)
                                                .set(Map.of("events", new ArrayList<String>() {{ add(eventId); }}));
                                    });
                        }
                    }

                    // Add event to the owner's archive
                    firestore.collection("user_owner_arch").document(ownerId)
                            .update("events", FieldValue.arrayUnion(eventId))
                            .addOnFailureListener(e -> {
                                firestore.collection("user_owner_arch").document(ownerId)
                                        .set(Map.of("events", new ArrayList<String>() {{ add(eventId); }}));
                            });

                    // Remove event from `event_guest_list`
                    firestore.collection("event_guest_list").document(eventId).delete();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error retrieving guest list for event: " + eventId, e));
    }




    private void setupFilterSpinner() {
        String[] themes = {"All", "Sports", "Music", "Festival", "Concert", "Custom"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, themes);
        spnFilter.setAdapter(adapter);

        spnFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTheme = parent.getItemAtPosition(position).toString();
                if (!isEditMode) { // Only update markers if in View Mode
                    googleMap.clear();
                    loadMarkers();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTheme = "All";
                if (!isEditMode) {
                    googleMap.clear();
                    loadMarkers();
                }
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    loadMarkers(); // Reload markers after selecting a date
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }


    private float getMarkerColor(String theme) {
        if (theme == null) return BitmapDescriptorFactory.HUE_RED;

        switch (theme.toLowerCase()) {
            case "sports":
                return BitmapDescriptorFactory.HUE_BLUE;
            case "music":
                return BitmapDescriptorFactory.HUE_VIOLET;
            case "festival":
                return BitmapDescriptorFactory.HUE_YELLOW;
            case "workshop":
                return BitmapDescriptorFactory.HUE_GREEN;
            case "custom":
                return BitmapDescriptorFactory.HUE_ORANGE;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private int calculateAge(String birthDateString) {
        try {
            // Parse the birthDate string (format: "YYYY-MM-DD")
            String[] parts = birthDateString.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = Integer.parseInt(parts[1]);
            int birthDay = Integer.parseInt(parts[2]);

            // Get current date
            Calendar today = Calendar.getInstance();
            int currentYear = today.get(Calendar.YEAR);
            int currentMonth = today.get(Calendar.MONTH) + 1; // Months are 0-based
            int currentDay = today.get(Calendar.DAY_OF_MONTH);

            // Calculate age
            int age = currentYear - birthYear;

            // Adjust if birthday hasn't occurred yet this year
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--;
            }

            Log.d(TAG, "Calculated age: " + age);
            return age;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing birthDate: " + birthDateString, e);
            return 0; // Return 0 if there's an error
        }
    }
}