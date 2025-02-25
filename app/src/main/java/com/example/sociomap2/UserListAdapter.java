package com.example.sociomap2;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListAdapter extends ArrayAdapter<User> {

    private static final String TAG = "UserListAdapter";
    private Context context;
    private List<User> userList;
    private FirebaseFirestore firestore;
    private String currentUserId;

    public UserListAdapter(Context context, List<User> userList) {
        super(context, 0, userList);
        this.context = context;
        this.userList = userList != null ? new ArrayList<>(userList) : new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 🔹 Prevents crash when userList is empty
        if (userList == null || userList.isEmpty() || position >= userList.size()) {
            Log.e(TAG, "getView: Attempted to access an empty list at position " + position);
            return new View(context);
        }

        ViewHolder holder;

        // 🔹 Ensure convertView is never null
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
            holder = new ViewHolder();
            holder.txtUserInfo = convertView.findViewById(R.id.txt_user_info);
            holder.btnFollowSigned = convertView.findViewById(R.id.btn_follow_signed);
            holder.btnFollowCreated = convertView.findViewById(R.id.btn_follow_created);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 🔹 Ensure ViewHolder is correctly initialized
        if (holder == null || holder.txtUserInfo == null || holder.btnFollowSigned == null || holder.btnFollowCreated == null) {
            Log.e(TAG, "getView: ViewHolder or its views are null, reinitializing...");
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
            holder = new ViewHolder();
            holder.txtUserInfo = convertView.findViewById(R.id.txt_user_info);
            holder.btnFollowSigned = convertView.findViewById(R.id.btn_follow_signed);
            holder.btnFollowCreated = convertView.findViewById(R.id.btn_follow_created);
            convertView.setTag(holder);
        }

        // 🔹 Fetch user data
        User user = userList.get(position);
        holder.txtUserInfo.setText(user.getFullName());

        // ✅ Check if user is already followed and update button text
        checkIfUserIsFollowed(user.getUserId(), "user_signup_follow", holder.btnFollowSigned);
        checkIfUserIsFollowed(user.getUserId(), "user_create_follow", holder.btnFollowCreated);

        // ✅ Toggle Follow/Unfollow
        ViewHolder finalHolder = holder;
        holder.btnFollowSigned.setOnClickListener(v -> toggleFollowUser(user.getUserId(), "user_signup_follow", finalHolder.btnFollowSigned));
        ViewHolder finalHolder1 = holder;
        holder.btnFollowCreated.setOnClickListener(v -> toggleFollowUser(user.getUserId(), "user_create_follow", finalHolder1.btnFollowCreated));

        return convertView;
    }

    public void updateList(List<User> newList) {
        if (newList == null) {
            Log.e(TAG, "updateList: Attempted to update with a null list.");
            return;
        }

        userList.clear();
        userList.addAll(newList);

        // 🔹 Prevent crash if the list is empty
        if (userList.isEmpty()) {
            Log.e(TAG, "updateList: The updated list is empty.");
        }

        notifyDataSetChanged(); // ✅ Ensures UI updates properly
    }

    private void checkIfUserIsFollowed(String followedUserId, String collection, Button followButton) {
        if (currentUserId == null) return;

        firestore.collection(collection).document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("following")) {
                        List<String> followingList = (List<String>) document.get("following");
                        followButton.setText((followingList != null && followingList.contains(followedUserId)) ? "Unfollow" : "Follow");
                    } else {
                        followButton.setText("Follow");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking follow status", e));
    }

    private void toggleFollowUser(String followedUserId, String collection, Button followButton) {
        if (currentUserId == null) {
            Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection(collection).document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    boolean isFollowing = document.exists() && document.contains("following") &&
                            ((List<String>) document.get("following")).contains(followedUserId);

                    // 🔄 **Instant UI update** before Firestore operation
                    followButton.setText(isFollowing ? "Follow" : "Unfollow");

                    // 🔄 **Use Firestore's array operations**
                    firestore.collection(collection).document(currentUserId)
                            .update("following", isFollowing ? FieldValue.arrayRemove(followedUserId) : FieldValue.arrayUnion(followedUserId))
                            .addOnSuccessListener(aVoid -> {
                                if (isFollowing) {
                                    Toast.makeText(context, "Unfollowed!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Now following!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error toggling follow status", e);
                                followButton.setText(isFollowing ? "Unfollow" : "Follow"); // Revert UI if failed
                            });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error retrieving follow status", e));
    }

    static class ViewHolder {
        TextView txtUserInfo;
        Button btnFollowSigned;
        Button btnFollowCreated;
    }
}