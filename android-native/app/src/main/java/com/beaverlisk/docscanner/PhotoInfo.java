package com.beaverlisk.docscanner;

import android.net.Uri;

import org.opencv.core.Point;

import java.util.HashMap;

/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class PhotoInfo {

    private final int width;
    private final int height;
    private final Uri croppedImageUri;
    private final HashMap<String, Point> rectangleCoordinates;

    PhotoInfo(int width, int height, Uri croppedImagePath, HashMap<String, Point> rectangleCoordinates) {
        this.width = width;
        this.height = height;
        this.croppedImageUri = croppedImagePath;
        this.rectangleCoordinates = rectangleCoordinates;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Uri getCroppedImageUri() {
        return croppedImageUri;
    }

    public HashMap<String, Point> getRectangleCoordinates() {
        return rectangleCoordinates;
    }

}
