package com.example.signme;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionActivity extends AppCompatActivity {

    private static final String TAG = "SessionActivity";

    // Database related
    private ConnectionClass connectionClass;
    private Connection con;
    private String str;

    // UI Elements
    private ImageView imageView, imageView1, signView;
    private TextView labelView, dateTimeView;
    private ImageButton muteUnmuteButton;
    private Button endSessionButton;

    // Media related
    private MediaPlayer mediaPlayer;
    private boolean isMuted = false;
    private AudioManager audioManager;
    private List<String> audioUrls;

    // Session data
    private String email, startingPoint, destination;
    private String videoId;

    // Handlers and timers
    private Handler handler = new Handler();
    private Timer timer = new Timer();
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        try {
            initializeViews();
            initializeSession();
            setupMediaPlayer();
            setupButtonListeners();
            startSession();
        } catch (Exception e) {
            Log.e(TAG, "Error during activity creation", e);
            showError("Failed to initialize session", e);
        }
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imgGlide);
        imageView1 = findViewById(R.id.imgGlide1);
        labelView = findViewById(R.id.label_view);
        signView = findViewById(R.id.sign_view);
        dateTimeView = findViewById(R.id.date_time_view);
        muteUnmuteButton = findViewById(R.id.mute_unmute);
        endSessionButton = findViewById(R.id.end_session_button);
    }

    private void initializeSession() {
        connectionClass = new ConnectionClass();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioUrls = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();

        // Get intent extras
        Intent intent = getIntent();
        if (intent != null) {
            email = intent.getStringExtra("email");
            startingPoint = intent.getStringExtra("startingPoint");
            destination = intent.getStringExtra("destination");
            Log.d(TAG, "Session initialized for route: " + startingPoint + " to " + destination);
        } else {
            Log.e(TAG, "No intent extras found");
            showError("Session initialization failed", null);
        }

        videoId = determineVideoId(startingPoint, destination);
    }

    private String determineVideoId(String startingPoint, String destination) {
        Log.d(TAG, "Determining videoId for route: " + startingPoint + " to " + destination);

        String routeKey = startingPoint + "_to_" + destination;
        String videoId;

        switch (routeKey) {
            case "Mihinthale_to_Anuradhapura":
                videoId = "v1";
                break;
            case "Saliyapura_to_Rambewa":
                videoId = "v2";
                break;
            case "Kekirawa_to_Maradankadawala":
                videoId = "v3";
                break;
            default:
                videoId = null;
                Log.w(TAG, "No matching video ID found for route: " + routeKey);
                return null;
        }

        Log.d(TAG, "Assigned videoId: " + videoId + " for route: " + routeKey);
        return videoId;
    }

    private void setupMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            showError("Media playback error", null);
            return false;
        });
    }

    private void setupButtonListeners() {
        muteUnmuteButton.setOnClickListener(v -> toggleMute());
        endSessionButton.setOnClickListener(v -> showEndSessionDialog());
    }

    private void startSession() {
        if (videoId == null) {
            showError("No video available for this route", null);
            return;
        }

        connect();
        startPeriodicTasks();
        loadMediaContent();
    }

    private void connect() {
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    str = "Error in connection with MySQL Server";
                    Log.e(TAG, str);
                } else {
                    str = "Connected with MySQL server";
                    Log.d(TAG, str);
                }
            } catch (Exception e) {
                str = "Exception in connection";
                Log.e(TAG, str, e);
            }
            runOnUiThread(() -> Toast.makeText(SessionActivity.this, str, Toast.LENGTH_SHORT).show());
        });
    }

    private void startPeriodicTasks() {
        // Update date/time every second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void loadMediaContent() {
        try {
            imgGlide();
            updateLabelAndSign();
            fetchAudioUrls();
        } catch (Exception e) {
            Log.e(TAG, "Error loading media content", e);
            showError("Failed to load media content", e);
        }
    }

    public void imgGlide() {

        executorService.execute(() -> {
            List<String> urlList = new ArrayList<>();
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    Log.e(TAG, "Database connection failed in imgGlide");
                    showError("Database connection failed", null);
                    return;
                }

                String query = "SELECT CAPTURED_FRAME FROM record_video WHERE VIDEO_ID = ?";
                Log.d(TAG, "Executing query for videoId: " + videoId);

                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setString(1, videoId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String url = rs.getString("CAPTURED_FRAME").trim();
                            urlList.add(url);
                            Log.d(TAG, "Added URL: " + url);
                        }
                    }
                }

                Log.d(TAG, "Retrieved " + urlList.size() + " URLs for videoId: " + videoId);
            } catch (SQLException e) {
                Log.e(TAG, "SQL Exception in imgGlide", e);
                showError("Database error while loading images", e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing connection", e);
                }
            }

            runOnUiThread(() -> {
                if (!urlList.isEmpty()) {
                    displayImages(urlList);
                } else {
                    showError("No images found for this route", null);
                }
            });
        });
    }

    private void displayImages(List<String> urlList) {
        final Handler handler = new Handler();
        final int[] index = {0};
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!urlList.isEmpty() && !isFinishing()) {
                    String url = urlList.get(index[0]);
                    try {
                        if (url != null && (url.startsWith("http") || url.startsWith("https"))) {
                            if (index[0] % 2 == 0) {
                                imageView.startAnimation(fadeIn);
                                Glide.with(SessionActivity.this).load(url).into(imageView);
                            } else {
                                imageView1.startAnimation(fadeIn);
                                Glide.with(SessionActivity.this).load(url).into(imageView1);
                            }
                        } else {
                            Log.e(TAG, "Invalid URL: " + url);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image: " + url, e);
                    }

                    index[0] = (index[0] + 1) % urlList.size();
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.post(runnable);
    }

    private void updateLabelAndSign() {
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    showError("Database connection failed", null);
                    return;
                }

                String query = "SELECT SIGN_NAME, SEGMENTED_SIGN FROM record_video WHERE VIDEO_ID = ?";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setString(1, videoId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> labels = new ArrayList<>();
                        List<String> signUrls = new ArrayList<>();

                        while (rs.next()) {
                            labels.add(rs.getString("SIGN_NAME"));
                            signUrls.add(rs.getString("SEGMENTED_SIGN"));
                        }

                        runOnUiThread(() -> {
                            if (!labels.isEmpty() && !signUrls.isEmpty()) {
                                updateLabelView(labels);
                                updateSignView(signUrls);
                            } else {
                                showError("No sign data found for this route", null);
                            }
                        });
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error updating labels and signs", e);
                showError("Failed to load sign information", e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing connection", e);
                }
            }
        });
    }

    private void updateLabelView(List<String> labels) {
        if (labels.isEmpty()) return;

        final Handler handler = new Handler();
        final int[] index = {0};

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    labelView.setText(labels.get(index[0]));
                    index[0] = (index[0] + 1) % labels.size();
                    handler.postDelayed(this, 2000);
                }
            }
        });
    }

    private void updateSignView(List<String> signUrls) {
        if (signUrls.isEmpty()) return;

        final Handler handler = new Handler();
        final int[] index = {0};
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && signUrls.get(index[0]) != null) {
                    signView.startAnimation(fadeIn);
                    Glide.with(SessionActivity.this)
                            .load(signUrls.get(index[0]))
                            .into(signView);
                    index[0] = (index[0] + 1) % signUrls.size();
                    handler.postDelayed(this, 2000);
                }
            }
        });
    }

    public void fetchAudioUrls() {
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    showError("Database connection failed", null);
                    return;
                }

                String query = "SELECT AUDIO_ALERT FROM record_video WHERE VIDEO_ID = ?";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setString(1, videoId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        audioUrls.clear();
                        while (rs.next()) {
                            audioUrls.add(rs.getString("AUDIO_ALERT"));
                        }
                    }
                }

                if (!audioUrls.isEmpty()) {
                    runOnUiThread(this::playAudioSequentially);
                } else {
                    Log.w(TAG, "No audio URLs found for videoId: " + videoId);
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error fetching audio URLs", e);
                showError("Failed to load audio content", e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing connection", e);
                }
            }
        });
    }

    private void playAudioSequentially(int index) {
        if (index >= audioUrls.size()) {
            Log.d(TAG, "Finished playing all audio files");
            return;
        }
        String audioUrl = audioUrls.get(index);
        if (audioUrl == null || audioUrl.isEmpty()) {
            handler.postDelayed(() -> playAudioSequentially(index + 1), 1500);
            return;
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
            mediaPlayer.setOnCompletionListener(mp -> playAudioSequentially(index + 1));
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                playAudioSequentially(index + 1);
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error playing audio: " + audioUrl, e);
            playAudioSequentially(index + 1);
        }
    }

    private void playAudioSequentially() {
        playAudioSequentially(0);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMuted);
        muteUnmuteButton.setImageResource(isMuted ? R.drawable.volume_off : R.drawable.volume);
        Toast.makeText(SessionActivity.this,
                isMuted ? "Sound is muted" : "Sound is unmuted",
                Toast.LENGTH_SHORT).show();
    }

    private void updateDateTime() {
        if (!isFinishing()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());
            dateTimeView.setText(currentDateAndTime);
        }
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end the session?")
                .setPositiveButton("OK", (dialog, which) -> endSession())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        try {
            String sessionEndTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",
                    Locale.getDefault()).format(new Date());
            saveSessionEndTime(email, sessionEndTime);
        } catch (Exception e) {
            Log.e(TAG, "Error ending session", e);
            showError("Failed to end session", e);
        }
    }

    private void saveSessionEndTime(String email, String sessionEndTime) {
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    showError("Database connection failed", null);
                    return;
                }

                String query = "UPDATE session SET SESSION_END_TIME = ? WHERE EMAIL = ? AND SESSION_END_TIME IS NULL";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setString(1, sessionEndTime);
                    stmt.setString(2, email);

                    int rowsUpdated = stmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        Log.d(TAG, "Session end time saved successfully");
                        navigateToDriveActivity();
                    } else {
                        showError("No active session found to end", null);
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error saving session end time", e);
                showError("Failed to save session end time", e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (SQLException e) {
                    Log.e(TAG, "Error closing connection", e);
                }
            }
        });
    }

    private void navigateToDriveActivity() {
        runOnUiThread(() -> {
            Intent intent = new Intent(SessionActivity.this, DriveActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        });
    }

    private void showError(String message, Exception e) {
        String errorMessage = message;
        if (e != null) {
            errorMessage += ": " + e.getMessage();
        }
        String finalErrorMessage = errorMessage;

        runOnUiThread(() -> {
            Log.e(TAG, finalErrorMessage);
            Toast.makeText(SessionActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void cleanup() {
        // Cancel all running handlers and timers
        handler.removeCallbacksAndMessages(null);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Release MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
        }

        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Close database connection if open
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error closing database connection", e);
        }
    }
}