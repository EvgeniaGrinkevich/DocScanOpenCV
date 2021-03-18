package com.beaverlisk.docscanner;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class Quadrilateral {

    public MatOfPoint contour;
    public Point[] points;

    public Quadrilateral(MatOfPoint contour, Point[] points) {
        this.contour = contour;
        this.points = points;
    }
}

