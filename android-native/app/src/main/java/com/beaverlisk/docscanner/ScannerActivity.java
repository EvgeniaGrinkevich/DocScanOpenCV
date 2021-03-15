package com.beaverlisk.docscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class ScannerActivity extends AppCompatActivity {

    private final int PERMISSION_REQUEST_CODE = 1029;
    private OpenNoteCameraView openNoteCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout frameLayout = (FrameLayout) lf.inflate(R.layout.activity_open_note_scanner, null);
        openNoteCameraView = new OpenNoteCameraView(this, -1, this, frameLayout);
        if (requestPermissions()) {
            openNoteCameraView.setCameraPermissionGranted();
        }
        openNoteCameraView.setOnProcessingListener((inProcessing) -> Log.w("WUF", "onProcessingChange"));
        openNoteCameraView.setOnScannerListener(photoInfo -> Log.w("WUF", "onPictureTaken"));
        FrameLayout root = findViewById(R.id.root);
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
        openNoteCameraView.removeOnProcessingListener();
        openNoteCameraView.removeOnScannerListener();
        openNoteCameraView = null;
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
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.snackbar_msg_request_permissions, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.snackbar_button_enable), v -> requestPermissions())
                        .show();
            }
        }
    }
}