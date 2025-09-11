package com.beaverlisk.docscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class ScannerActivity extends AppCompatActivity {

    private final String TAG = "ScannerActivity";
    private final int PERMISSION_REQUEST_CODE = 1029;
    private final long FINISH_DELAY_MILLIS = 600;
    private OpenNoteCameraView openNoteCameraView;
    private FrameLayout cameraViewLayout;
    private boolean convertToBase64 = false;
    private boolean captureMultiple = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cameraViewLayout = (FrameLayout) lf.inflate(R.layout.activity_open_note_scanner, null);

        openNoteCameraView = new OpenNoteCameraView(this, -1, this, cameraViewLayout);
        openNoteCameraView.setOnProcessingListener((inProcessing) -> Log.w(TAG, "onProcessingChange"));
        openNoteCameraView.setOnScannerListener(new OpenNoteCameraView.OnScannerListener() {
            @Override
            public void onPictureTaken(PhotoInfo photoInfo) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(ScanConstants.KEY_EXTRA_IMAGE_CONTENT_URI, photoInfo.getCroppedImageUri());
                setResult(Activity.RESULT_OK, resultIntent);
                if (!captureMultiple) {
                    openNoteCameraView.invalidate();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), FINISH_DELAY_MILLIS);
                }
            }

            @Override
            public void onError(Throwable error) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(ScanConstants.KEY_EXTRA_ERROR, error);
                setResult(Activity.RESULT_CANCELED, resultIntent);
                openNoteCameraView.invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), FINISH_DELAY_MILLIS);
            }
        });
        if (requestPermissions()) {
            initCameraView();
        }
    }

    private void initCameraView() {
        Button captureButton = cameraViewLayout.findViewById(R.id.capture);
        captureButton.setOnClickListener((e) -> this.openNoteCameraView.capture());
        cameraViewLayout.findViewById(R.id.cancel).setOnClickListener((e) -> this.onBackPressed());

        FrameLayout root = findViewById(R.id.root);
        root.addView(openNoteCameraView, 0, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(cameraViewLayout, 1, openNoteCameraView.getLayoutParams());

        Bundle propertiesBundle = getIntent().getBundleExtra(ScanConstants.KEY_BUNDLE_EXTRA);
        if (propertiesBundle != null) {
            String overlayColor = propertiesBundle.getString(ScanConstants.KEY_BUNDLE_PROP_OVERLAY_COLOR);
            String borderColor = propertiesBundle.getString(ScanConstants.KEY_BUNDLE_PROP_BORDER_COLOR);
            int detectionCount = propertiesBundle.getInt(ScanConstants.KEY_BUNDLE_PROP_DETECTION_COUNT, -1);
            boolean enableTorch = propertiesBundle.getBoolean(ScanConstants.KEY_BUNDLE_PROP_ENABLE_TORCH, false);
            double brightness = propertiesBundle.getDouble(ScanConstants.KEY_BUNDLE_PROP_BRIGHTNESS, -1);
            double contrast = propertiesBundle.getDouble(ScanConstants.KEY_BUNDLE_PROP_CONTRAST, -1);
            convertToBase64 = propertiesBundle.getBoolean(ScanConstants.KEY_BUNDLE_PROP_USE_BASE_64, false);
            captureMultiple = propertiesBundle.getBoolean(ScanConstants.KEY_BUNDLE_PROP_CAPTURE_MULTIPLE, false);

            CaptureMode captureMode = CaptureMode.valueOf(propertiesBundle.getInt(ScanConstants.KEY_BUNDLE_PROP_CAPTURE_MODE, CaptureMode.AUTO.getValue()));
            captureMode = captureMode != null ? captureMode : CaptureMode.AUTO;
            if (captureMode != CaptureMode.AUTO) {
                captureButton.setVisibility(View.VISIBLE);
            }

            this.openNoteCameraView.setCaptureMode(captureMode);
            if (overlayColor != null) openNoteCameraView.setOverlayColor(overlayColor);
            if (borderColor != null) openNoteCameraView.setOverlayBorderColor(borderColor);
            if (detectionCount != -1) openNoteCameraView.setDetectionCountBeforeCapture(detectionCount);
            if (brightness != (double) -1.0F) openNoteCameraView.setBrightness(brightness);
            if (contrast != (double) -1.0F) openNoteCameraView.setContrast(brightness);

            openNoteCameraView.setMultiCapture(captureMultiple);
            openNoteCameraView.setEnableTorch(enableTorch);
        }
        openNoteCameraView.setCameraPermissionGranted();
    }

    private boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            List<String> requiredPermissions = new ArrayList<>();
            requiredPermissions.add(Manifest.permission.CAMERA);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            ActivityCompat.requestPermissions(ScannerActivity.this, requiredPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean readExternalStorage = Build.VERSION.SDK_INT > Build.VERSION_CODES.P || grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission && readExternalStorage) {
                initCameraView();
            } else {
                setResult(Activity.RESULT_CANCELED);
                openNoteCameraView.invalidate();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (openNoteCameraView != null) {
            openNoteCameraView.removeOnProcessingListener();
            openNoteCameraView.removeOnScannerListener();
            openNoteCameraView = null;
        }
        super.onDestroy();
    }
}