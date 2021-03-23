package com.beaverlisk.docscanner;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.opencv.core.Point;
import org.opencv.core.Size;

import java.lang.ref.WeakReference;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
class AnimationRunnable implements Runnable {

    private static final String TAG = "AnimationRunnable";
    private final Size imageSize;
    private final WeakReference<ImageView> imageViewWeakReference;
    private final android.graphics.Point displaySize;
    private Point[] previewPoints = null;
    public org.opencv.core.Size previewSize = null;
    public Uri imageUri;
    private Bitmap bitmap;

    public AnimationRunnable(@NonNull Uri imageUri,
                             @NonNull ScannedDocument document,
                             @NonNull ImageView imageView,
                             @NonNull android.graphics.Point displaySize) {
        this.imageUri = imageUri;
        this.imageSize = document.processed.size();
        imageViewWeakReference = new WeakReference<>(imageView);
        this.displaySize = displaySize;
        if (document.quadrilateral != null) {
            this.previewPoints = document.previewPoints;
            this.previewSize = document.previewSize;
        }
    }

    public double hypotenuse(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    @Override
    public void run() {
        ImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return;

        int width = Math.min(displaySize.x, displaySize.y);
        int height = Math.max(displaySize.x, displaySize.y);
        // captured images are always in landscape, values should be swapped
        double imageWidth = imageSize.height;
        double imageHeight = imageSize.width;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

        if (previewPoints != null) {
            double documentLeftHeight = hypotenuse(previewPoints[0], previewPoints[1]);
            double documentBottomWidth = hypotenuse(previewPoints[1], previewPoints[2]);
            double documentRightHeight = hypotenuse(previewPoints[2], previewPoints[3]);
            double documentTopWidth = hypotenuse(previewPoints[3], previewPoints[0]);

            double documentWidth = Math.max(documentTopWidth, documentBottomWidth);
            double documentHeight = Math.max(documentLeftHeight, documentRightHeight);

            Log.d(TAG, "device: " + width + "x" + height + " image: " + imageWidth + "x" + imageHeight
                    + " document: " + documentWidth + "x" + documentHeight);

            Log.d(TAG, "previewPoints[0] x=" + previewPoints[0].x + " y=" + previewPoints[0].y);
            Log.d(TAG, "previewPoints[1] x=" + previewPoints[1].x + " y=" + previewPoints[1].y);
            Log.d(TAG, "previewPoints[2] x=" + previewPoints[2].x + " y=" + previewPoints[2].y);
            Log.d(TAG, "previewPoints[3] x=" + previewPoints[3].x + " y=" + previewPoints[3].y);

            // again, swap width and height
            double xRatio = width / previewSize.height;
            double yRatio = height / previewSize.width;

            params.topMargin = (int) (previewPoints[3].x * yRatio);
            params.leftMargin = (int) ((previewSize.height - previewPoints[3].y) * xRatio);
            params.width = (int) (documentWidth * xRatio);
            params.height = (int) (documentHeight * yRatio);
        } else {
            params.topMargin = height / 4;
            params.leftMargin = width / 4;
            params.width = width / 2;
            params.height = height / 2;
        }

        bitmap = BitmapProcessor.decodeSampledBitmapFromUri(imageView.getContext(), imageUri, params.width, params.height);
        if (bitmap == null) return;

        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);

        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0);
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.ABSOLUTE,
                0,
                Animation.ABSOLUTE,
                -params.leftMargin,
                Animation.ABSOLUTE,
                0,
                Animation.ABSOLUTE,
                height - params.topMargin
        );

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        animationSet.setDuration(600);
        animationSet.setInterpolator(new AccelerateInterpolator());
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.setVisibility(View.INVISIBLE);
                imageView.setImageBitmap(null);
                bitmap.recycle();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        imageView.startAnimation(animationSet);
    }
}

