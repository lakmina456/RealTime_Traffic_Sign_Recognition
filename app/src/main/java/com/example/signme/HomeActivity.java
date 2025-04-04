package com.example.signme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.example.signme.detection.Main;
import com.example.signme.detection.Main2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageButton driveButton;
    private ImageButton profileButton;
    private ImageButton recordButton;
    private ImageButton performanceButton;
    private ImageButton historyButton;
    private ImageButton logoutButton;
    private Button test1;
    private ConnectionClass connectionClass;
    private String firstName; // To store user's first name
    private String email; // To store user's email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        connectionClass = new ConnectionClass();

        //connect();

        // Initialize views
        welcomeText = findViewById(R.id.welcomeText);
        driveButton = findViewById(R.id.driveButton);
        profileButton = findViewById(R.id.profileButton);
        recordButton = findViewById(R.id.recordButton);
        performanceButton = findViewById(R.id.performanceButton);
        historyButton = findViewById(R.id.historyButton);
        logoutButton = findViewById(R.id.logoutButton);  // Initialize the logout button
        test1 = findViewById(R.id.button1);
        // Get email from intent
        email = getIntent().getStringExtra("email");

        // Fetch user's first name from database
        fetchFirstName();

        // Set click listeners
        setupButtonListeners();

        // Set up logout button listener
        logoutButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            showLogoutDialog();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data if needed when returning to this activity
        fetchFirstName();
    }

    private void fetchFirstName() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            ConnectionClass connectionClass = new ConnectionClass();
            Connection con = connectionClass.CONN();
            if (con == null) {
                runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                return;
            }

            String query = "SELECT FIRST_NAME FROM driver WHERE EMAIL = ?";
            try {
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    firstName = rs.getString("FIRST_NAME");
                    runOnUiThread(() -> welcomeText.setText("Welcome " + firstName + "!"));
                }
                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupButtonListeners() {
        driveButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            navigateToDriveActivity();
        });
        test1.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            test();
        });

        recordButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            navigateToRecordActivity();
        });
        profileButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            navigateToProfileActivity();
        });
        performanceButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            navigateToAnalyticsActivity();
        });
        historyButton.setOnClickListener(v -> {
            AnimationUtil.applyZoomInAnimation(this, v);
            navigateToHistoryActivity();
        });
    }

    private void navigateToDriveActivity() {
        Intent driveIntent = new Intent(HomeActivity.this, DriveActivity2.class);
        driveIntent.putExtra("email", email);
        startActivity(driveIntent);
    }
    private void test() {
        Intent driveIntent = new Intent(HomeActivity.this, Test_Audio1.class);

        startActivity(driveIntent);
    }

    private void navigateToHistoryActivity() {
        Intent historyIntent = new Intent(HomeActivity.this, HistoryActivity.class);
        historyIntent.putExtra("email", email);
        startActivity(historyIntent);
    }

    private void navigateToAnalyticsActivity() {
        Intent intent = new Intent(HomeActivity.this, MainAnalyticsActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }

    private void navigateToRecordActivity() {
        Intent intent = new Intent(HomeActivity.this, Main2.class);
        startActivity(intent);
    }

    private void navigateToProfileActivity() {
        Intent profileIntent = new Intent(HomeActivity.this, ProfileActivity.class);
        profileIntent.putExtra("email", email);
        startActivity(profileIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        moveTaskToBack(true); // Minimize the app to the background
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Perform logout actions
                    logout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
/*
    private void connect() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN(); // Establishing database connection
                String str;
                if (con == null) {
                    str = "Error in connection with server";
                } else {
                    str = "Connected with server";

                }
                con.close();
                runOnUiThread(() -> Toast.makeText(this, str, Toast.LENGTH_SHORT).show()); // Show connection status
            } catch (Exception e) {
                e.printStackTrace(); // Log the exception for debugging
            }
             // Close the database connection
        });
        //executorService.shutdown();

    }
*/
    private void logout() {
        // Clear login state
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // Clear all the saved data
        editor.apply();

        // Show logout message
        Toast.makeText(this, "Successfully Logged Out", Toast.LENGTH_SHORT).show();

        // Navigate to MainActivity
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
