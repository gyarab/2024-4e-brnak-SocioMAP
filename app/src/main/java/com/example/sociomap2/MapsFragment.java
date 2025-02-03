package com.example.sociomap2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapsFragment";
    private GoogleMap googleMap;
    private boolean isDefaultMap = true; // Flag to toggle between default and add-marker map
    private FirebaseFirestore firestore; // Firestore instance

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

        // Set up the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up the spinner for filtering
        Spinner spnFilter = view.findViewById(R.id.spn_filter);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.map_filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFilter.setAdapter(adapter);

        // Handle spinner item selection
        spnFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = parent.getItemAtPosition(position).toString();
                applyMarkerFilter(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Button to switch between default and add-marker maps
        view.findViewById(R.id.btn_switch_map).setOnClickListener(v -> {
            isDefaultMap = !isDefaultMap; // Toggle map mode
            if (googleMap != null) {
                googleMap.clear(); // Clear markers and listeners when switching maps
                if (isDefaultMap) {
                    loadDefaultMap();
                } else {
                    loadAddMarkerMap();
                }
            }
        });
    }

    private void applyMarkerFilter(String filter) {
        if (googleMap == null) return;

        googleMap.clear(); // Clear all markers before applying the filter

        firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                // Safely retrieve marker data
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String title = document.getString("title");
                String description = document.getString("description");
                String theme = document.getString("theme"); // Retrieve theme for filtering

                if (latitude == null || longitude == null || title == null || description == null || theme == null) {
                    Log.e(TAG, "Missing field in document: " + document.getId());
                    continue; // Skip this marker if any field is missing
                }

                // Apply filter: Skip markers that don't match the selected filter (unless "All Markers" is selected)
                if (!filter.equals("All Markers") && !filter.equals(theme)) {
                    continue;
                }

                // Add marker to the map
                LatLng position = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(description));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching markers for filter", e);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Load the initial map (default map showing Firebase markers)
        if (isDefaultMap) {
            loadDefaultMap();
        } else {
            loadAddMarkerMap();
        }
    }

    private void loadDefaultMap() {
        // Center the map on Prague, Czech Republic
        LatLng czechRepublic = new LatLng(49.8175, 15.4730);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(czechRepublic, 7));

        // Fetch markers from Firebase Firestore
        firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                // Safely retrieve marker data
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String title = document.getString("title");
                String description = document.getString("description");

                if (latitude == null || longitude == null || title == null || description == null) {
                    Log.e(TAG, "Missing field in document: " + document.getId());
                    continue; // Skip this marker if any field is missing
                }

                // Add marker to the map
                LatLng position = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(description));
            }
        }).addOnFailureListener(e -> {
            // Log error if Firestore query fails
            Log.e(TAG, "Error fetching markers", e);
        });

        // Handle marker click events
        googleMap.setOnMarkerClickListener(marker -> {
            // Open MarkerInfoActivity with marker details
            Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
            intent.putExtra("TITLE", marker.getTitle());
            intent.putExtra("DESCRIPTION", marker.getSnippet());
            intent.putExtra("LATITUDE", marker.getPosition().latitude);
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);
            startActivity(intent);
            return true; // Return true to indicate the click event is handled
        });
    }

    private void loadAddMarkerMap() {
        // Center the map on Prague, Czech Republic
        LatLng czechRepublic = new LatLng(49.8175, 15.4730);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(czechRepublic, 7));

        // Set a listener for map clicks to add new markers
        googleMap.setOnMapClickListener(latLng -> {
            // Open AddMarkerActivity with clicked coordinates
            Intent intent = new Intent(getActivity(), AddMarkerActivity.class);
            intent.putExtra("LATITUDE", latLng.latitude);
            intent.putExtra("LONGITUDE", latLng.longitude);
            startActivity(intent);
        });
    }


}