package com.example.mondrian2;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import joinery.DataFrame;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////// Setting up READ BUTTON listener ////////////////////////////////////////////////////
        Button readFileButton = findViewById(R.id.readFileButton);
        readFileButton.setOnClickListener(v -> readCsvFileFromAssets());

        ////// Setting up ANONYMIZE BUTTON listener //////////////////////////////////////////////
        Button anonymizeButton = findViewById(R.id.anonymizeButton);
        findViewById(R.id.anonymizeButton).setOnClickListener(v -> checkAndRequestPermissions());
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        }, PERMISSION_REQUEST_CODE);
            } else {

//                final List<String> commandlineArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
                executeAnonymization();
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                executeAnonymization();
                Log.d(TAG, "(checkAndRequestPermissions) Permission granted, executing anonymization");
            }
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted, executing anonymization");
                executeAnonymization();
                Toast.makeText(this, "Permission Granted. Performing anonymization.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Permission denied, Cannot perform anonymization");
                Toast.makeText(this, "Permission Denied. Cannot perform anonymization.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    ///////////// READ FILE BUTTON - ACTIVATE ////////////////////////////////////////////////////////////
    private void readCsvFileFromAssets() {
        AssetManager assetManager = getAssets();
        try (InputStream inputStream = assetManager.open("dataset.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 5) {
                // Process each line
                Log.d("CSV_Content", line);
                lineCount++;
            }
            Toast.makeText(this, "CSV file read successfully in the backend", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("CSV_Error", "Error reading CSV file", e);
            Toast.makeText(this, "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void executeAnonymization() {

//        Toast.makeText(this, "executeAnonymization code is initiated", Toast.LENGTH_SHORT).show();

        try {
            // Set default values
            int k = 5;
            String dataFilePath = "./app/src/main/assets/dataset.csv";
            String anonymizedFileDirPath = getExternalFilesDir(null) + "/anonymized/";
            String hierarchyFileDirPath = getExternalFilesDir(null) + "/hierarchy/";

            // Ensure the anonymized file directory exists
            File anonymizedDir = new File(anonymizedFileDirPath);
            if (!anonymizedDir.exists()) {
                anonymizedDir.mkdirs();
            }

            List<String> quasiIdentifiers = Arrays.asList(
                    "sex", "age", "race", "marital-status", "education",
                    "native-country", "workclass", "occupation"
            );

            // Execute Mondrian algorithm
            DataFrame anonymizedDf = Mondrian.runAnonymize(quasiIdentifiers, dataFilePath, hierarchyFileDirPath, k);

            // Save the anonymized DataFrame
            String outputFilePath = anonymizedFileDirPath + "k_" + k + "_anonymized_dataset.csv";
            anonymizedDf.writeCsv(outputFilePath);

            runOnUiThread(() -> Toast.makeText(this, "Anonymization completed. Output saved to: " + outputFilePath, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error during anonymization: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

}