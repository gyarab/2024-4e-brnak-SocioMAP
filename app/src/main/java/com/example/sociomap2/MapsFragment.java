package com.example.sociomap2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapsFragment";
    private GoogleMap googleMap;
    private boolean isDefaultMap = true; // Toggle between default and add-marker map
    private FirebaseFirestore firestore; // Firestore instance
    private FirebaseAuth firebaseAuth; // Firebase Authentication instance
    private String userId; // Store logged-in user ID

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

        // Get logged-in user ID
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid(); // Store user ID
        } else {
            Log.e(TAG, "User is not logged in.");
            userId = "UnknownUser"; // Fallback user ID
        }

        // Set up the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Button to toggle between default and add-marker maps
        view.findViewById(R.id.btn_switch_map).setOnClickListener(v -> {
            isDefaultMap = !isDefaultMap;
            if (googleMap != null) {
                googleMap.clear(); // Clear markers when switching modes
                if (isDefaultMap) {
                    loadDefaultMap();
                } else {
                    loadAddMarkerMap();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (isDefaultMap) {
            loadDefaultMap();
        } else {
            loadAddMarkerMap();
        }
    }

    private void loadDefaultMap() {
        LatLng czechRepublic = new LatLng(49.8175, 15.4730);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(czechRepublic, 7));

        firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String title = document.getString("title");
                String description = document.getString("description");
                String theme = document.getString("theme"); // Get theme from Firestore

                if (latitude == null || longitude == null || title == null || description == null) {
                    Log.e(TAG, "Missing field in document: " + document.getId());
                    continue;
                }

                LatLng position = new LatLng(latitude, longitude);
                float color = getMarkerColor(theme); // Get color based on theme

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(description)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))); // Apply color

                if (marker != null) {
                    marker.setTag(document.getId()); // Store Firestore document ID
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching markers", e));

        googleMap.setOnMarkerClickListener(marker -> {
            // Get the tag associated with the marker (this should be the Firestore document ID or event ID)
            Object tag = marker.getTag();

            if (tag == null || tag.toString().isEmpty()) {
                Toast.makeText(getActivity(), "Error: Marker ID or invalid.", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Create an intent to open MarkerInfoActivity
            Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);

            // Pass data to MarkerInfoActivity
            intent.putExtra("MARKER_ID", tag.toString());  // Pass Firestore document ID or marker ID
            intent.putExtra("TITLE", marker.getTitle());   // Pass the title of the marker
            intent.putExtra("DESCRIPTION", marker.getSnippet()); // Pass the description of the marker
            intent.putExtra("LATITUDE", marker.getPosition().latitude);  // Pass the latitude of the marker
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);  // Pass the longitude of the marker

            // Start MarkerInfoActivity
            startActivity(intent);

            return true;
        });
    }

    private void loadAddMarkerMap() {
        LatLng czechRepublic = new LatLng(49.8175, 15.4730);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(czechRepublic, 7));

        googleMap.setOnMapClickListener(latLng -> {
            Intent intent = new Intent(getActivity(), AddMarkerActivity.class);
            intent.putExtra("LATITUDE", latLng.latitude);
            intent.putExtra("LONGITUDE", latLng.longitude);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });
    }

    // Function to assign marker color based on theme
    private float getMarkerColor(String theme) {
        if (theme == null) return BitmapDescriptorFactory.HUE_RED; // Default

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
                return BitmapDescriptorFactory.HUE_RED; // Default for unknown themes
        }
    }
}