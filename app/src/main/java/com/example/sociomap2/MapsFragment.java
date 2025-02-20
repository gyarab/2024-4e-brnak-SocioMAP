package com.example.sociomap2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

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

        // ✅ Initialize Calendar Filter Button
        Button btnCalendarFilter = view.findViewById(R.id.btn_calendar_filter);
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

        googleMap.clear(); // Clear the map when switching modes

        if (isEditMode) {
            // ✅ Edit Mode: Hide markers, enable map clicks for adding events
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
            // ✅ View Mode: Show markers and allow users to click on them
            btnToggleMode.setText("Switch to Edit Mode");
            spnFilter.setVisibility(View.VISIBLE); // Show filter in View Mode
            loadMarkers();
        }
    }

    private void loadMarkers() {
        firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            googleMap.clear(); // Clear previous markers before applying the filter

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String title = document.getString("title");
                String description = document.getString("description");
                String theme = document.getString("theme");
                String eventDateTime = document.getString("eventDateTime"); // Get full date-time string

                if (latitude == null || longitude == null || title == null || description == null || theme == null) {
                    Log.e(TAG, "Missing field in document: " + document.getId());
                    continue;
                }

                // ✅ Check if eventDateTime is valid before splitting
                String eventDate = null;
                if (eventDateTime != null && eventDateTime.contains(" ")) {
                    String[] dateTimeParts = eventDateTime.split(" ");
                    if (dateTimeParts.length > 1) {
                        eventDate = dateTimeParts[0]; // Extract only the date (YYYY-MM-DD)
                    }
                }

                // ✅ Apply Theme Filter
                if (!selectedTheme.equals("All") && !selectedTheme.equalsIgnoreCase(theme)) {
                    continue;
                }

                // ✅ Apply Date Filter (If a date is selected)
                if (selectedDate != null && (eventDate == null || !selectedDate.equals(eventDate))) {
                    continue;
                }

                LatLng position = new LatLng(latitude, longitude);
                float color = getMarkerColor(theme);

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(description)
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));

                if (marker != null) {
                    marker.setTag(document.getId());
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching markers", e));

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



    private void setupFilterSpinner() {
        String[] themes = {"All", "Sports", "Music", "Festival", "Workshop", "Custom"};

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
}