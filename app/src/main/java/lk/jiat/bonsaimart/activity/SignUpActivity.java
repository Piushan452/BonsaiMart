package lk.jiat.bonsaimart.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.jiat.bonsaimart.databinding.ActivitySignUpBinding;
import lk.jiat.bonsaimart.model.User;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding.signupBtnGotoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signupBtnRegister.setOnClickListener(view -> {

            String name = binding.signupInputName.getText().toString().trim();
            String email = binding.signupInputEmail.getText().toString().trim();
            String password = binding.signupInputPassword.getText().toString().trim();
            String retypePassword = binding.signupInputRetypePassword.getText().toString().trim();

            if (name.isEmpty()) {
                binding.signupInputName.setError("Name is required!");
                return;
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signupInputEmail.setError("Enter valid email!");
                return;
            }

            if (password.length() < 6) {
                binding.signupInputPassword.setError("Password must be at least 6 characters!");
                return;
            }

            if (!retypePassword.equals(password)) {
                binding.signupInputRetypePassword.setError("Password does not match!");
                return;
            }

            binding.signupBtnRegister.setEnabled(false);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();

                            User user = User.builder()
                                    .uid(uid)
                                    .name(name)
                                    .email(email)
                                    .build();

                            firebaseFirestore.collection("users").document(uid)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(SignUpActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        binding.signupBtnRegister.setEnabled(true);
                                        Toast.makeText(SignUpActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        Log.e("FirestoreError", e.getMessage());
                                    });
                        } else {
                            binding.signupBtnRegister.setEnabled(true);
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                            Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("AuthError", errorMessage);
                        }
                    });
        });
    }
}
