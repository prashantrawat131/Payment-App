package com.example.mobilepaymentapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilepaymentapp.databinding.ActivityLoginBinding;
import com.example.mobilepaymentapp.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        binding.registerLayout.registerMainLayout.setVisibility(View.GONE);

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        setUpCommonStuff();
        setUpLoginPage();
        setUpRegisterPage();
    }

    private void setUpRegisterPage() {
        binding.registerLayout.registerButton.setOnClickListener(v -> {
            String name = binding.registerLayout.registerFullName.getText().toString();
            String phoneNumber = binding.registerLayout.registerPhoneNumber.getText().toString();
            String email = binding.registerLayout.registerEmail.getText().toString();
            String password = binding.registerLayout.registerPassword.getText().toString();
            String confirmPassword = binding.registerLayout.registerConfirmPassword.getText().toString();

            if (!areAllFieldsValidForRegistration(name, email, phoneNumber, password, confirmPassword)) {
                return;
            }

            User user = new User(name, phoneNumber, email, password, 1000.00);

            mDatabase.child("users").child(phoneNumber).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    registerUser(user);
                    return;
                }

                DataSnapshot data = task.getResult();
                User user1 = data.getValue(User.class);
                if (user1 == null) {
                    registerUser(user);
                } else {
                    CommonOperations.toast("User already present", getApplicationContext());
                }
            });
        });
    }

    private boolean areAllFieldsValidForRegistration(String name, String email, String phoneNumber, String password, String confirmPassword) {
        if (name.isEmpty() ||
                phoneNumber.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty()) {
            CommonOperations.toast("Please fill all the fields", getApplicationContext());
            return false;
        }

        if (!isEmailValid(email)) {
            CommonOperations.toast("Invalid email", getApplicationContext());
            return false;
        }

        if (phoneNumber.length() != 10) {
            CommonOperations.toast("Invalid phone number", getApplicationContext());
            return false;
        }

        if (!password.equals(confirmPassword)) {
            CommonOperations.toast("Confirm password doesn't match password", getApplicationContext());
            return false;
        }

        return true;
    }

    private boolean areAllFieldsValidForLogin(String phoneNumber, String password) {
        if (phoneNumber.isEmpty() ||
                password.isEmpty()) {
            CommonOperations.toast("Please fill all the fields", getApplicationContext());
            return false;
        }

        if (phoneNumber.length() != 10) {
            CommonOperations.toast("Invalid phone number", getApplicationContext());
            return false;
        }

        return true;
    }

    private boolean isEmailValid(String email) {
        Pattern VALID_EMAIL_ADDRESS_REGEX =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
        return matcher.find();
    }

    private void registerUser(User user) {
        Task<Void> task = mDatabase.child("users").child(user.getPhoneNumber()).setValue(user);
        task.addOnSuccessListener(s -> {
            registerSuccessful(user);
        });
        task.addOnFailureListener(e -> registerFailed());
    }

    private void registerSuccessful(User user) {
        editor.putBoolean(Constants.SP_USER_LOGGED_IN, true);
        editor.apply();

        editor.putString(Constants.SP_USER_PHONE_NUMBER, user.getPhoneNumber());
        editor.apply();

        editor.putString(Constants.SP_USER_PASSWORD, user.getPassword());
        editor.apply();

        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void registerFailed() {
        CommonOperations.toast("Register failed", getApplicationContext());
    }

    private void setUpCommonStuff() {
        binding.singUpOrLoginToggleTv.setOnClickListener(view -> {
            if (binding.loginLayout.loginMainLayout.getVisibility() == View.VISIBLE) {
                binding.registerLayout.registerMainLayout.setVisibility(View.VISIBLE);
                binding.loginLayout.loginMainLayout.setVisibility(View.GONE);
                binding.singUpOrLoginToggleTv.setText("Already have an account?\nWanna Login");
            } else {
                binding.registerLayout.registerMainLayout.setVisibility(View.GONE);
                binding.loginLayout.loginMainLayout.setVisibility(View.VISIBLE);
                binding.singUpOrLoginToggleTv.setText("Do not have an account?\nWanna Register");
            }
        });
    }

    private void setUpLoginPage() {
        binding.loginLayout.loginButton.setOnClickListener(view -> {
            String phoneNumber = binding.loginLayout.loginPhoneNumberEt.getText().toString();
            String password = binding.loginLayout.loginPasswordEt.getText().toString();

            if (!areAllFieldsValidForLogin(phoneNumber, password)) {
                return;
            }

            mDatabase.child("users").child(phoneNumber).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    CommonOperations.log("Failed task for loading specific user");
                }

                try {
                    DataSnapshot data = task.getResult();
                    User user = data.getValue(User.class);
                    if (user == null) {
                        loginFailed();
                        return;
                    }

                    if (user.getPassword().equals(password)) {
                        loginSuccessFul(user);
                    } else {
                        loginFailed();
                    }
                } catch (Exception e) {
                    CommonOperations.log("Error: " + e.getMessage());
                }
            });
        });
    }

    private void loginSuccessFul(User user) {
        editor.putString(Constants.SP_USER_PHONE_NUMBER, user.getPhoneNumber());
        editor.apply();

        editor.putString(Constants.SP_USER_PASSWORD, user.getPassword());
        editor.apply();

        editor.putBoolean(Constants.SP_USER_LOGGED_IN, true);
        editor.apply();

        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFailed() {
        CommonOperations.toast("Login failed", getApplicationContext());
    }
}