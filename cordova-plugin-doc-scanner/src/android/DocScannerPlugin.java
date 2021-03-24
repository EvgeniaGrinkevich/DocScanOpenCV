package com.exadel.docscanplugin;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.beaverlisk.docscanner.BitmapProcessor;
import com.beaverlisk.docscanner.ScanConstants;
import com.beaverlisk.docscanner.ScannerActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Evgenia Grinkevich on 18, March, 2021
 **/
public class DocScannerPlugin extends CordovaPlugin {

    private static final int REQUEST_CODE_SCAN = 99;
    public CallbackContext callbackContext;
    private boolean useBase64 = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.callbackContext = callbackContext;
            try {
                Bundle optionsBundle = createOptionsBundle(args);
                Intent intent = new Intent(cordova.getActivity().getApplicationContext(), ScannerActivity.class);
                intent.putExtra(ScanConstants.KEY_BUNDLE_EXTRA, optionsBundle);
                cordova.setActivityResultCallback(this);
                cordova.getActivity().startActivityForResult(intent, REQUEST_CODE_SCAN);
            } catch (IllegalArgumentException e) {
                this.callbackContext.error("Illegal Argument Exception");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                this.callbackContext.sendPluginResult(r);
            } catch (Exception e) {
                this.callbackContext.error("Something went wrong!");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                this.callbackContext.sendPluginResult(r);
            }
            return true;
        }
        return false;
    }

    private Bundle createOptionsBundle(JSONArray args) throws JSONException {
        Bundle bundle = new Bundle();

        String overlayColor = args.getString(0);
        String borderColor = args.getString(1);
        int detectionCountBeforeCapture = args.getInt(2);
        boolean enableTorch = args.getBoolean(3);
        double brightness = args.getDouble(4);
        double contrast = args.getDouble(5);
        useBase64 = args.getBoolean(6);
        boolean captureMultiple = args.getBoolean(7);
        if (overlayColor != null && !overlayColor.isEmpty()) {
            bundle.putString(ScanConstants.KEY_BUNDLE_PROP_OVERLAY_COLOR, overlayColor);
        }
        if (borderColor != null && !borderColor.isEmpty()) {
            bundle.putString(ScanConstants.KEY_BUNDLE_PROP_BORDER_COLOR, borderColor);
        }
        if (detectionCountBeforeCapture != -1) {
            bundle.putInt(ScanConstants.KEY_BUNDLE_PROP_DETECTION_COUNT, detectionCountBeforeCapture);
        }
        if (brightness != -1) {
            bundle.putDouble(ScanConstants.KEY_BUNDLE_PROP_BRIGHTNESS, brightness);
        }
        if (contrast != -1) {
            bundle.putDouble(ScanConstants.KEY_BUNDLE_PROP_CONTRAST, contrast);
        }
        bundle.putBoolean(ScanConstants.KEY_BUNDLE_PROP_ENABLE_TORCH, enableTorch);
        bundle.putBoolean(ScanConstants.KEY_BUNDLE_PROP_USE_BASE_64, useBase64);
        bundle.putBoolean(ScanConstants.KEY_BUNDLE_PROP_CAPTURE_MULTIPLE, captureMultiple);

        return bundle;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == cordova.getActivity().RESULT_OK) {
            Uri uri = data.getExtras().getParcelable(ScanConstants.KEY_EXTRA_IMAGE_CONTENT_URI);
            if (uri != null) {
                this.callbackContext.success(
                        useBase64
                                ? BitmapProcessor.convertToBase64(cordova.getContext(), uri)
                                : getRealPath(uri)
                );
            } else {
                this.callbackContext.error("null data from scan activity");
            }
        } else {
            this.callbackContext.error("Incorrect result or user canceled the action.");
        }
    }

    private String getRealPath(Uri contentUri) {
        String picturePath = null;
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = cordova.getActivity().getContentResolver()
                .query(contentUri, filePathColumn, null, null, null);
        if (cursor == null) return null;
        if (cursor.moveToFirst()) {
            picturePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
        }
        cursor.close();
        return picturePath;
    }
}