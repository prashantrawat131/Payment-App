package com.example.mobilepaymentapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilepaymentapp.databinding.ActivitySettingsBinding;
import com.example.mobilepaymentapp.databinding.ChangePasswordAlertDialogLayoutBinding;
import com.example.mobilepaymentapp.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatActivity {

    ActivitySettingsBinding binding;
    AlertDialog changePasswordAlert;
    DatabaseReference database;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String userPhoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        database = FirebaseDatabase.getInstance().getReference();


        userPhoneNumber = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");

        binding.changePassword.setOnClickListener(view -> {
            showChangePasswordDialog();
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        View alertDialogView = getLayoutInflater().inflate(R.layout.change_password_alert_dialog_layout, null, false);

        ChangePasswordAlertDialogLayoutBinding binding = ChangePasswordAlertDialogLayoutBinding.inflate(getLayoutInflater(), null, false);


        binding.progressBar.setVisibility(View.GONE);

        binding.saveButton.setOnClickListener(view -> {

            CommonOperations.log("Clicked");
            String previousPassword = binding.previousPassword.getText().toString();
            String newPassword = binding.newPassword.getText().toString();
            String confirmPassword = binding.confirmNewPassword.getText().toString();


            if (previousPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                CommonOperations.toast("Please fill all the details", getApplicationContext());
                CommonOperations.log("Please fill all the details");
                return;
            }

            if (!binding.confirmNewPassword.getText().toString().equals(binding.newPassword.getText().toString())) {
                CommonOperations.toast("New Password doesn't match confirm password", getApplicationContext());
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);

            String originalPassword = sharedPreferences.getString(Constants.SP_USER_PASSWORD, "-vevervverv");
            if (previousPassword.equals(originalPassword)) {
                database.child("users").child(userPhoneNumber).get()
                        .addOnCompleteListener(task -> {
                            User user = task.getResult().getValue(User.class);
                            user.setPassword(newPassword);

                            database.child("users").child(userPhoneNumber).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    CommonOperations.toast("Password change successful", getApplicationContext());
                                    binding.doneLayout.setVisibility(View.VISIBLE);
                                    binding.progressBar.setVisibility(View.GONE);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(() -> {
                                                editor.putString(Constants.SP_USER_PASSWORD, newPassword);
                                                editor.apply();
                                                changePasswordAlert.dismiss();
                                            });
                                        }
                                    }, 2000);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    CommonOperations.toast("Password updation failed please check and try again.", getApplicationContext());
                                    binding.progressBar.setVisibility(View.GONE);
                                }
                            });
                        });
            } else {
                CommonOperations.toast("Wrong password", getApplicationContext());
                binding.progressBar.setVisibility(View.GONE);
            }
        });


        builder.setView(binding.getRoot());


        changePasswordAlert = builder.create();

        changePasswordAlert.show();
    }

}