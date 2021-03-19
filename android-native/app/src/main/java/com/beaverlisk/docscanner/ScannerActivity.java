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
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class ScannerActivity extends AppCompatActivity {

    private final int PERMISSION_REQUEST_CODE = 1029;
    private OpenNoteCameraView openNoteCameraView;
    private FrameLayout rootContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootContainer = (FrameLayout) lf.inflate(R.layout.activity_open_note_scanner, null);
        openNoteCameraView = new OpenNoteCameraView(this, -1, this, rootContainer);
        openNoteCameraView.setOnProcessingListener((inProcessing) -> Log.w("WUF", "onProcessingChange"));
        openNoteCameraView.setOnScannerListener(new OpenNoteCameraView.OnScannerListener() {
            @Override
            public void onPictureTaken(PhotoInfo photoInfo) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(ScanConstants.KEY_EXTRA_IMAGE_CONTENT_URI, photoInfo.getCroppedImageUri());
                setResult(Activity.RESULT_OK, resultIntent);
                openNoteCameraView.invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1000);
            }

            @Override
            public void onError(Throwable error) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(ScanConstants.KEY_EXTRA_ERROR, error);
                setResult(Activity.RESULT_CANCELED, resultIntent);
                openNoteCameraView.invalidate();
                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1000);
            }
        });
        if (requestPermissions()) {
            initCameraView();
        }
    }

    private void initCameraView() {
        FrameLayout root = findViewById(R.id.root);
        root.addView(openNoteCameraView,
                0,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        );
        root.addView(rootContainer, 1, openNoteCameraView.getLayoutParams());
        openNoteCameraView.setCameraPermissionGranted();
    }

    private boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            String[] requiredPermissions = new String[2];
            requiredPermissions[0] = Manifest.permission.CAMERA;
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