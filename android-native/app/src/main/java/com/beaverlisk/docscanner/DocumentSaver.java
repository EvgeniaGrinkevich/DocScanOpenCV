package com.beaverlisk.docscanner;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class DocumentSaver {

    private static final String TAG = "DocumentSaver";

    @WorkerThread
    @Nullable
    public static Uri save(Context appContext, String folderName, ScannedDocument document) {
        Mat doc = (document.processed != null) ? document.processed : document.original;
        Mat endDoc = new Mat(
                Double.valueOf(doc.size().width).intValue(),
                Double.valueOf(doc.size().height).intValue(),
                CvType.CV_8UC4);
        Core.flip(doc.t(), endDoc, 1);

        Uri scannedDocumentUri;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, ScanConstants.MIME_TYPE_IMAGE_JPEG);
        values.put(MediaStore.Images.Media.DATE_ADDED, document.dateCreated / 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName);
            values.put(MediaStore.Images.Media.DATE_TAKEN, document.dateCreated);
            values.put(MediaStore.Images.Media.IS_PENDING, true);
            scannedDocumentUri = appContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (scannedDocumentUri != null) {
                boolean writeSuccess = false;
                try (OutputStream outputStream = appContext.getContentResolver().openOutputStream(scannedDocumentUri)) {
                    if (outputStream != null) {
                        MatOfByte matOfByte = new MatOfByte();
                        Imgcodecs.imencode(ScanConstants.IMAGE_FILE_FORMAT_EXTENSION_JPEG, endDoc, matOfByte);
                        byte[] byteArray = matOfByte.toArray();
                        outputStream.write(byteArray);
                        outputStream.flush();
                        writeSuccess = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during saving file: " + e.getMessage());
                    e.printStackTrace();
                }

                if (writeSuccess) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    appContext.getContentResolver().update(scannedDocumentUri, values, null, null);
                } else {
                    try {
                        appContext.getContentResolver().delete(scannedDocumentUri, null, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to delete incomplete file entry: " + e.getMessage());
                    }
                    scannedDocumentUri = null;
                }
            }
        } else {
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/" + folderName);
            if (!folder.exists()) {
                folder.mkdirs();
                Log.d(TAG, "wrote: created folder " + folder.getPath());
            }
            String filePath = Environment.getExternalStorageDirectory().toString()
                    + "/"
                    + folderName
                    + "/"
                    + document.dateCreated
                    + ScanConstants.IMAGE_FILE_FORMAT_EXTENSION_JPEG;
            Imgcodecs.imwrite(filePath, endDoc);
            values.put(MediaStore.MediaColumns.DATA, filePath);
            scannedDocumentUri = appContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
        endDoc.release();
        return scannedDocumentUri;
    }
}
