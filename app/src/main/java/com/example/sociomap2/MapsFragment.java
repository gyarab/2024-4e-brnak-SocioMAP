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

                if (latitude == null || longitude == null || title == null || description == null) {
                    Log.e(TAG, "Missing field in document: " + document.getId());
                    continue;
                }

                LatLng position = new LatLng(latitude, longitude);
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(description));

                if (marker != null) {
                    marker.setTag(document.getId()); // Store Firestore document ID
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching markers", e));

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag == null) {
                Toast.makeText(getActivity(), "Error: Marker ID is missing.", Toast.LENGTH_SHORT).show();
                return false;
            }

            Intent intent = new Intent(getActivity(), MarkerInfoActivity.class);
            intent.putExtra("MARKER_ID", tag.toString()); // Pass Firestore document ID
            intent.putExtra("TITLE", marker.getTitle());
            intent.putExtra("DESCRIPTION", marker.getSnippet());
            intent.putExtra("LATITUDE", marker.getPosition().latitude);
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);

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
}
