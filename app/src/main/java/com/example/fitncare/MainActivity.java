package com.example.fitncare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int SENSOR_PERMISSION_REQUEST_CODE = 1;
    private ProgressBar progressBar;
    private TextView stepsTextView, distanceTextView, goalTextView, remainingStepsTextView;
    private Button setGoalButton;
    private int stepsCount = 0;
    private int goalSteps = 5000;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        stepsTextView = findViewById(R.id.stepsTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        goalTextView = findViewById(R.id.goalTextView);
        remainingStepsTextView = findViewById(R.id.remainingStepsTextView);
        setGoalButton = findViewById(R.id.setGoalButton);

        // Check and request sensor permission
        if (checkSensorPermission()) {
            initializeSensor();
        } else {
            requestSensorPermission();
        }

        setGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSetGoalDialog();
            }
        });

        // Initialize the UI
        updateUI();
    }

    private boolean checkSensorPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestSensorPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    SENSOR_PERMISSION_REQUEST_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    SENSOR_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor == null) {
            // Handle the case where the step counter sensor is not available
            stepsTextView.setText("Step counter sensor not available on this device.");
            setGoalButton.setEnabled(false);
        } else {
            // Register the listener if the sensor is available
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stepCounterSensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepsCount += (int) event.values[0];
            updateUI();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    private void showSetGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Goal");
        builder.setMessage("Enter your new step goal:");

        // Set up the input
        final EditText input = new EditText(this);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the entered goal from the dialog
                String enteredGoal = input.getText().toString();

                // Update the goalTextView with the entered goal
                updateGoal(enteredGoal);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateGoal(String newGoal) {
        // Update the goalTextView with the new goal
        try {
            goalSteps = Integer.parseInt(newGoal);
            updateUI();
        } catch (NumberFormatException e) {
            // Handle the case where the entered value is not a valid number
            e.printStackTrace();
        }
    }

    private void updateRemainingSteps() {
        int remainingSteps = goalSteps - stepsCount;
        if (remainingSteps > 0) {
            remainingStepsTextView.setText("You have " + remainingSteps + " steps remaining!");
        } else {
            remainingStepsTextView.setText("Congratulations! Goal achieved!");
        }
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(stepsCount * 100 / goalSteps);
                stepsTextView.setText(String.valueOf(stepsCount));
                double distance = stepsCount * 0.00076;  // Update stride length with user input
                distanceTextView.setText(String.format("%.2f km", distance));
                goalTextView.setText(String.format("Goal: %d steps", goalSteps));
                updateRemainingSteps();
            }
        });
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SENSOR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSensor();
            } else {
                // Handle the case where the user denies permission
                stepsTextView.setText("Permission denied. The app may not work properly.");
                setGoalButton.setEnabled(true);
            }
        }
    }
}
