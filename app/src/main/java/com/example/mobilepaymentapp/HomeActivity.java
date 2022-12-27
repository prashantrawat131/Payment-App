package com.example.mobilepaymentapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mobilepaymentapp.databinding.ActivityHomeBinding;
import com.example.mobilepaymentapp.databinding.PhoneNumberInputDialogBinding;
import com.example.mobilepaymentapp.model.User;
import com.example.mobilepaymentapp.qrgenerator.QRGContents;
import com.example.mobilepaymentapp.qrgenerator.QRGEncoder;
import com.example.mobilepaymentapp.qrscanner.SmallCaptureActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.concurrent.Executor;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    ActionBarDrawerToggle actionBarDrawerToggle;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private ActivityHomeBinding binding;
    Bitmap qrBitmap = null;
    User user = null;

    AlertDialog alertDialogForPhoneNumber = null;

    String userPhoneNumber = "";

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userPhoneNumber = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");

        askCameraPermission();

        initialSetUp();

        showQr();

        loadAndShowName();

        binding.testButton.setOnClickListener(view -> {
            testing();
        });

        setUpBioMetricAuthentication();

        setUpNavView();
    }

    private void showAlertForPhoneNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        PhoneNumberInputDialogBinding binding = PhoneNumberInputDialogBinding.inflate(getLayoutInflater());
        builder.setView(binding.getRoot());

        binding.nextButton.setOnClickListener(view -> {
            String phoneNumber = binding.phoneNumberEt.getText().toString();
            if (phoneNumber.length() != 10 || phoneNumber.trim().equals("")) {
                CommonOperations.toast("Wrong phone number", getApplicationContext());
                return;
            }

            goToPaymentActivity(phoneNumber);
            alertDialogForPhoneNumber.dismiss();
        });

        alertDialogForPhoneNumber = builder.create();
        alertDialogForPhoneNumber.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            goToSettings();
        } else if (item.getItemId() == R.id.logout) {
            logout();
        }
        return true;
    }

    private void setUpNavView() {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, R.string.nav_drawer_open, R.string.nav_drawer_close);

        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        actionBarDrawerToggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);


        View headerView = binding.navView.getHeaderView(0);

        TextView nameTv = headerView.findViewById(R.id.nav_header_name);
        nameTv.setText(sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1"));

    }


    private void setUpBioMetricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(HomeActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();

                finishAffinity();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();

                finishAffinity();
            }
        });

        BiometricPrompt.PromptInfo.Builder biometricPromptBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel");

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//            biometricPromptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL);
//        }

        promptInfo = biometricPromptBuilder.build();


        mDatabase.child("settings").get().addOnCompleteListener(task -> {
            try {
                DataSnapshot dataSnapshot = task.getResult();
                boolean biometric = (boolean) dataSnapshot.child("biometric").getValue();
                if (biometric) {
                    biometricPrompt.authenticate(promptInfo);
                }
            } catch (Exception e) {
                CommonOperations.log("Error: " + e.getMessage());
            }
        });
    }

    private void loadAndShowName() {
    }

    private void testing() {
        MoneyTransaction moneyTransaction = new MoneyTransaction(getApplicationContext());

        moneyTransaction.sendMoney("8587096891", 2, new TransactionListeners() {
            @Override
            public void insufficientAmount() {
                CommonOperations.log("Insufficient transaction");
            }

            @Override
            public void error(String message) {
                CommonOperations.log("Error: " + message);
            }

            @Override
            public void success() {
                CommonOperations.log("Transaction completed successfully");
            }
        });
    }

    private void initialSetUp() {
        binding.homeScanToPay.setOnClickListener(view -> {
            scanQrCode();
        });

        binding.homeTransactionHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(HomeActivity.this, "transaction history", Toast.LENGTH_SHORT).show();
                goToTransactionHistoryActivity();
            }
        });

        binding.homeNumberToPay.setOnClickListener(view -> {
            showAlertForPhoneNumber();
        });

        /*ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                User user = dataSnapshot.getValue(User.class);
                binding.balanceTv.setText("Rs. " + user.getBalance());
                // ..
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                CommonOperations.log("Error: " + databaseError.getMessage());
//                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);*/
    }

    private void goToTransactionHistoryActivity() {
        Intent intent = new Intent(HomeActivity.this, TransactionHistoryActivity.class);
        startActivity(intent);
    }

    private void askCameraPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    Constants.REQUEST_CODE_FOR_CAMERA_PERMISSION);
        }
    }

    private void showQr() {
        try {
            String inputValue = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");

            QRGEncoder qrgEncoder = new QRGEncoder(inputValue, null,
                    QRGContents.Type.PHONE, 1000);
            qrgEncoder.setColorBlack(Color.BLACK);
            qrgEncoder.setColorWhite(Color.WHITE);

            qrBitmap = qrgEncoder.getBitmap();

            binding.qrBelowTv.setText(inputValue);
            binding.qrImageView.setImageBitmap(qrBitmap);
            CommonOperations.log("Showing qr code for: " + inputValue);
        } catch (Exception e) {
            CommonOperations.log("Qr code error: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDatabase.child("users").child(userPhoneNumber).get().addOnCompleteListener(task -> {
            user = task.getResult().getValue(User.class);
            binding.balanceTv.setText("Rs. " + user.getBalance());
            binding.userNameTv.setText("" + user.getName());
        });
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {

//                goToPaymentActivity("9990897418");

                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d("MainActivity", "Cancelled scan");
                        Toast.makeText(HomeActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                        Toast.makeText(HomeActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("MainActivity", "Scanned");
                    Toast.makeText(HomeActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                    String[] parts = result.getContents().split(":");
                    goToPaymentActivity(parts[1]);
                }
            });

    private void goToPaymentActivity(String receiverNumber) {
        if (receiverNumber.equals(userPhoneNumber)) {
            CommonOperations.toast("Can't send money to yourself", getApplicationContext());
            return;
        }
        Intent intent = new Intent(HomeActivity.this, PaymentActivity.class);
        intent.putExtra("receiverNumber", receiverNumber);
        startActivity(intent);
    }

    public void scanQrCode() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setCaptureActivity(SmallCaptureActivity.class);
        options.setPrompt("Place the QR code inside the box to scan");
        barcodeLauncher.launch(options);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            logout();
        } else if (item.getItemId() == R.id.settings) {
            goToSettings();
        } else if (item.getItemId() == 16908332) {
            if (binding.drawerLayout.isOpen()) {
                binding.drawerLayout.close();
            } else {
                binding.drawerLayout.open();
            }
        }
        return true;
    }

    private void goToSettings() {
        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void logout() {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}