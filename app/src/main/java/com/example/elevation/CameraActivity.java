package com.example.elevation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements Orientation.Listener {
    private Executor executor = Executors.newSingleThreadExecutor();
    private int REQUEST_CAMERA_PERMISSION = 1001;
    private int GREEN_THRESHOLD = 20;
    private int YELLOW_THRESHOLD = 40;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"};
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Orientation mOrientation;
    private PreviewView mPreviewView;
    private ImageView captureImage;
    private TextView gpsPrecision;
    private TextView azimuth;
    private TextView pitch;
    private int azimuteAux;
    private double pitchAux;
    private Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreviewView = findViewById(R.id.camera);
        captureImage = findViewById(R.id.captureImg);
        gpsPrecision = findViewById(R.id.gpsPrecision);
        azimuth = findViewById(R.id.azimuth);
        pitch = findViewById(R.id.pitch);

        getSupportActionBar().hide();

        mOrientation = new Orientation((SensorManager) getSystemService(Activity.SENSOR_SERVICE),
                getWindow().getWindowManager());

        if(allPermissionsGranted()){
            // Start camera if permission has been granted by user
            startCamera();
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CAMERA_PERMISSION);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("Location",location.toString());
                Log.i("GetAccuracy", String.valueOf(location.getAccuracy()));

                currentLocation = location;

                if (location.getAccuracy() <= GREEN_THRESHOLD) {
                    gpsPrecision.setTextColor(Color.GREEN);
                } else if (location.getAccuracy() <= YELLOW_THRESHOLD) {
                    gpsPrecision.setTextColor(Color.YELLOW);
                } else {
                    gpsPrecision.setTextColor(Color.RED);
                }
                gpsPrecision.setText(String.valueOf((int)currentLocation.getAccuracy() + " m"));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.i("onStatusChanged", s);
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i("onProviderEnabled", s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.i("onProviderDisabled", s);
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (currentLocation != null) {
                if (currentLocation.getAccuracy() <= 15) {
                    gpsPrecision.setTextColor(Color.GREEN);
                } else if (currentLocation.getAccuracy() <= 30) {
                    gpsPrecision.setTextColor(Color.YELLOW);
                } else {
                    gpsPrecision.setTextColor(Color.RED);
                }
                gpsPrecision.setText(String.valueOf((int)currentLocation.getAccuracy() + " m"));
                Log.i("Location",currentLocation.toString());
                Log.i("GetAccuracy", String.valueOf(currentLocation.getAccuracy()));
            }
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);

        ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CameraControl cameraControl = camera.getCameraControl();
                CameraInfo cameraInfo = camera.getCameraInfo();

                // Get the camera's current zoom ratio
                float zoomRatio = cameraInfo.getZoomState().getValue().getZoomRatio();

                // Get the pinch gesture's scaling factor
                float delta = detector.getScaleFactor();

                // Update the camera's zoom ratio. This is an asynchronous operation that returns
                // a ListenableFuture, allowing you to listen to when the operation completes.
                cameraControl.setZoomRatio(zoomRatio * delta);

                // Return true, as the event was handled
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }
        };

        ScaleGestureDetector detector = new ScaleGestureDetector(CameraActivity.this, onScaleGestureListener);

        // Attach the pinch gesture listener to the viewfinder
        mPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });

        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = getApplication().getString(R.string.gps_accuracy) + " " + String.valueOf((int) currentLocation.getAccuracy()) + "\n"
                        + getApplication().getString(R.string.azimuth) + " " + String.valueOf(azimuteAux) + "º" + "\n"
                        + getApplication().getString(R.string.pitch) + " " + String.format("%.1f", pitchAux) + "º";

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(CameraActivity.this);
                alertDialog.setTitle(getApplication().getString(R.string.add_estimation_dialog));
                alertDialog.setMessage(message);
                alertDialog.setPositiveButton(getApplication().getString(R.string.yes_dialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("azimuteAux", azimuteAux);
                        resultIntent.putExtra("pitchAux", pitchAux);
                        resultIntent.putExtra("latitude", currentLocation.getLatitude());
                        resultIntent.putExtra("longitude", currentLocation.getLongitude());
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }
                });
                alertDialog.setNeutralButton(getApplication().getString(R.string.cancel_dialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("PERMISSION", "request code: " + requestCode);

        if (grantResults.length > 0 ) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, getApplication().getString(R.string.camera_permission), Toast.LENGTH_SHORT).show();
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (currentLocation != null) {
                    if (currentLocation.getAccuracy() <= 15) {
                        gpsPrecision.setTextColor(Color.GREEN);
                    } else if (currentLocation.getAccuracy() <= 30) {
                        gpsPrecision.setTextColor(Color.YELLOW);
                    } else {
                        gpsPrecision.setTextColor(Color.RED);
                    }
                    gpsPrecision.setText(String.valueOf((int) currentLocation.getAccuracy() + " m"));

                    Log.i("Location", currentLocation.toString());
                    Log.i("GetAccuracy", String.valueOf(currentLocation.getAccuracy()));

                }
            } else {
                Toast.makeText(this, getApplication().getString(R.string.location_permission), Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onOrientationChanged(float pitch, float roll, float azimuth) {
        this.pitch.setText(String.valueOf(String.format("%.1f", pitch)) + " º");
        this.azimuth.setText(String.valueOf((int) azimuth) + " º");

        pitchAux = pitch;
        azimuteAux = (int) azimuth;
    }

    @Override
    public void onAzimuthChanged(float pitch, float roll, int azimuth) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientation.startListening(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientation.stopListening();
    }
}