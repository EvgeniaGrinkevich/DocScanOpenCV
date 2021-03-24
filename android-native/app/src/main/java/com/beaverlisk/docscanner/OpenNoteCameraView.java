package com.beaverlisk.docscanner;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class OpenNoteCameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "JavaCameraView";
    private Context context;
    SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CanvasView canvasView;
    private ImageProcessorHandler imageProcessor;
    private boolean isFocused;
    private boolean isSafeToTakePicture;
    private Activity hostActivity;
    private boolean mFlashMode = false;
    private boolean autoMode = true;
    private boolean multiCapture = false;
    private HandlerThread imageProcessorThread;
    private View progressSpinner;
    private PictureCallback pCallback;

    private boolean documentAnimation = false;
    private int numberOfRectangles = 15;
    private Boolean enableTorch = false;

    private final PreviewOverlayColor argbOverlayColor = new PreviewOverlayColor();

    private View blinkView = null;
    private View mainView = null;
    private boolean manualCapture = false;

    private OnScannerListener onScannedListener = null;
    private OnProcessingListener processingListener = null;

    public interface OnScannerListener {
        void onPictureTaken(PhotoInfo photoInfo);

        void onError(Throwable error);
    }

    public interface OnProcessingListener {
        void onProcessingChange(boolean inProcessing);
    }

    public void setOnScannerListener(OnScannerListener listener) {
        this.onScannedListener = listener;
    }

    public void removeOnScannerListener() {
        this.onScannedListener = null;
    }

    public void setOnProcessingListener(OnProcessingListener processingListener) {
        this.processingListener = processingListener;
    }

    public void removeOnProcessingListener() {
        this.processingListener = null;
    }

    public OpenNoteCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenNoteCameraView(Context context, Integer numCam, Activity activity, FrameLayout frameLayout) {
        super(context, numCam);
        this.context = context;
        this.hostActivity = activity;
        pCallback = this;
        mainView = frameLayout;
        initOpenCv(context);
    }

    public void setDocumentAnimation(boolean animate) {
        this.documentAnimation = animate;
    }

    public void setOverlayColor(String rgbaColor) {
        argbOverlayColor.setOverlayColor(rgbaColor);
    }

    public void setOverlayBorderColor(String rgbaColor) {
        argbOverlayColor.setBorderColor(rgbaColor);
    }

    public int getOverlayColor() {
        return argbOverlayColor.getOverlayColor();
    }

    public int getOverlayBorderColor() {
        return argbOverlayColor.getBorderColor();
    }

    public void setDetectionCountBeforeCapture(int numberOfRectangles) {
        this.numberOfRectangles = numberOfRectangles;
    }

    public void setEnableTorch(boolean enableTorch) {
        this.enableTorch = enableTorch;
        if (mCamera != null) {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(enableTorch ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(p);
        }
    }

    public void setMultiCapture(boolean captureMultiple) {
        this.multiCapture = captureMultiple;
    }

    public void capture() {
        this.requestManualPicture();
    }

    public void setManualOnly(boolean manualOnly) {
        this.manualCapture = manualOnly;
    }

    public void setBrightness(double brightness) {
        if (imageProcessor != null) {
            imageProcessor.setBrightness(brightness);
        }
    }

    public void setContrast(double contrast) {
        if (imageProcessor != null) {
            imageProcessor.setContrast(contrast);
        }
    }

    public void initOpenCv(Context context) {
        canvasView = mainView.findViewById(R.id.canvas_view);
        progressSpinner = mainView.findViewById(R.id.wait_spinner);
        blinkView = mainView.findViewById(R.id.blink_view);

        blinkView.setBackgroundColor(Color.WHITE);
        hostActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Display display = hostActivity.getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);

        BaseLoaderCallback openCVCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    Log.d(TAG, "SUCCESS init openCV: " + status);
                    enableCameraView();
                } else {
                    Log.d(TAG, "ERROR init Opencv: " + status);
                    super.onManagerConnected(status);
                }
            }
        };

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, openCVCallback);
        } else {
            openCVCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (imageProcessorThread == null) {
            imageProcessorThread = new HandlerThread("Worker Thread");
            imageProcessorThread.start();
        }

        if (imageProcessor == null) {
            imageProcessor = new ImageProcessorHandler(imageProcessorThread.getLooper(), this);
        }
        this.setImageProcessorBusy(false);

    }

    public CanvasView getCanvasView() {
        return canvasView;
    }

    private boolean imageProcessorBusy = true;
    private boolean attemptToFocus = false;

    public void setImageProcessorBusy(boolean imageProcessorBusy) {
        this.imageProcessorBusy = imageProcessorBusy;
    }

    public void setAttemptToFocus(boolean attemptToFocus) {
        this.attemptToFocus = attemptToFocus;
    }

    public boolean isFocused() {
        return this.isFocused;
    }

    public void turnCameraOn() {
        surfaceView = mainView.findViewById(R.id.surfaceView);
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.setVisibility(SurfaceView.VISIBLE);
    }

    public void enableCameraView() {
        if (surfaceView == null) {
            turnCameraOn();
        }
    }

    private int findBestCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
            cameraId = i;
        }
        return cameraId;
    }

    public Size getMaxPreviewResolution() {
        int maxWidth = 0;
        Size currentResolution = null;
        mCamera.lock();
        for (Size size : getResolutionList()) {
            if (size.width > maxWidth) {
                Log.d(TAG, "supported preview resolution: " + size.width + "x" + size.height);
                maxWidth = size.width;
                currentResolution = size;
            }
        }
        return currentResolution;
    }

    public Size getMaxPictureResolution(float previewRatio) {
        int maxPixels = 0;
        int ratioMaxPixels = 0;
        Size currentMaxRes = null;
        Size ratioCurrentMaxRes = null;
        for (Size r : getPictureResolutionList()) {
            float pictureRatio = (float) r.width / r.height;
            Log.d(TAG, "supported picture resolution: " + r.width + "x" + r.height + " ratio: " + pictureRatio);
            int resolutionPixels = r.width * r.height;

            if (resolutionPixels > ratioMaxPixels && pictureRatio == previewRatio) {
                ratioMaxPixels = resolutionPixels;
                ratioCurrentMaxRes = r;
            }

            if (resolutionPixels > maxPixels) {
                maxPixels = resolutionPixels;
                currentMaxRes = r;
            }
        }

        if (ratioCurrentMaxRes != null) {
            Log.d(TAG, "Max supported picture resolution with preview aspect ratio: " + ratioCurrentMaxRes.width + "x"
                    + ratioCurrentMaxRes.height);
            return ratioCurrentMaxRes;

        }
        return currentMaxRes;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            int cameraId = findBestCamera();
            mCamera = Camera.open(cameraId);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error opening camera" + e.getMessage());
            return;
        }
        Camera.Parameters param;
        param = mCamera.getParameters();

        Size pSize = getMaxPreviewResolution();
        param.setPreviewSize(pSize.width, pSize.height);
        param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        float previewRatio = (float) pSize.width / pSize.height;

        Display display = hostActivity.getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);

        int displayWidth = Math.min(size.y, size.x);
        int displayHeight = Math.max(size.y, size.x);

        float displayRatio = (float) displayHeight / displayWidth;

        int previewHeight = displayHeight;

        if (displayRatio > previewRatio) {
            ViewGroup.LayoutParams surfaceParams = surfaceView.getLayoutParams();
            previewHeight = (int) ((float) size.y / displayRatio * previewRatio);
            surfaceParams.height = previewHeight;
            surfaceView.setLayoutParams(surfaceParams);
            canvasView.getLayoutParams().height = previewHeight;
        }

        int hotAreaWidth = displayWidth / 4;
        int hotAreaHeight = previewHeight / 2 - hotAreaWidth;

        Size maxRes = getMaxPictureResolution(previewRatio);
        if (maxRes != null) {
            param.setPictureSize(maxRes.width, maxRes.height);
            Log.d(TAG, "max supported picture resolution: " + maxRes.width + "x" + maxRes.height);
        }

        PackageManager pm = hostActivity.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
                && param.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            isFocused = true;
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            param.setFlashMode(enableTorch ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        }
        if (param.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        try {
            mCamera.setParameters(param);
        } catch (Exception e) {
            Log.e(TAG, "failed setting camera params");
            onScannedListener.onError(e);
            e.printStackTrace();
        }

        mCamera.setDisplayOrientation(90);

        if (imageProcessor != null) {
            imageProcessor.setNumOfRectangles(numberOfRectangles);
        }

        try {
            mCamera.setAutoFocusMoveCallback((start, camera) -> {
                isFocused = !start;
                Log.d(TAG, "focusMoving: " + isFocused);
            });
        } catch (Exception e) {
            Log.d(TAG, "failed setting AutoFocusMoveCallback");
        }
        // some devices doesn't call the AutoFocusMoveCallback - fake the
        // focus to true at the start
        isFocused = true;
        isSafeToTakePicture = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshCamera();
    }

    private void refreshCamera() {
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Unable to stop camera preview " + e.getMessage());
        }
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
        } catch (Exception e) {
            Log.e(TAG, "Unable to refresh camera, " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Size pictureSize = camera.getParameters().getPreviewSize();
        if (isFocused && !imageProcessorBusy) {
            setImageProcessorBusy(true);
            Mat yuv = new Mat(new org.opencv.core.Size(pictureSize.width, pictureSize.height * 1.5), CvType.CV_8UC1);
            yuv.put(0, 0, data);
            Mat mat = new Mat(new org.opencv.core.Size(pictureSize.width, pictureSize.height), CvType.CV_8UC4);
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            yuv.release();
            if (!manualCapture) {
                sendImageProcessorMessage(ImageProcessorMessage.MSG_PREVIEW_FRAME, new PreviewFrame(mat, autoMode, !(autoMode)));
            }
        }
    }

    public void invalidateCanvasView() {
        hostActivity.runOnUiThread(() -> canvasView.invalidate());
    }

    public void sendImageProcessorMessage(String messageText, Object obj) {
        Log.d(TAG, "sending message to ImageProcessor: " + messageText + " - " + obj.toString());
        Message msg = imageProcessor.obtainMessage();
        msg.obj = new ImageProcessorMessage(messageText, obj);
        imageProcessor.sendMessage(msg);
    }

    public void blinkScreenAndShutterSound() {
        AudioManager audio = (AudioManager) hostActivity.getSystemService(Context.AUDIO_SERVICE);
        switch (audio.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.SHUTTER_CLICK);
                break;
            case AudioManager.RINGER_MODE_SILENT:
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
        }
    }

    public void waitSpinnerVisible() {
        hostActivity.runOnUiThread(() -> {
            progressSpinner.setVisibility(View.VISIBLE);
            if (processingListener != null) {
                processingListener.onProcessingChange(true);
            }
        });
    }

    public void waitSpinnerInvisible() {
        hostActivity.runOnUiThread(() -> {
            progressSpinner.setVisibility(View.INVISIBLE);
            if (processingListener != null) {
                processingListener.onProcessingChange(false);
            }
        });
    }

    public void blinkScreen() {
        hostActivity.runOnUiThread(() -> {
            blinkView.bringToFront();
            blinkView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink));
            blinkView.setVisibility(View.INVISIBLE);
        });
    }

    public boolean requestManualPicture() {
        this.blinkScreenAndShutterSound();
        this.waitSpinnerVisible();
        if (isSafeToTakePicture) {
            isSafeToTakePicture = false;
            try {
                mCamera.takePicture(null, null, pCallback);
            } catch (Exception e) {
                this.waitSpinnerInvisible();
            }
            return true;
        }
        return false;
    }

    public boolean requestPicture() {
        PackageManager pm = hostActivity.getPackageManager();
        if (isSafeToTakePicture) {
            isSafeToTakePicture = false;
            try {
                if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
                    mCamera.autoFocus((success, camera) -> {
                        if (success) {
                            mCamera.takePicture(null, null, pCallback);
                            blinkScreen();
                            blinkScreenAndShutterSound();
                        }
                        if (attemptToFocus) {
                            return;
                        } else {
                            attemptToFocus = true;
                        }
                    });
                } else {
                    mCamera.takePicture(null, null, pCallback);
                    blinkScreen();
                    blinkScreenAndShutterSound();
                }
            } catch (Exception e) {
                waitSpinnerInvisible();
            } finally {
                waitSpinnerInvisible();
            }
            return true;
        }
        return false;
    }

    public void saveDocument(ScannedDocument scannedDocument) {
        Uri savedDocumentUri = DocumentSaver.save(context.getApplicationContext(), "DocScanner", scannedDocument);
        if (savedDocumentUri != null) {
            animateDocument(savedDocumentUri, scannedDocument);
            if (onScannedListener != null) {
                onScannedListener.onPictureTaken(new PhotoInfo(
                        scannedDocument.widthWithRatio,
                        scannedDocument.heightWithRatio,
                        savedDocumentUri,
                        scannedDocument.previewPointsAsHash()
                ));
            }
        }
        if (multiCapture) {
            refreshCamera();
        }
    }

    private void animateDocument(@NonNull Uri imageUri, @NonNull ScannedDocument scannedDocument) {
        Display display = hostActivity.getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        ImageView imageView = mainView.findViewById(R.id.image_view_scanned_animation);
        AnimationRunnable runnable = new AnimationRunnable(imageUri, scannedDocument, imageView, displaySize);
        hostActivity.runOnUiThread(runnable);
        this.waitSpinnerInvisible();
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public boolean isEffectSupported(String effect) {
        List<String> effectList = getEffectList();
        for (String str : effectList) {
            if (str.trim().contains(effect))
                return true;
        }
        return false;
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public List<Size> getPictureResolutionList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setMaxPictureResolution() {
        int maxWidth = 0;
        Size curRes = null;
        for (Size r : getPictureResolutionList()) {
            Log.d(TAG, "supported picture resolution: " + r.width + "x" + r.height);
            if (r.width > maxWidth) {
                maxWidth = r.width;
                curRes = r;
            }
        }

        if (curRes != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPictureSize(curRes.width, curRes.height);
            mCamera.setParameters(parameters);
            Log.d(TAG, "selected picture resolution: " + curRes.width + "x" + curRes.height);
        }
    }

    public void setMaxPreviewResolution() {
        int maxWidth = 0;
        Size curRes = null;
        mCamera.lock();
        for (Size r : getResolutionList()) {
            if (r.width > maxWidth) {
                Log.d(TAG, "supported preview resolution: " + r.width + "x" + r.height);
                maxWidth = r.width;
                curRes = r;
            }
        }

        if (curRes != null) {
            setResolution(curRes);
            Log.d(TAG, "selected preview resolution: " + curRes.width + "x" + curRes.height);
        }
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
        Log.d(TAG, "resolution: " + resolution.width + " x " + resolution.height);
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void setFlash(boolean stateFlash) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(stateFlash ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
        Log.d(TAG, "flash: " + (stateFlash ? "on" : "off"));
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Mat orig = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
        Bitmap myBitmap32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(myBitmap32, orig);
        setImageProcessorBusy(true);
        sendImageProcessorMessage(ImageProcessorMessage.MSG_PICTURE_TAKEN, orig);
        camera.cancelAutoFocus();
        isSafeToTakePicture = true;
        waitSpinnerInvisible();
    }

}