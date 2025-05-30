package com.example.appdev50;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class Professor_settings extends Fragment {

    private LinearLayout profile, resetpass, logout, privacy, terms;
    private ImageView profilepic;
    TextView proffullname;
    String uid;
    DatabaseReference database;
    View view;
    private ProgressDialog progressDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_professor_settings, container, false);

        proffullname = view.findViewById(R.id.profile_name);
        profilepic = view.findViewById(R.id.profile_picture);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false); // Prevent dismissal by touch outside
        progressDialog.show(); // Show the dialog

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid(); // Get the logged-in user's UID
        }
        database = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                    String firstnameval = snapshot.child("firstname").getValue(String.class);
                    String middlenameval = snapshot.child("middlename").getValue(String.class);
                    String lastnameval = snapshot.child("lastname").getValue(String.class);

                    String name = firstnameval + " " + lastnameval;
                    String fullnameval = firstnameval + " " + middlenameval + " " + lastnameval;
                    proffullname.setText(fullnameval);

                    if (profilePictureUrl != null) {
                        Picasso.get()
                                .load(profilePictureUrl)
                                .fit()
                                .centerCrop()
                                .into(profilepic, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        progressDialog.dismiss(); // Dismiss when image loads successfully
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        progressDialog.dismiss(); // Dismiss even if there's an error
                                        profilepic.setImageResource(R.drawable.profile_icon); // Set default image
                                    }
                                });
                    } else {
                        progressDialog.dismiss(); // Dismiss if no profile picture URL exists
                        profilepic.setImageResource(R.drawable.profile_icon); // Default profile picture
                    }
                } else {
                    progressDialog.dismiss();
                    proffullname.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                proffullname.setText("Error fetching data");
            }
        });

        profile = view.findViewById(R.id.btnProfProfile);
        profile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), Professor_settings_profile.class);
            startActivity(intent);
        });

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();  // Goes back to the previous activity
            }
        });

        resetpass = view.findViewById(R.id.btnProfResetPassword);
        resetpass.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), Professor_settings_resetpassword.class);
            startActivity(intent);
        });

        privacy = view.findViewById(R.id.btnprivacy);
        privacy.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), privacyPolicy.class);
            startActivity(intent);
        });

        terms = view.findViewById(R.id.btnTermsandConditions);
        terms.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TermsandConditions.class);
            startActivity(intent);
        });

        logout = view.findViewById(R.id.btnProfLogout);
        logout.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Log out from Firebase
                        FirebaseAuth.getInstance().signOut();

                        // Redirect to Login Activity
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Dismiss the dialog
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        });
        return view;
    }
}