package lk.jiat.bonsaimart.fagment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import lk.jiat.bonsaimart.activity.SignInActivity;
import lk.jiat.bonsaimart.databinding.FragmentProfileBinding;
import lk.jiat.bonsaimart.model.User;
import lk.jiat.bonsaimart.util.PermissionHelper;
import lk.jiat.bonsaimart.util.PreferenceManager;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(requireContext());

        loadUserData();

        binding.profileBtnUpdate.setOnClickListener(v -> processUpdates());
        binding.profileBtnLogout.setOnClickListener(v -> logout());

        // ============================================================
        // PROFILE IMAGE CLICK - Pick from Gallery
        // ============================================================
        binding.profileImage.setOnClickListener(v -> {
            requestStoragePermissionAndPickImage();
        });

        getActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                }
        );
    }

    // ============================================================
    // IMAGE PICKER LAUNCHER
    // ============================================================
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (!isAdded() || binding == null) return;

                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {

                            Uri imageUri = result.getData().getData();

                            if (imageUri != null) {

                                // Show in Profile page
                                Glide.with(this)
                                        .load(imageUri)
                                        .circleCrop()
                                        .into(binding.profileImage);

                                // Save to Internal Storage
                                saveProfileImageToInternalStorage(imageUri);

                                // ============================================================
                                // SET FLAG - Notify MainActivity to reload image
                                // ============================================================
                                if (preferenceManager != null) {
                                    preferenceManager.setProfileImageUpdated(true);
                                    Log.d(TAG, "Profile image update flag set");
                                }

                                Toast.makeText(getContext(),
                                        "Profile picture updated!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    // ============================================================
    // REQUEST STORAGE PERMISSION THEN PICK IMAGE
    // ============================================================
    private void requestStoragePermissionAndPickImage() {

        if (PermissionHelper.hasStoragePermission(requireContext())) {
            pickImageFromGallery();
        } else {
            PermissionHelper.requestStoragePermission(requireActivity());
        }
    }

    // ============================================================
    // PICK IMAGE FROM GALLERY
    // ============================================================
    private void pickImageFromGallery() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // ============================================================
    // PERMISSION RESULT HANDLER
    // ============================================================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.REQUEST_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getContext(),
                        "Storage permission granted",
                        Toast.LENGTH_SHORT).show();

                pickImageFromGallery();

            } else {
                Toast.makeText(getContext(),
                        "Storage permission denied",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ============================================================
    // LOAD USER DATA
    // ============================================================
    private void loadUserData() {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (!isAdded() || binding == null) return;

                        if (documentSnapshot.exists()) {
                            User userModel = documentSnapshot.toObject(User.class);
                            if (userModel != null) {

                                binding.profileInputName.setText(userModel.getName());
                                binding.profileInputEmail.setText(userModel.getEmail());

                                // Load profile image from Internal Storage
                                Bitmap savedImage = loadProfileImageFromInternalStorage();
                                if (savedImage != null) {
                                    binding.profileImage.setImageBitmap(savedImage);
                                    Log.d(TAG, "Profile image loaded from internal storage");
                                }
                                // If no local image, load from Firestore URL
                                else if (userModel.getProfilePicUrl() != null
                                        && !userModel.getProfilePicUrl().isEmpty()) {

                                    Glide.with(ProfileFragment.this)
                                            .load(userModel.getProfilePicUrl())
                                            .placeholder(android.R.drawable.ic_menu_myplaces)
                                            .circleCrop()
                                            .into(binding.profileImage);
                                }
                            }
                        }
                    });
        }
    }

    // ============================================================
    // PROCESS UPDATES
    // ============================================================
    private void processUpdates() {

        if (!isAdded() || binding == null) return;

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String newName = binding.profileInputName.getText().toString().trim();
        String newEmail = binding.profileInputEmail.getText().toString().trim();
        String oldPassword = binding.profileInputOldPassword.getText().toString();
        String newPassword = binding.profileInputNewPassword.getText().toString();
        String confirmPassword = binding.profileInputConfirmPassword.getText().toString();

        if (newName.isEmpty()) {
            binding.profileInputName.setError("Name is required");
            return;
        }

        // Update Name in Firestore
        updateNameInFirestore(user.getUid(), newName);

        // Handle Email/Password Change
        if (!newEmail.equals(user.getEmail()) || !newPassword.isEmpty()) {

            if (oldPassword.isEmpty()) {
                binding.profileInputOldPassword.setError("Required");
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
            user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {

                if (!isAdded() || binding == null) return;

                if (reAuthTask.isSuccessful()) {

                    if (!newEmail.equals(user.getEmail())) {
                        updateEmail(user, newEmail);
                    }

                    if (!newPassword.isEmpty()) {
                        if (newPassword.equals(confirmPassword)) {
                            updatePassword(user, newPassword);
                        } else {
                            binding.profileInputConfirmPassword.setError("Passwords do not match");
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateNameInFirestore(String uid, String name) {
        db.collection("users").document(uid).update("name", name)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(getContext(), "Name Updated!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmail(FirebaseUser user, String newEmail) {
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (!isAdded() || binding == null) return;

            if (task.isSuccessful()) {
                db.collection("users").document(user.getUid()).update("email", newEmail);
                Toast.makeText(getContext(), "Verification email sent", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Email Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (!isAdded() || binding == null) return;

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Password Updated!", Toast.LENGTH_SHORT).show();
                binding.profileInputOldPassword.setText("");
                binding.profileInputNewPassword.setText("");
                binding.profileInputConfirmPassword.setText("");
            } else {
                Toast.makeText(getContext(), "Password Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // SAVE IMAGE TO INTERNAL STORAGE
    // ============================================================
    private void saveProfileImageToInternalStorage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext()
                    .getContentResolver().openInputStream(imageUri);

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (inputStream != null) inputStream.close();
            if (bitmap == null) return;

            FileOutputStream fos = requireContext()
                    .openFileOutput("profile_pic.jpg", android.content.Context.MODE_PRIVATE);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Log.d(TAG, "Profile image saved to internal storage");

        } catch (IOException e) {
            Log.e(TAG, "Failed to save image", e);
        }
    }

    // ============================================================
    // LOAD IMAGE FROM INTERNAL STORAGE
    // ============================================================
    private Bitmap loadProfileImageFromInternalStorage() {
        try {
            FileInputStream fis = requireContext()
                    .openFileInput("profile_pic.jpg");

            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            return bitmap;

        } catch (IOException e) {
            return null;
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    private void logout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}