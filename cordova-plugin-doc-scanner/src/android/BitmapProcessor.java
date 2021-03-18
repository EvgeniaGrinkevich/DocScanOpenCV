package com.exadel.docscanplugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class BitmapProcessor {

    private static final String TAG = "BitmapProcessor";

    @Nullable
    public static String convertToBase64(@NonNull Context context, @NonNull Uri imagePath) {
        Bitmap bitmap;
        String base64;
        InputStream stream;
        try {
            stream = context.getContentResolver().openInputStream(imagePath);
            bitmap = BitmapFactory.decodeStream(stream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
            stream.close();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath.toString());
            return null;
        }
        return base64;
    }

    @Nullable
    public static Bitmap decodeSampledBitmapFromUri(@NonNull Context context, @NonNull Uri imagePath, int reqWidth, int reqHeight) {
        Bitmap bitmap = null;
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(imagePath);
            BitmapFactory.decodeStream(stream, new Rect(), options);
            stream.close();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath.toString());
            return null;
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        try {
            stream = context.getContentResolver().openInputStream(imagePath);
            bitmap = BitmapFactory.decodeStream(stream, new Rect(), options);
            stream.close();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath.toString());
            return null;
        }
        return bitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}
