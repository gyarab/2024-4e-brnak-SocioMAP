package com.example.sociomap2.Main.Map;

import android.Manifest;
import android.animation.ValueAnimator;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    private List<String> filteredSignUpUsers = new ArrayList<>();
    private List<String> filteredOwnerUsers = new ArrayList<>();

    private CardView cardFilterMenu;
    private FloatingActionButton btnFriendsSignUp, btnFriendsCreateOwner, btnToggleFilters, btnCalendarFilter;
    private LinearLayout layoutFilterMenu;



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


        cardFilterMenu = view.findViewById(R.id.card_filter_menu);
        btnFriendsSignUp = view.findViewById(R.id.btn_friends_sign_up);
        btnFriendsCreateOwner = view.findViewById(R.id.btn_friends_create_owner);
        btnToggleFilters = view.findViewById(R.id.btn_toggle_filters);
        layoutFilterMenu = view.findViewById(R.id.layout_filter_menu);

        //btnFriendsSignUp.setOnClickListener(v -> openUserSearchDialog("signUp"));
        //btnFriendsCreateOwner.setOnClickListener(v -> openUserSearchDialog("createOwner"));

        // Toggle filter menu visibility
        btnToggleFilters.setOnClickListener(v -> {
            if (layoutFilterMenu.getVisibility() == View.VISIBLE) {
                // Collapse to 90dp
                ValueAnimator animator = ValueAnimator.ofInt(cardFilterMenu.getHeight(), 280);
                animator.setDuration(300);
                animator.addUpdateListener(animation -> {
                    ViewGroup.LayoutParams params = cardFilterMenu.getLayoutParams();
                    params.height = (int) animation.getAnimatedValue();
                    cardFilterMenu.setLayoutParams(params);
                });

                layoutFilterMenu.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction(() -> layoutFilterMenu.setVisibility(View.GONE))
                        .start();

                animator.start();
            } else {
                // Expand to 350dp
                ValueAnimator animator = ValueAnimator.ofInt(cardFilterMenu.getHeight(), 850);
                animator.setDuration(500);
                animator.addUpdateListener(animation -> {
                    ViewGroup.LayoutParams params = cardFilterMenu.getLayoutParams();
                    params.height = (int) animation.getAnimatedValue();
                    cardFilterMenu.setLayoutParams(params);
                });

                layoutFilterMenu.setVisibility(View.VISIBLE);
                layoutFilterMenu.setAlpha(0f);
                layoutFilterMenu.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start();

                animator.start();
            }
        });

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
        btnCalendarFilter = view.findViewById(R.id.btn_calendar_filter);
        btnCalendarFilter.setOnClickListener(v -> showDatePickerDialog());

        updateMapMode();

        btnFriendsSignUp.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Friends Sign-Up Filter Activated", Toast.LENGTH_SHORT).show();
            showUserSearchDialog("signup");
        });
        btnFriendsCreateOwner.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Friends Owner Filter Activated", Toast.LENGTH_SHORT).show();
            showUserSearchDialog("owner");

        });
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
                LatLng defaultLocation = new LatLng(48.69096, 9.14062);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 7));
            }
        });
    }

    private void updateMapMode() {
        if (googleMap == null) return;


        googleMap.clear(); // Only clear once
        if (!isEditMode) {
            loadMarkers(); // Reload markers when switching to View Mode
        }

        if (isEditMode) {
            // Edit Mode: Hide markers, enable map clicks - adding events
            btnToggleMode.setText("Switch to View Mode");
            spnFilter.setVisibility(View.GONE);
            btnToggleFilters.setVisibility(View.GONE);
            btnCalendarFilter.setVisibility(View.GONE);

            googleMap.setOnMapClickListener(latLng -> {
                Intent intent = new Intent(getActivity(), AddMarkerActivity.class);
                intent.putExtra("LATITUDE", latLng.latitude);
                intent.putExtra("LONGITUDE", latLng.longitude);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            });

            googleMap.setOnMarkerClickListener(null); // Disable marker clicks
        } else {
            // ✅ View Mode: Show markers & ensure they load
            btnToggleMode.setText("Switch to Edit Mode");
            spnFilter.setVisibility(View.VISIBLE);
            btnToggleFilters.setVisibility(View.VISIBLE);
            btnCalendarFilter.setVisibility(View.VISIBLE);
            loadMarkers(); // ✅ Ensure markers load when switching modes
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

            // Fetch all markers
            firestore.collection("markers").get().addOnSuccessListener(queryDocumentSnapshots -> {
                googleMap.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date currentDate = new Date();

                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    String eventId = document.getId();
                    Double latitude = document.getDouble("latitude");
                    Double longitude = document.getDouble("longitude");
                    String title = document.getString("title");
                    String description = document.getString("description");
                    String theme = document.getString("theme");
                    String eventDateTime = document.getString("eventDateTime");
                    String ownerId = document.getString("userId");
                    boolean isFamous = document.contains("isFamous") && Boolean.TRUE.equals(document.getBoolean("isFamous"));
                    int eventAgeLimit = document.contains("ageLimit") ? document.getLong("ageLimit").intValue() : 0; // Default to 0

                    // Skip if any critical information is missing
                    if (latitude == null || longitude == null || title == null || description == null ||
                            theme == null || eventDateTime == null || ownerId == null) {
                        Log.e(TAG, "Skipping marker due to missing fields: " + eventId);
                        continue;
                    }

                    // Convert event date
                    try {
                        Date eventDate = sdf.parse(eventDateTime);
                        if (eventDate != null && eventDate.before(currentDate)) {
                            archiveEvent(document, eventId, ownerId);
                            continue;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event date for event: " + eventId, e);
                        continue;
                    }

                    // Age restriction check
                    if (userAge < eventAgeLimit) {
                        Log.d(TAG, "Skipping event: " + title + " (User age: " + userAge + ", Required: " + eventAgeLimit + ")");
                        continue;
                    }

                    // Apply filters
                    boolean passesSignUpFilter = filteredSignUpUsers.isEmpty() || filteredSignUpUsers.contains(ownerId);
                    boolean passesOwnerFilter = filteredOwnerUsers.isEmpty() || filteredOwnerUsers.contains(ownerId);
                    boolean passesThemeFilter = selectedTheme.equals("All") || selectedTheme.equalsIgnoreCase(theme);
                    boolean passesDateFilter = (selectedDate == null || selectedDate.equals(eventDateTime.split(" ")[0]));

                    // Famous user filter
                    if (showOnlyFamous && !isFamous) {
                        continue;
                    }

                    // No filters applied? Show everything
                    boolean noFiltersApplied = filteredSignUpUsers.isEmpty() && filteredOwnerUsers.isEmpty()
                            && selectedTheme.equals("All") && selectedDate == null && !showOnlyFamous;

                    if (noFiltersApplied || (passesSignUpFilter && passesOwnerFilter && passesThemeFilter && passesDateFilter)) {
                        addMarker(document);
                    }
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error fetching markers", e));

        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user birthDate", e));

        // Handle marker click events
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

    private void archiveEvent(DocumentSnapshot document, String eventId, String ownerId) {
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
        List<String> themes = Arrays.asList("All", "Sports", "Music", "Festival", "Concert", "Custom");

        // Create Adapter with custom layouts
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_selected_item, themes);
        adapter.setDropDownViewResource(R.layout.spinner_item); // Set dropdown layout

        // Set adapter to the Spinner
        spnFilter.setAdapter(adapter);

        // Handle selection events
        spnFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTheme = parent.getItemAtPosition(position).toString();
                // Ensure selected text remains black
                ((TextView) parent.getChildAt(0)).setText(selectedTheme);
                ((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(android.R.color.black));
                ((TextView) parent.getChildAt(0)).setVisibility(View.VISIBLE);
                if (!isEditMode) { // Only update markers if in View Mode
                    googleMap.clear();
                    loadMarkers();
                }

                // Do something when an item is selected
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

    //Search system if needed
    private void openUserSearchDialog(String filterType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Search for a User");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText searchInput = new EditText(requireContext());
        searchInput.setHint("Enter username");
        layout.addView(searchInput);

        ListView listView = new ListView(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_multiple_choice);
        listView.setAdapter(adapter);
        layout.addView(listView);

        builder.setView(layout);

        // Fetch the list of followed users based on filter type
        String collectionName = filterType.equals("signUp") ? "user_sign_up_follow" : "user_create_follow";
        firestore.collection(collectionName).document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.get("users") != null) {
                        List<String> followedUsers = (List<String>) document.get("users");
                        adapter.addAll(followedUsers);
                        adapter.notifyDataSetChanged();
                    }
                });

        builder.setPositiveButton("Apply Filter", (dialog, which) -> {
            List<String> selectedUsers = new ArrayList<>();
            for (int i = 0; i < listView.getCount(); i++) {
                if (listView.isItemChecked(i)) {
                    selectedUsers.add(adapter.getItem(i));
                }
            }

            if (filterType.equals("signUp")) {
                filterMarkersBySignUpUsers(selectedUsers);
            } else {
                filterMarkersByOwnerUsers(selectedUsers);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void filterMarkersBySignUpUsers(List<String> users) {
        filteredSignUpUsers = users;
        loadMarkers();
    }

    private void filterMarkersByOwnerUsers(List<String> users) {
        filteredOwnerUsers = users;
        loadMarkers();
    }

    private void addMarker(DocumentSnapshot document) {
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        String title = document.getString("title");
        String description = document.getString("description");
        String theme = document.getString("theme");

        if (latitude == null || longitude == null || title == null || description == null || theme == null) {
            return;
        }

        LatLng position = new LatLng(latitude, longitude);
        float markerColor = getMarkerColor(theme);

        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title)
                .snippet(description)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

        if (marker != null) {
            marker.setTag(document.getId());
        }
    }

    private void showUserSearchDialog(String filterType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select a Friend");

        // Get the collection name based on filter type
        String collection = filterType.equals("signup") ? "user_signup_follow" : "user_great_follow";

        firestore.collection(collection).document(userId).get().addOnSuccessListener(document -> {
            if (document.exists() && document.contains("following")) {
                List<String> followingIds = (List<String>) document.get("following");

                if (followingIds == null || followingIds.isEmpty()) {
                    Toast.makeText(getActivity(), "No friends found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> displayNames = new ArrayList<>();
                Map<String, String> idToDisplayNameMap = new HashMap<>();

                for (String friendId : followingIds) {
                    firestore.collection("users").document(friendId).get().addOnSuccessListener(friendDoc -> {
                        if (friendDoc.exists()) {
                            String username = friendDoc.getString("username");
                            String name = friendDoc.getString("name");
                            String surname = friendDoc.getString("surname");

                            if (username != null && name != null && surname != null) {
                                String displayName = username + " " + name + " " + surname;
                                displayNames.add(displayName);
                                idToDisplayNameMap.put(displayName, friendId);
                            }

                            // Check if all users are fetched
                            if (displayNames.size() == followingIds.size()) {
                                String[] userArray = displayNames.toArray(new String[0]);

                                builder.setItems(userArray, (dialog, which) -> {
                                    String selectedDisplayName = userArray[which];
                                    String selectedUserId = idToDisplayNameMap.get(selectedDisplayName);
                                    applyUserFilter(selectedUserId, filterType);
                                });

                                builder.show();
                            }
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "No friends found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching user follow list", e);
            Toast.makeText(getActivity(), "Failed to load friends!", Toast.LENGTH_SHORT).show();
        });
    }

    private void applyUserFilter(String selectedUserId, String filterType) {
        googleMap.clear(); // Clear the map before applying the filter

        if (filterType.equals("signup")) {
            // Fetch events the user has signed up for
            firestore.collection("user_events").document(selectedUserId).get().addOnSuccessListener(document -> {
                if (document.exists() && document.contains("events")) {
                    List<String> eventIds = (List<String>) document.get("events");

                    if (eventIds == null || eventIds.isEmpty()) {
                        Toast.makeText(getActivity(), "No events found for this user!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Fetch marker details for those event IDs
                    for (String eventId : eventIds) {
                        firestore.collection("markers").document(eventId).get().addOnSuccessListener(eventDoc -> {
                            if (eventDoc.exists()) {
                                addMarker(eventDoc);
                            }
                        });
                    }
                } else {
                    Toast.makeText(getActivity(), "No events found for this user!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user events", e));
        } else {
            // Fetch events created by the user
            firestore.collection("markers").whereEqualTo("userId", selectedUserId).get().addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        addMarker(document);
                    }
                } else {
                    Toast.makeText(getActivity(), "No events created by this user!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user-created events", e));
        }
    }

}

