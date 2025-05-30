package com.finals.appdev50;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class Professor_settings_profile extends AppCompatActivity {

    DatabaseReference database;
    TextView proffullname, profemail, profdateofBirth, profgender, profmobileNumber, profaddress, profmessage;
    String uid;
    private ImageView profilepic;
    private Button edit;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_settings_profile);

        noInternet.showPendulumDialog(this, getLifecycle());

        proffullname = findViewById(R.id.proftvname);
        profemail = findViewById(R.id.proftvemail);
        profdateofBirth = findViewById(R.id.proftvdate_of_birth);
        profgender = findViewById(R.id.proftvgender);
        profmobileNumber = findViewById(R.id.proftvmobile_number);
        profaddress = findViewById(R.id.proftvaddress);
        profmessage = findViewById(R.id.profwelcome_message);
        profilepic = findViewById(R.id.profprofile_image);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        edit = findViewById(R.id.btnEditProfile);
        edit.setOnClickListener(v -> {
            Intent intent = new Intent(Professor_settings_profile.this, Professor_settings_editprofile.class);
            startActivity(intent);
            finish();
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false); // Disable canceling the dialog
        progressDialog.show();

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
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        // Load image using Picasso with a callback
                        Picasso.get()
                                .load(profilePictureUrl)
                                .fit()
                                .centerCrop()
                                .into(profilepic, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        // Dismiss progress dialog on success
                                        progressDialog.dismiss();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Handle error and dismiss dialog
                                        progressDialog.dismiss();
                                        profilepic.setImageResource(R.drawable.profile_icon); // Load default profile picture
                                    }
                                });
                    } else {
                        // No profile picture URL; use default
                        profilepic.setImageResource(R.drawable.profile_icon);
                        progressDialog.dismiss();
                    }

                    String firstnameval = snapshot.child("firstname").getValue(String.class);
                    String middlenameval = snapshot.child("middlename").getValue(String.class);
                    String lastnameval = snapshot.child("lastname").getValue(String.class);
                    String emailVal = snapshot.child("email").getValue(String.class);
                    String dob = snapshot.child("dateofbirth").getValue(String.class);
                    String genderVal = snapshot.child("gender").getValue(String.class);
                    String mobile = snapshot.child("mobilenumber").getValue(String.class);
                    String addr = snapshot.child("address").getValue(String.class);

                    String name = firstnameval + " " + lastnameval;
                    String fullnameval = firstnameval + " " + middlenameval + " " + lastnameval;
                    // Set the values in the TextViews
                    profmessage.setText("Welcome, " + name);
                    proffullname.setText(fullnameval);
                    profemail.setText(emailVal);
                    profdateofBirth.setText(dob);
                    profgender.setText(genderVal);
                    profmobileNumber.setText(mobile);
                    profaddress.setText(addr);
                } else {
                    proffullname.setText("N/A");
                    profemail.setText("N/A");
                    profdateofBirth.setText("N/A");
                    profgender.setText("N/A");
                    profmobileNumber.setText("N/A");
                    profaddress.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                proffullname.setText("Error fetching data");
            }
        });
    }
}