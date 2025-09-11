package com.beaverlisk.docscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class BitmapProcessor {

    private static final String TAG = "BitmapProcessor";

    @Nullable
    public static String convertToBase64(@NonNull Context context, @NonNull Uri imagePath) {
        try {
            InputStream stream = context.getContentResolver().openInputStream(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            String base64 = Base64.encodeToString(outputStream.toByteArray(), 2);
            stream.close();
            return base64;
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath);
            return null;
        }
    }

    @Nullable
    public static Bitmap decodeSampledBitmapFromUri(@NonNull Context context, @NonNull Uri imagePath, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            InputStream stream = context.getContentResolver().openInputStream(imagePath);
            BitmapFactory.decodeStream(stream, new Rect(), options);
            stream.close();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath);
            return null;
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        try {
            InputStream stream = context.getContentResolver().openInputStream(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, new Rect(), options);
            stream.close();
            return bitmap;
        } catch (Exception exception) {
            Log.e(TAG, "Unable to open or close input stream for Uri " + imagePath);
            return null;
        }
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
