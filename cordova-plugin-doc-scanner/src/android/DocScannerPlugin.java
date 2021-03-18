package com.exadel.docscanplugin;

import android.content.Intent;
import android.net.Uri;

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

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.callbackContext = callbackContext;
            try {
                Intent intent = new Intent(cordova.getActivity().getApplicationContext(), ScannerActivity.class);
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
        } else {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == cordova.getActivity().RESULT_OK) {
            Uri uri = data.getExtras().getParcelable(ScanConstants.KEY_EXTRA_IMAGE_URI);
            if (uri != null) {
                this.callbackContext.success(uri.toString());
            } else {
                this.callbackContext.error("null data from scan activity");
            }
        } else {
            this.callbackContext.error("Incorrect result or user canceled the action.");
        }
    }

}
