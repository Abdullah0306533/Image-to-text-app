package com.example.imagetotextapp;

import android.app.Activity;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.imagetotextapp.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Activity_scanner extends AppCompatActivity {

    // UI components
    private Button snapBtn, detectBtn;
    private TextView resultCapture;
    private ImageView captureIV;
    private Bitmap bitmap;
    private Uri photoUri; // To store the URI of the image file

    // Constant for image capture request code
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // Initialize UI components
        snapBtn = findViewById(R.id.idButtonSnap);
        detectBtn = findViewById(R.id.idButtonDetect);
        captureIV = findViewById(R.id.IdIvCapturesImage);
        resultCapture = findViewById(R.id.idTVDetected);

        // Set click listener for the snap button
        snapBtn.setOnClickListener(view -> {
            if (checkPermission()) {
                captureImage();
            } else {
                requestPermission();
            }
        });

        // Set click listener for the detect button
        detectBtn.setOnClickListener(view -> detectText());
    }

    /**
     * Checks if the camera permission has been granted.
     */
    private boolean checkPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.CAMERA);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests camera permission if it has not been granted.
     * Displays the system permission dialog for the user to grant the camera permission.
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    /**
     * Captures an image using the device's camera and stores it in a file.
     */
    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {

                photoFile = createImageFile();
            } catch (IOException ex) {
                resultCapture.setText("Error creating image file.");
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.imagetotextapp.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Creates a temporary file to save the captured image.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                captureIV.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Detects text from the captured image using Google ML Kit.
     */
    private void detectText() {
        if (bitmap != null) {
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            StringBuilder resultText = new StringBuilder();
                            for (Text.TextBlock textBlock : text.getTextBlocks()) {
                                resultText.append(textBlock.getText()).append("\n");
                            }
                            resultCapture.setText(resultText.toString().trim());
                        }
                    })
                    .addOnFailureListener(e -> {
                        resultCapture.setText("Text recognition failed. Please try again.");
                    });
        } else {
            resultCapture.setText("No image captured. Please snap an image first.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                resultCapture.setText("Camera permission is required to use this feature.");
            }
        }
    }
}
