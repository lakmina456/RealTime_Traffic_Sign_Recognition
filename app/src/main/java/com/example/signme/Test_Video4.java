package com.example.signme;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;
//import com.example.signme.sqlCommand;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test_Video4 extends AppCompatActivity {

    private static final String TAG = "Test_Video4";
    private TrafficSignAlertSystem alertSystem;
    private String firstSignDetected = null;
    private String secondSignDetected = null;
    private String previousSign = "";
    private VideoCapture videoCapture;
    private boolean isPlaying = false;
    private Handler handler;
    private Runnable videoRunnable;
    private ImageView imageView, signView,image_view1,image_view2;
    private TextView labelView;
    private final double alpha = 1.20; // Fixed contrast
    private final double beta = 20.0;  // Fixed brightness
    private Map<Integer, JSONArray> frameDetections;
    private ConnectionClass connectionClass;
    private ExecutorService executorService;
    private Connection con;
    private TextToSpeech textToSpeech; // TextToSpeech instance
    private TextView dateTimeView;
    private Handler handler2 = new Handler();
    private ImageButton muteUnmuteButton;
    private boolean isMuted = false; // Track mute state
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        connectionClass = new ConnectionClass();

        alertSystem = new TrafficSignAlertSystem(this);

        imageView = findViewById(R.id.ImageView1);
        signView = findViewById(R.id.sign_view);
        labelView = findViewById(R.id.label_view);

        dateTimeView = findViewById(R.id.date_time_view);
        muteUnmuteButton = findViewById(R.id.mute_unmute1);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        image_view1 = findViewById(R.id.image_view1);
        image_view2 = findViewById(R.id.image_view2);


        muteUnmuteButton.setOnClickListener(v -> toggleMute());

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TextToSpeech: Language not supported");
                } else {
                    Log.d(TAG, "TextToSpeech initialized successfully");
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
            }
        });


        Intent intent = getIntent();
        String email = intent.getStringExtra("email");
        String source = intent.getStringExtra("startingPoint");
        String destination = intent.getStringExtra("destination");
        String session_id = intent.getStringExtra("session_id");

        playVideo(source, destination);
        updateDateTime();
        String videoResId = getVideoResource(source, destination);
        int getVideoframeCount = getVideoframeCount(videoResId);

        final int rowcount = getRowCount("enhanced_frame");
        final int rowcount1 = getRowCount("captured_frame");
        final int rowcount2 = getRowCount("detected_sign");
        final int rowcount3 = getRowCount("record_video");
        final int rowcount4 = getRowCount("segmented_frame");




        //generateAndInsertSQL(email,videoResId,getVideoframeCount,rowcount);
        int JsonPath1 = getJsonPath(source, destination);
        String JsonResId = getJsonResource(source, destination);
        String jsonPath = copyResourceToInternalStorage(JsonPath1, JsonResId);


        if (videoResId.equals("v1")){
            if (checkVideoIdExists("record_video",videoResId) == 0) {

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {

                    try {
                        generateAndInsertSQL1(email,videoResId,getVideoframeCount(getVideoResource(source, destination)),rowcount3);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showToast("Error: " + e.getMessage()));
                    }
                });

                executorService.shutdown();
            }
            updateTrafficSignData(jsonPath,"record_video",email,videoResId,session_id);
        } else if (videoResId.equals("v2")) {
            if (checkVideoIdExists("record_video",videoResId) == 0) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {

                    try {
                        generateAndInsertSQL1(email,videoResId,getVideoframeCount(getVideoResource(source, destination)),rowcount3);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showToast("Error: " + e.getMessage()));
                    }
                });

                executorService.shutdown();
            }
            updateTrafficSignData(jsonPath,"record_video",email,videoResId,session_id);
        }else if (videoResId.equals("v3")) {
            if (checkVideoIdExists("record_video",videoResId) == 0) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {

                    try {
                        generateAndInsertSQL1(email,videoResId,getVideoframeCount(getVideoResource(source, destination)),rowcount3);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showToast("Error: " + e.getMessage()));
                    }
                });

                executorService.shutdown();
            }
            updateTrafficSignData(jsonPath,"record_video",email,videoResId,session_id);
        }

    }

    private void playVideo(String source, String destination) {

        int videoPath1 = getVideoPath(source, destination);
        int JsonPath1 = getJsonPath(source, destination);
        String videoResId = getVideoResource(source, destination);
        String JsonResId = getJsonResource(source, destination);
        if (videoPath1 != 0) {

            // Initialize OpenCV
            if (!OpenCVLoader.initDebug()) {
                Log.e(TAG, "OpenCV initialization failed");
            } else {
                Log.d(TAG, "OpenCV initialized successfully");
            }

            String videoPath = copyResourceToInternalStorage(videoPath1, videoResId);
            String jsonPath = copyResourceToInternalStorage(JsonPath1, JsonResId);

            parseDetectionJSON(jsonPath);

            videoCapture = new VideoCapture();
            if (!videoCapture.open(videoPath)) {
                Log.e(TAG, "Cannot open video: " + videoPath);
                return;
            }

            handler = new Handler();

            videoRunnable = new Runnable() {
                int frameCount = 0;

                @Override
                public void run() {
                    Mat frame = new Mat();
                    if (videoCapture.read(frame)) {
                        frameCount++;

                        Mat adjustedFrame = new Mat();
                        frame.convertTo(adjustedFrame, -1, alpha, beta);

                        // Annotate frame and update views
                        annotateFrame(adjustedFrame, frameCount);

                        Bitmap bitmap = Bitmap.createBitmap(adjustedFrame.cols(), adjustedFrame.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(adjustedFrame, bitmap);

                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));

                        handler.postDelayed(this, 33); // ~30fps
                    } else {
                        isPlaying = false;
                    }
                }
            };

            isPlaying = true;
            handler.post(videoRunnable);

        } else {
            // Handle unmatched routes (optional)
            System.out.println("No video available for the selected route.");
        }
        ConstraintLayout mainLayout = findViewById(R.id.root_layout);
        // Fade animation for layout transparency
        ObjectAnimator fadeLayout = ObjectAnimator.ofFloat(mainLayout, "alpha", 1f, 0.1f);
        fadeLayout.setDuration(3000);

        // Combine animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeLayout);
        animatorSet.start();

    }
    private void toggleMute() {
        if (isMuted) {
            // Unmute
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            isMuted = false;
        } else {
            // Mute
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            isMuted = true;
        }
        updateButtonImage();
    }
    private void updateButtonImage() {
        if (isMuted) {
            muteUnmuteButton.setImageResource(R.drawable.volume_off); // Replace with your mute icon
        } else {
            muteUnmuteButton.setImageResource(R.drawable.volume); // Replace with your unmute icon
        }
    }
    private void updateDateTime() {
        // Create a Runnable to update the TextView every second
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Get the current date and time
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault());
                String currentDateTime = dateFormat.format(calendar.getTime());

                // Update the TextView
                dateTimeView.setText(currentDateTime);

                // Schedule the next update after 1 second
                handler2.postDelayed(this, 1000);
            }
        };

        // Start the first update
        handler2.post(runnable);
    }
    private int getVideoPath(String source, String destination) {
        // Define route-to-video mapping
        if (source.equals("Matale Junction") && destination.equals("Anuradhapura New Town")) {
            return R.raw.v1;
        } else if (source.equals("Saliyapura") && destination.equals("Rambewa")) {
            return R.raw.v2;
        } else if (source.equals("Kekirawa") && destination.equals("Maradankadawala")) {
            return R.raw.v3;
        }
        return 0; // No match found
    }
    private int getJsonPath(String source, String destination) {
        // Define route-to-video mapping
        if (source.equals("Matale Junction") && destination.equals("Anuradhapura New Town")) {
            return R.raw.detection_results1;
        } else if (source.equals("Saliyapura") && destination.equals("Rambewa")) {
            return R.raw.detection_results2;
        } else if (source.equals("Kekirawa") && destination.equals("Maradankadawala")) {
            return R.raw.detection_results3;
        }
        return 0; // No match found
    }
    private String getJsonPath2(String source, String destination) {
        // Define route-to-video mapping
        if (source.equals("Matale Junction") && destination.equals("Anuradhapura New Town")) {
            return "R.raw.detection_results1";
        } else if (source.equals("Saliyapura") && destination.equals("Rambewa")) {
            return "R.raw.detection_results2";
        } else if (source.equals("Kekirawa") && destination.equals("Maradankadawala")) {
            return "R.raw.detection_results3";
        }
        return null; // No match found
    }

    private String getVideoResource(String source, String destination) {
        if (source.equals("Matale Junction") && destination.equals("Anuradhapura New Town")) {
            return "v1";
        } else if (source.equals("Saliyapura") && destination.equals("Rambewa")) {
            return "v2";
        } else if (source.equals("Kekirawa") && destination.equals("Maradankadawala")) {
            return "v3";
        }
        return "UNKNOWN_VIDEO";
    }
    private String getJsonResource(String source, String destination) {
        if (source.equals("Matale Junction") && destination.equals("Anuradhapura New Town")) {
            return "detection_results1";
        } else if (source.equals("Saliyapura") && destination.equals("Rambewa")) {
            return "detection_results2";
        } else if (source.equals("Kekirawa") && destination.equals("Maradankadawala")) {
            return "detection_results3";
        }
        return "UNKNOWN_VIDEO";
    }

    private int getVideoframeCount(String getVideoResource) {
        if (getVideoResource == "v1") {
            return 690;
        } else if(getVideoResource == "v2") {
            return 510;
        }else if(getVideoResource == "v3")
            return 994;
        return 0;
    }

    private void annotateFrame(Mat frame, int frameNumber) {
        JSONArray detections = frameDetections.get(frameNumber);
        if (detections == null) return;

        for (int i = 0; i < detections.length(); i++) {
            try {
                JSONObject detection = detections.getJSONObject(i);
                JSONArray bboxArray = detection.getJSONArray("bbox");
                String className = detection.getString("class_name");

                int xMin = bboxArray.getInt(0);
                int yMin = bboxArray.getInt(1);
                int xMax = bboxArray.getInt(2);
                int yMax = bboxArray.getInt(3);

                Rect rect = new Rect(xMin, yMin, xMax - xMin, yMax - yMin);
                org.opencv.imgproc.Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);

                updatePreviousAndCurrentSign1(className);
                // Update sign image and label
                updateSignAndLabel(className);
            } catch (Exception e) {
                Log.e(TAG, "Error annotating frame: " + e.getMessage());
            }
        }
    }
    private void updateSignAndLabel(String className) {
        runOnUiThread(() -> {
            // Convert the detected sign name to a drawable resource name

            String drawableName = formatToDrawableName(className);
            labelView.setText(className);
            // Load sign image dynamically
            int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());

            if (resId != 0) {
                signView.setImageResource(resId);

            } else {
                //labelView.setText("");
            }

            // Speak the detected sign name
            //speakSignName(className);

            // Update label text
            //labelView.setText(className);
        });
    }
    private void updatePreviousAndCurrentSign1(String className) {
        if (firstSignDetected == null) {
            firstSignDetected = className;
            alertSystem.speak(className);
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(className), "drawable", getPackageName()));
        } else if (!className.equals(firstSignDetected) && secondSignDetected == null) {
            secondSignDetected = className;
            alertSystem.speak(className);
            alertSystem.alertBasedOnContext(firstSignDetected, secondSignDetected);
            image_view2.setImageResource(getResources().getIdentifier(formatToDrawableName(firstSignDetected), "drawable", getPackageName()));
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(secondSignDetected), "drawable", getPackageName()));
        } else if (!className.equals(firstSignDetected) && !className.equals(secondSignDetected)) {
            previousSign = firstSignDetected;
            firstSignDetected = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
            previousSign = secondSignDetected;
            secondSignDetected = className;
            alertSystem.alertBasedOnContext(previousSign, secondSignDetected);
            image_view2.setImageResource(getResources().getIdentifier(formatToDrawableName(firstSignDetected), "drawable", getPackageName()));
            image_view1.setImageResource(getResources().getIdentifier(formatToDrawableName(secondSignDetected), "drawable", getPackageName()));
        }
    }

    private String formatToDrawableName(String className) {
        // Convert to lowercase and replace spaces with underscores
        String formattedName = className.toLowerCase().replace(" ", "_");

        // Replace slashes with an empty string (to remove /) and remove all non-alphanumeric characters except underscore and digits
        formattedName = formattedName.replace("/", "").replaceAll("[^a-z0-9_]", "");

        // Specifically handle "km/h" to become "kmh"
        formattedName = formattedName.replace("km_h", "kmh");

        return formattedName;
    }


    private String copyResourceToInternalStorage(int resourceId, String outputFileName) {
        try {
            Resources resources = getResources();
            InputStream inputStream = resources.openRawResource(resourceId);
            File outputFile = new File(getFilesDir(), outputFileName);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Resource copied to: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error copying resource: " + e.getMessage());
            return null;
        }
    }

    private void parseDetectionJSON(String jsonPath) {
        frameDetections = new HashMap<>();
        try {
            File jsonFile = new File(jsonPath);
            InputStream inputStream = new java.io.FileInputStream(jsonFile);
            byte[] buffer = new byte[(int) jsonFile.length()];
            inputStream.read(buffer);
            inputStream.close();

            String jsonString = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject frameData = jsonArray.getJSONObject(i);
                int frameNumber = frameData.getInt("frame");
                JSONArray detections = frameData.getJSONArray("detections");
                frameDetections.put(frameNumber, detections);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoCapture != null) {
            videoCapture.release();
        }
        if (handler != null) {
            handler.removeCallbacks(videoRunnable);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        handler2.removeCallbacksAndMessages(null);
    }

    private int getRowCount(String table_name) {
        // Initialize row count to -1 to indicate an error if not updated
        final int[] rowCount = {-1};

        // Create an executor service to handle database operations on a background thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Connection con = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                // Establish a connection
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                // Create and execute the SQL query to count rows
                String query = "SELECT COUNT(*) AS row_count FROM " + table_name;
                stmt = con.createStatement();
                rs = stmt.executeQuery(query);

                // Retrieve the row count from the result set
                if (rs.next()) {
                    rowCount[0] = rs.getInt("row_count");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Database error: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Error: " + e.getMessage()));
            } finally {
                // Close resources
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.shutdown();
        try {
            // Wait for the background task to complete
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return rowCount[0];
    }


    private String getCoordinate(String table_name) {


        return table_name;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void generateAndInsertSQL(String email,String videoResId,int getVideoframeCount,int rowcount) {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }
                Python py = Python.getInstance();

                // Load the Python module
                PyObject pyModule = py.getModule("sql"); // Name of your Python file without '.py'

                // Call the function and get SQL statements
                PyObject result = pyModule.callAttr(
                        "qsql",
                        email, // email_id
                        videoResId,                // video_id
                        "data/traffic/enhanced_frames/"+videoResId+"/", // base_url
                        getVideoframeCount,                // frame_count
                        rowcount                 // current_row
                );

                PyObject result1 = pyModule.callAttr(
                        "captured_frame",
                        email, // email_id
                        videoResId,                // video_id
                        "data/traffic/captured_frame/"+videoResId+"/", // base_url
                        getVideoframeCount,                // frame_count
                        rowcount                 // current_row
                );

                PyObject result2 = pyModule.callAttr(
                        "record_video",
                        email,
                        videoResId,
                        "data/traffic/captured_frame/"+videoResId+"/",
                        "data/traffic/enhanced_frames/"+videoResId+"/",
                        getVideoframeCount,
                        rowcount
                );


                // Process the list of SQL commands
                List<PyObject> sqlCommands = result.asList();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        con = connectionClass.CONN();

                        for (PyObject sqlCommand : sqlCommands) {
                            try (PreparedStatement stmt = con.prepareStatement(sqlCommand.toString())) {
                                stmt.executeUpdate();
                            }
                        }
                        con.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace(); // Log the exception for debugging
            }
            // Close the database connection
        });
    }

    public void generateAndInsertSQL1(String email, String videoResId, int getVideoframeCount, int rowcount) {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }
                Python py = Python.getInstance();

                // Load the Python module
                PyObject pyModule = py.getModule("sql");

                PyObject result1 = pyModule.callAttr(
                        "record_video",
                        email,
                        videoResId,
                        "data/traffic/captured_frame/"+videoResId+"/",
                        "data/traffic/enhanced_frames/"+videoResId+"/",
                        getVideoframeCount,
                        rowcount
                );



                // Process the list of SQL commands
                List<PyObject> sqlCommands1 = result1.asList();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        con = connectionClass.CONN();

                        for (PyObject sqlCommand : sqlCommands1) {
                            try (PreparedStatement stmt = con.prepareStatement(sqlCommand.toString())) {
                                stmt.executeUpdate();
                            }
                        }
                        con.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace(); // Log the exception for debugging
            }
            // Close the database connection
        });
    }

    public void updateTrafficSignData(String jsonFilePath, String tableName, String email, String videoId, String session_id) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                // Establish database connection
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                // Start Python if not already started
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }

                Python py = Python.getInstance();
                PyObject pyModule = py.getModule("sql2"); // Load the Python module

                // Read the JSON file content as a string
                String jsonContent = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)), StandardCharsets.UTF_8);
                }

                // Call the Python function to generate SQL update commands
                PyObject result = pyModule.callAttr("generate_update_sql", jsonContent, tableName, email, videoId, session_id);
                List<PyObject> sqlCommands = result.asList();

                // Execute SQL commands
                for (PyObject sqlCommand : sqlCommands) {
                    try (PreparedStatement stmt = con.prepareStatement(sqlCommand.toString())) {
                        stmt.executeUpdate();
                    }
                }

                con.close();
            } catch (Exception e) {
                e.printStackTrace(); // Log the exception for debugging
            }
        });
    }
    private int checkVideoIdExists(String table_name, String videoId) {
        // Initialize result to 0 (not found)
        final int[] result = {0};

        // Create an executor service to handle database operations on a background thread
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Connection con = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                // Establish a connection
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Error in connection with server"));
                    return;
                }

                // Create and execute the SQL query to check for the VIDEO_ID
                String query = "SELECT 1 FROM " + table_name + " WHERE VIDEO_ID = '" + videoId + "' LIMIT 1";
                stmt = con.createStatement();
                rs = stmt.executeQuery(query);

                // If a row is found, set result to 1
                if (rs.next()) {
                    result[0] = 1;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Database error: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Error: " + e.getMessage()));
            } finally {
                // Close resources
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.shutdown();
        try {
            // Wait for the background task to complete
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return result[0];
    }

}
