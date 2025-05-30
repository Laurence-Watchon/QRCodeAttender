package com.example.appdev50;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Calendar;

public class Professor_settings_editprofile extends AppCompatActivity {

    private EditText editFirstName, editMiddleName, editLastName, editEmail, editDateOfBirth, editGender, editMobileNumber, editAddress;
    private Button btnSave, btnupload;
    private DatabaseReference database;
    private String uid;
    private DatePickerDialog picker;
    private ImageView profilepic;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private boolean pictureChanged = false;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private StorageReference storagereference;
    private String originalFirstName, originalMiddleName, originalLastName, originalEmail, originalDateOfBirth, originalGender, originalMobileNumber, originalAddress;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_settings_editprofile);

        noInternet.showPendulumDialog(this, getLifecycle());

        try {
            editFirstName = findViewById(R.id.editFirstName);
            editMiddleName = findViewById(R.id.editMiddleName);
            editLastName = findViewById(R.id.editLastName);
            editEmail = findViewById(R.id.editEmail);
            editDateOfBirth = findViewById(R.id.editDateOfBirth);
            editDateOfBirth.setOnClickListener(v -> {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                picker = new DatePickerDialog(Professor_settings_editprofile.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        editDateOfBirth.setText(i2 + "/" + (i1 + 1) + "/" + i);
                    }
                }, year, month, day);
                picker.show();
            });
            editGender = findViewById(R.id.editGender);
            editMobileNumber = findViewById(R.id.editMobileNumber);
            editAddress = findViewById(R.id.editAddress);
            btnSave = findViewById(R.id.btnSave);
            profilepic = findViewById(R.id.profprofile_image);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                uid = user.getUid();
                Uri uri = user.getPhotoUrl();
                if(uri != null){
                    Picasso.get().load(uri).into(profilepic);
                }
            }
            storagereference = FirebaseStorage.getInstance().getReference("DisplayPictures");
            btnupload = findViewById(R.id.btnUpload);
            btnupload.setOnClickListener(v -> {
                openFileChooser();
            });

            database = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid);
            // Load original values for comparison
            loadUserProfile();
            btnSave.setOnClickListener(v -> saveChanges());

        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileChooser() {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while choosing the file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                imageUri = data.getData();
                checkAndSetImage(imageUri);
                pictureChanged = true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "An error occurred while loading the image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndSetImage(Uri uri) {
        // Check file size
        try (Cursor returnCursor = getContentResolver().query(uri, null, null, null, null)) {
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();
            long fileSize = returnCursor.getLong(sizeIndex);

            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(this, "File is too large. Please select an image under 2 MB.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error checking file size.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load image with Picasso and resize
        Picasso.get()
                .load(uri)
                .resize(500, 500) // Resize to a manageable size (500x500 for example)
                .centerCrop()
                .into(profilepic);
    }

    private void uploadpic() {
        if (imageUri != null) {
            try {
                // Re-validate file size before upload
                ContentResolver resolver = getContentResolver();
                Cursor returnCursor = resolver.query(imageUri, null, null, null, null);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                long fileSize = returnCursor.getLong(sizeIndex);
                returnCursor.close();

                if (fileSize > MAX_FILE_SIZE) {
                    Toast.makeText(this, "File is too large. Please select an image under 2 MB.", Toast.LENGTH_SHORT).show();
                    return;
                }

                StorageReference fileReference = storagereference.child(uid + "/" + System.currentTimeMillis() + "." + getFileExtension(imageUri));
                fileReference.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUri = uri.toString();
                            database.child("profilePictureUrl").setValue(downloadUri)
                                    .addOnSuccessListener(aVoid -> {
                                        Picasso.get()
                                                .load(downloadUri)
                                                .resize(500, 500) // Resize to avoid memory issues
                                                .centerCrop()
                                                .into(profilepic);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show();
                                    });

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri).build();
                            FirebaseAuth.getInstance().getCurrentUser().updateProfile(profileUpdates);
                        }))
                        .addOnFailureListener(e -> {
                            Toast.makeText(Professor_settings_editprofile.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                        });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "An error occurred while processing the file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadUserProfile() {
        // Load data from Firebase and set it to EditTexts (like in your profile view)
        // Store these values in original variables for comparison on save
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        Picasso.get()
                                .load(profilePictureUrl)
                                .fit() // Ensures the image fits the ImageView dimensions
                                .centerCrop() // Ensures the image is cropped to fit a circle or specific dimensions
                                .into(profilepic);
                    } else {
                        profilepic.setImageResource(R.drawable.profile_icon); // Load default profile picture
                    }
                    originalFirstName = snapshot.child("firstname").getValue(String.class);
                    originalMiddleName = snapshot.child("middlename").getValue(String.class);
                    originalLastName = snapshot.child("lastname").getValue(String.class);
                    originalEmail = snapshot.child("email").getValue(String.class);
                    originalDateOfBirth = snapshot.child("dateofbirth").getValue(String.class);
                    originalGender = snapshot.child("gender").getValue(String.class);
                    originalMobileNumber = snapshot.child("mobilenumber").getValue(String.class);
                    originalAddress = snapshot.child("address").getValue(String.class);

                    // Set original values to EditTexts
                    editFirstName.setText(originalFirstName);
                    editMiddleName.setText(originalMiddleName);
                    editLastName.setText(originalLastName);
                    editEmail.setText(originalEmail);
                    editDateOfBirth.setText(originalDateOfBirth);
                    editGender.setText(originalGender);
                    editMobileNumber.setText(originalMobileNumber);
                    editAddress.setText(originalAddress);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_settings_editprofile.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String firstName = formatName(editFirstName.getText().toString());
        String middleName = formatName(editMiddleName.getText().toString());
        String lastName = formatName(editLastName.getText().toString());
        String email = editEmail.getText().toString();
        String dateOfBirth = editDateOfBirth.getText().toString();
        String gender = formatName(editGender.getText().toString());
        String mobileNumber = editMobileNumber.getText().toString();
        String address = editAddress.getText().toString();

        // Check for empty fields and set error
        if (TextUtils.isEmpty(firstName)) {
            editFirstName.setError("First Name is required");
            editFirstName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            editLastName.setError("Last Name is required");
            editLastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            editEmail.setError("Valid email address is required.");
            editEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(dateOfBirth)){
            editDateOfBirth.setError("Please enter your date of birth.");
            editDateOfBirth.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(gender)){
            editGender.setError("Please enter your gender.");
            editGender.requestFocus();
            return;
        }

        if (!gender.equals("Female") && !gender.equals
                ("Male")) {
            editGender.setError("Please enter a valid gender (Male/Female)");
            editGender.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mobileNumber)) {
            editMobileNumber.setError("Mobile Number is required");
            editMobileNumber.requestFocus();
            return;
        }

        if (mobileNumber.length() != 11 || !mobileNumber.startsWith("09")) {
            editMobileNumber.setError("Please enter valid mobile number");
            editMobileNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            editAddress.setError("Address is required");
            editAddress.requestFocus();
            return;
        }

        if (address.length() < 5) {
            editAddress.setError("Please enter valid address");
            editAddress.requestFocus();
            return;
        }

        if (imageUri != null) {
            uploadpic();
        }

        if (noChangesMade(firstName, middleName, lastName, email, dateOfBirth, gender, mobileNumber, address, pictureChanged)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Changes")
                    .setMessage("There are no changes to save.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        } else {
            if (pictureChanged) {
                uploadpic();  // Upload the profile picture if it was changed
            }
            // Update fields in Firebase if there are changes
            database.child("firstname").setValue(firstName);
            database.child("middlename").setValue(middleName);
            database.child("lastname").setValue(lastName);
            database.child("email").setValue(email);
            database.child("dateofbirth").setValue(dateOfBirth);
            database.child("gender").setValue(gender);
            database.child("mobilenumber").setValue(mobileNumber);
            database.child("address").setValue(address);
            pictureChanged = false; // Reset picture change flag after saving
            Toast.makeText(this, "Data successfully updated.", Toast.LENGTH_SHORT).show();
            finish(); // Go back to the previous screen
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    // Helper method to format names (capitalizes the first letter of each word)
    private String formatName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String[] words = name.toLowerCase().trim().split(" ");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }

        return formattedName.toString().trim();
    }

    private boolean noChangesMade(String firstName, String middleName, String lastName, String email, String dateOfBirth,
                                  String gender, String mobileNumber, String address, boolean pictureChanged) {
        return TextUtils.equals(firstName, originalFirstName) &&
                TextUtils.equals(middleName, originalMiddleName) &&
                TextUtils.equals(lastName, originalLastName) &&
                TextUtils.equals(email, originalEmail) &&
                TextUtils.equals(dateOfBirth, originalDateOfBirth) &&
                TextUtils.equals(gender, originalGender) &&
                TextUtils.equals(mobileNumber, originalMobileNumber) &&
                TextUtils.equals(address, originalAddress) &&
                !pictureChanged;
    }
}