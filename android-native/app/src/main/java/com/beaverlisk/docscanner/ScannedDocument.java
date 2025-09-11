package com.beaverlisk.docscanner;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.HashMap;

/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class ScannedDocument {

    public static final String KEY_RECTANGLE_TOP_LEFT = "topLeft";
    public static final String KEY_RECTANGLE_TOP_RIGHT = "topRight";
    public static final String KEY_RECTANGLE_BOTTOM_RIGHT = "bottomRight";
    public static final String KEY_RECTANGLE_BOTTOM_LEFT = "bottomLeft";
    public Mat original;
    public Mat processed;
    public Quadrilateral quadrilateral;
    public Point[] previewPoints;
    public Size previewSize;
    public Size originalSize;
    public Point[] originalPoints;
    public int heightWithRatio;
    public int widthWithRatio;
    public long dateCreated;

    public ScannedDocument(Mat original) {
        this.original = original;
        this.dateCreated = System.currentTimeMillis();
    }

    public Mat getProcessed() {
        return processed;
    }

    public ScannedDocument setProcessed(Mat processed) {
        this.processed = processed;
        return this;
    }

    public HashMap<String, Point> previewPointsAsHash() {
        if (this.previewPoints == null) return null;

        Point topLeft = new Point(originalPoints[0].x, originalPoints[0].y);
        Point topRight = new Point(originalPoints[1].x, originalPoints[1].y);
        Point bottomRight = new Point(originalPoints[2].x, originalPoints[2].y);
        Point bottomLeft = new Point(originalPoints[3].x, originalPoints[3].y);

        HashMap<String, Point> rectangleCoordinates = new HashMap<>();
        rectangleCoordinates.put(KEY_RECTANGLE_TOP_LEFT, topLeft);
        rectangleCoordinates.put(KEY_RECTANGLE_TOP_RIGHT, topRight);
        rectangleCoordinates.put(KEY_RECTANGLE_BOTTOM_RIGHT, bottomRight);
        rectangleCoordinates.put(KEY_RECTANGLE_BOTTOM_LEFT, bottomLeft);

        return rectangleCoordinates;
    }

    public void release() {
        if (processed != null) processed.release();
        if (original != null) original.release();
        if (quadrilateral != null && quadrilateral.contour != null) quadrilateral.contour.release();
    }
}
