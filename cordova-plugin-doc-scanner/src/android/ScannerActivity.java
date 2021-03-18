package com.exadel.docscanplugin;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ScannerActivity extends AppCompatActivity {

    private final int PERMISSION_REQUEST_CODE = 1029;
    private OpenNoteCameraView openNoteCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        FrameLayout frameLayout = (FrameLayout) lf.inflate(
                getResources().getIdentifier("activity_open_note_scanner", "layout", getPackageName()), null);
        openNoteCameraView = new OpenNoteCameraView(this, -1, this, frameLayout);
        if (requestPermissions()) {
            openNoteCameraView.setCameraPermissionGranted();
        }
        openNoteCameraView.setOnProcessingListener((inProcessing) -> Log.w("WUF", "onProcessingChange"));
        openNoteCameraView.setOnScannerListener(photoInfo -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(ScanConstants.KEY_EXTRA_IMAGE_URI, photoInfo.getCroppedImageUri());
            setResult(Activity.RESULT_OK, resultIntent);
            openNoteCameraView.invalidate();
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
        });
        FrameLayout root = findViewById(getResources().getIdentifier("root", "id", getPackageName()));
        root.addView(openNoteCameraView,
                0,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        );
        root.addView(frameLayout, 1, openNoteCameraView.getLayoutParams());
    }

    private boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] requiredPermissions = new String[]{Manifest.permission.CAMERA};
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                requiredPermissions[1] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            }
            ActivityCompat.requestPermissions(ScannerActivity.this, requiredPermissions, PERMISSION_REQUEST_CODE);
            return false;
        } else {
            return true;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean readExternalStorage = Build.VERSION.SDK_INT > Build.VERSION_CODES.P || grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission && readExternalStorage) {
                openNoteCameraView.setCameraPermissionGranted();
            } else {
                setResult(Activity.RESULT_CANCELED);
                openNoteCameraView.invalidate();
                finish();
            }
        }
    }
}