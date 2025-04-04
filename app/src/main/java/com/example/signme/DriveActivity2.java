package com.example.signme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveActivity2 extends AppCompatActivity {

    // Database connection
    private ConnectionClass connectionClass;
    private Connection con;

    // UI components
    private Button startButton;
    private ImageView profileImage;
    private TextView profileName;
    private RadioButton radioButton1, radioButton2, radioButton3;
    private String email;
    private String startingPoint, destination,session_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive2);

        // Initialize connection class
        connectionClass = new ConnectionClass();

        // Bind UI components
        startButton = findViewById(R.id.startButton);
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        radioButton1 = findViewById(R.id.radioButton1);
        radioButton2 = findViewById(R.id.radioButton2);
        radioButton3 = findViewById(R.id.radioButton3);

        // Set listeners for RadioButtons
        radioButton1.setOnClickListener(v -> setRoute("Matale Junction", "Anuradhapura New Town"));
        radioButton2.setOnClickListener(v -> setRoute("Saliyapura", "Rambewa"));
        radioButton3.setOnClickListener(v -> setRoute("Kekirawa", "Maradankadawala"));

        // Receive the email from the previous activity (HomeActivity)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
            loadProfileData(email);
        } else {
            showToast("Email not found");
        }

        // Start session when button is clicked
        startButton.setOnClickListener(v -> startSession());
    }

    // Set the source and destination based on RadioButton selection
    private void setRoute(String source, String destination) {
        this.startingPoint = source;
        this.destination = destination;
        showToast("Route set: " + source + " to " + destination);
    }

    // Load user profile data from database
    private void loadProfileData(String email) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                String query = "SELECT FIRST_NAME, LAST_NAME, PROFILE_PICTURE FROM driver WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("FIRST_NAME");
                    String lastName = rs.getString("LAST_NAME");
                    byte[] profilePictureBytes = rs.getBytes("PROFILE_PICTURE");

                    runOnUiThread(() -> {
                        profileName.setText(firstName + "\n" + lastName);
                        if (profilePictureBytes != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(profilePictureBytes, 0, profilePictureBytes.length);
                            profileImage.setImageBitmap(bitmap);
                        }
                    });
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Error: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Exception: " + e.getMessage()));
            }
        });
    }

    // Start session and save data to the database
    private void startSession() {
        if (startingPoint == null || destination == null) {
            showToast("Please select a valid source and destination route.");
            return;
        }

        String sessionId = generateSessionId();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        String sessionStartTime = sdf.format(new Date());

        saveSessionData(email, sessionId, startingPoint, destination, "Vehicle", sessionStartTime);

        // Navigate to the session details activity
        Intent intent = new Intent(DriveActivity2.this, Test_Video4.class);
        intent.putExtra("email", email);
        intent.putExtra("startingPoint", startingPoint);
        intent.putExtra("destination", destination);
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
    }

    // Save session data into the database
    private void saveSessionData(String email, String sessionId, String startingPoint, String destination, String vehicleType, String sessionStartTime) {
        String newendTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis() + 1 * 60 * 1000));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                String query = "INSERT INTO session (EMAIL, SESSION_ID, START_LOCATION, DESTINATION, VEHICLE_TYPE, SESSION_START_TIME, SESSION_END_TIME) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                stmt.setString(2, sessionId);
                stmt.setString(3, startingPoint);
                stmt.setString(4, destination);
                stmt.setString(5, vehicleType);
                stmt.setString(6, sessionStartTime);
                stmt.setString(6, newendTime);

                int rowsInserted = stmt.executeUpdate();


                runOnUiThread(() -> {
                    if (rowsInserted > 0) {
                        showToast("Session started and saved successfully");
                    } else {
                        showToast("Failed to start session");
                    }
                });

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Error saving session: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Exception: " + e.getMessage()));
            }
        });
    }

    // Generate a unique session ID
    private String generateSessionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return sdf.format(new Date());
    }

    // Show Toast message
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
