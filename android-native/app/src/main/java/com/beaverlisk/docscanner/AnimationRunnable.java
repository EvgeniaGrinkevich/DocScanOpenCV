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
    public Size previewSize = null;
    public Uri imageUri;
    private Bitmap bitmap;

    public AnimationRunnable(@NonNull Uri imageUri,
                             @NonNull ScannedDocument document,
                             @NonNull ImageView imageView,
                             @NonNull android.graphics.Point displaySize) {
        this.imageUri = imageUri;
        this.imageSize = document.processed.size();
        this.imageViewWeakReference = new WeakReference<>(imageView);
        this.displaySize = displaySize;
        if (document.quadrilateral != null) {
            this.previewPoints = document.previewPoints;
            this.previewSize = document.previewSize;
        }
    }

    public double hypotenuse(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2.0F) + Math.pow(a.y - b.y, 2.0F));
    }

    @Override
    public void run() {
        final ImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return;

        int width = Math.min(this.displaySize.x, this.displaySize.y);
        int height = Math.max(this.displaySize.x, this.displaySize.y);
        // captured images are always in landscape, values should be swapped
        double imageWidth = this.imageSize.height;
        double imageHeight = this.imageSize.width;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

        if (this.previewPoints != null) {
            double documentLeftHeight = hypotenuse(previewPoints[0], this.previewPoints[1]);
            double documentBottomWidth = hypotenuse(previewPoints[1], this.previewPoints[2]);
            double documentRightHeight = hypotenuse(previewPoints[2], this.previewPoints[3]);
            double documentTopWidth = hypotenuse(previewPoints[3], this.previewPoints[0]);
            double documentWidth = Math.max(documentTopWidth, documentBottomWidth);
            double documentHeight = Math.max(documentLeftHeight, documentRightHeight);

            Log.d(TAG, "device: " + width + "x" + height + " image: " + imageWidth + "x" + imageHeight
                    + " document: " + documentWidth + "x" + documentHeight);
            Log.d(TAG, "previewPoints[0] x=" + this.previewPoints[0].x + " y=" + this.previewPoints[0].y);
            Log.d(TAG, "previewPoints[1] x=" + this.previewPoints[1].x + " y=" + this.previewPoints[1].y);
            Log.d(TAG, "previewPoints[2] x=" + this.previewPoints[2].x + " y=" + this.previewPoints[2].y);
            Log.d(TAG, "previewPoints[3] x=" + this.previewPoints[3].x + " y=" + this.previewPoints[3].y);

            // again, swap width and height
            double xRatio = (double) width / this.previewSize.height;
            double yRatio = (double) height / this.previewSize.width;

            params.topMargin = (int) (this.previewPoints[3].x * yRatio);
            params.leftMargin = (int) ((this.previewSize.height - this.previewPoints[3].y) * xRatio);
            params.width = (int) (documentWidth * xRatio);
            params.height = (int) (documentHeight * yRatio);
        } else {
            params.topMargin = height / 4;
            params.leftMargin = width / 4;
            params.width = width / 2;
            params.height = height / 2;
        }

        this.bitmap = BitmapProcessor.decodeSampledBitmapFromUri(imageView.getContext(), this.imageUri, params.width, params.height);
        if (this.bitmap == null) return;

        imageView.setImageBitmap(this.bitmap);
        imageView.setVisibility(View.VISIBLE);

        AnimationSet animationSet = getAnimationSet(params, height, imageView);
        imageView.startAnimation(animationSet);
    }

    private AnimationSet getAnimationSet(RelativeLayout.LayoutParams params, int height, ImageView imageView) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0F, 0.0F, 1.0F, 0.0F);
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.ABSOLUTE,
                0.0F,
                Animation.ABSOLUTE,
                (float) (-params.leftMargin),
                Animation.ABSOLUTE,
                0.0F,
                Animation.ABSOLUTE,
                (float) (height - params.topMargin)
        );

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        animationSet.setDuration(600L);
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
        return animationSet;
    }
}
