package com.example.mobilepaymentapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilepaymentapp.databinding.ActivityPaymentBinding;
import com.example.mobilepaymentapp.model.Transaction;
import com.example.mobilepaymentapp.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class PaymentActivity extends AppCompatActivity {

    ActivityPaymentBinding binding;
    int amount = 0;
    String receiverNumber = "-1", receiverName = "", senderNumber = "";
    DatabaseReference database;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();

        receiverNumber = getIntent().getExtras().getString("receiverNumber", "-1");
        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        senderNumber = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");

        binding.receiverNumberTv.setText(receiverNumber);
        CommonOperations.log("Receiver's number in payment activity is " + receiverNumber);

        database = FirebaseDatabase.getInstance().getReference();

        binding.passwordInputLayout.setVisibility(View.GONE);
        binding.passwordSubmitButton.setVisibility(View.GONE);

        binding.amountSubmitButton.setVisibility(View.VISIBLE);
        binding.amountInputLayout.setVisibility(View.VISIBLE);

        binding.transactionCompleteLayout.setVisibility(View.GONE);

        setUpReceiverDetails();

        binding.amountSubmitButton.setOnClickListener(view -> {
            amountSubmitClicked();
        });

        binding.passwordSubmitButton.setOnClickListener(view -> {
            passwordSubmitButtonClicked();
        });
    }

    private void passwordSubmitButtonClicked() {
        try {
            String password = binding.passwordInputEt.getText().toString();
            if (password.trim().isEmpty()) {
                CommonOperations.toast("Please enter the password", getApplicationContext());
                return;
            }

            if (password.equals(sharedPreferences.getString(Constants.SP_USER_PASSWORD, "vegr54r34frggre3g5hgty465h545h54g54g43g34"))) {
                performTransaction();
            } else {
                CommonOperations.toast("Wrong password", getApplicationContext());
            }
        } catch (Exception e) {
            CommonOperations.log("Error: " + e.getMessage());
        }
    }

    private void performTransaction() {
        MoneyTransaction moneyTransaction = new MoneyTransaction(getApplicationContext());
        moneyTransaction.sendMoney(receiverNumber, amount, new TransactionListeners() {
            @Override
            public void insufficientAmount() {
                binding.passwordInputLayout.setVisibility(View.GONE);
                binding.passwordSubmitButton.setVisibility(View.GONE);
                binding.amountSubmitButton.setVisibility(View.VISIBLE);
                binding.amountInputLayout.setVisibility(View.VISIBLE);

                binding.passwordInputEt.setText("");
                CommonOperations.toast("Insufficient amount", getApplicationContext());
            }

            @Override
            public void error(String message) {
                CommonOperations.toast("Some error occurred: " + message, getApplicationContext());
            }

            @Override
            public void success() {
                createTransaction();
                showSuccessTransaction();
            }
        });
    }

    private void createTransaction() {
        long time = Calendar.getInstance().getTimeInMillis();
        String transactionId = md5(senderNumber + receiverNumber + time);
        binding.transactionIdTv.setText(transactionId);
        Transaction transaction = new Transaction(transactionId, senderNumber, receiverNumber, amount, time);
        database.child("transactions").child(senderNumber).child(transactionId).setValue(transaction);
        database.child("transactions").child(receiverNumber).child(transactionId).setValue(transaction);
    }

    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            CommonOperations.log("Error: " + e.getMessage());
        }
        return "";
    }

    private void showSuccessTransaction() {
        binding.passwordSubmitButton.setVisibility(View.GONE);
        binding.amountSubmitButton.setVisibility(View.GONE);
        binding.transactionCompleteLayout.setVisibility(View.VISIBLE);
        binding.passwordInputLayout.setVisibility(View.GONE);
        binding.amountInputLayout.setVisibility(View.GONE);
    }

    private void goToHomeActivity() {
        onBackPressed();
    }

    private void amountSubmitClicked() {
        try {
            amount = Integer.parseInt(binding.amountInputEt.getText().toString());
            if (amount > 0) {
                binding.amountInputLayout.setVisibility(View.GONE);
                binding.amountSubmitButton.setVisibility(View.GONE);
                binding.passwordInputLayout.setVisibility(View.VISIBLE);
                binding.passwordSubmitButton.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            CommonOperations.log("Error: " + e.getMessage());
        }
    }

    private void setUpReceiverDetails() {
        database.child("users").child(receiverNumber).get().addOnCompleteListener(
                task -> {
                    try {
                        User user = task.getResult().getValue(User.class);
                        receiverName = user.getName();
                        binding.receiverNameTv.setText(receiverName);
                    } catch (Exception e) {
                        CommonOperations.log("Error: " + e.getMessage());
                    }
                }
        ).addOnFailureListener(e -> CommonOperations.log("Failure in loading the receiver details"));
    }
}