package com.exadel.docscanplugin;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class ImageProcessorHandler extends Handler {

    private static final String TAG = "ImageProcessor";

    private final OpenNoteCameraView openNoteCameraView;

    private double colorGain = 1;  // contrast
    private double colorBias = 10; // brightness

    private Size previewSize;
    private Point[] previewPoints;

    private int numOfSquares = 0;
    private int numOfRectangles = 10;

    public ImageProcessorHandler(Looper looper, OpenNoteCameraView openNoteCameraView) {
        super(looper);
        this.openNoteCameraView = openNoteCameraView;
    }

    public void setNumOfRectangles(int numOfRectangles) {
        this.numOfRectangles = numOfRectangles;
    }

    public void setBrightness(double brightness) {
        this.colorBias = brightness;
    }

    public void setContrast(double contrast) {
        this.colorGain = contrast;
    }

    public void handleMessage(Message msg) {
        if (msg.obj.getClass() == ImageProcessorMessage.class) {
            ImageProcessorMessage obj = (ImageProcessorMessage) msg.obj;
            String command = obj.getCommand();
            Log.d(TAG, "Message Received: " + command + " - " + obj.getObj().toString());
            switch (command) {
                case ImageProcessorMessage.MSG_PREVIEW_FRAME:
                    processPreviewFrame((PreviewFrame) obj.getObj());
                    break;
                case ImageProcessorMessage.MSG_PICTURE_TAKEN:
                    processPicture((Mat) obj.getObj());
                    break;
            }
        }
    }

    private void processPreviewFrame(PreviewFrame previewFrame) {
        Mat frame = previewFrame.getFrame();
        boolean focused = openNoteCameraView.isFocused();
        if (detectPreviewDocument(frame) && focused) {
            numOfSquares++;
            if (numOfSquares == numOfRectangles) {
                openNoteCameraView.requestPicture();
                openNoteCameraView.waitSpinnerVisible();
                numOfSquares = 0;
            }
        } else {
            numOfSquares = 0;
        }
        frame.release();
        openNoteCameraView.setImageProcessorBusy(false);
    }

    public void processPicture(Mat picture) {
        Mat img = picture.clone();
        picture.release();
        Log.d(TAG, "processPicture - imported image " + img.size().width + "x" + img.size().height);
        ScannedDocument doc = detectDocument(img);
        openNoteCameraView.getCanvasView().clear();
        openNoteCameraView.invalidateCanvasView();
        openNoteCameraView.saveDocument(doc);
        doc.release();
        picture.release();
        openNoteCameraView.setImageProcessorBusy(false);
        openNoteCameraView.setAttemptToFocus(false);
        openNoteCameraView.waitSpinnerInvisible();
    }

    private ScannedDocument detectDocument(Mat inputRgba) {
        ArrayList<MatOfPoint> contours = findContours(inputRgba);
        ScannedDocument sd = new ScannedDocument(inputRgba);
        sd.originalSize = inputRgba.size();
        Quadrilateral quad = getQuadrilateral(contours, sd.originalSize);
        double ratio = sd.originalSize.height / 500;
        sd.heightWithRatio = Double.valueOf(sd.originalSize.width / ratio).intValue();
        sd.widthWithRatio = Double.valueOf(sd.originalSize.height / ratio).intValue();
        Mat doc;
        if (quad != null) {
            sd.originalPoints = new Point[4];
            sd.originalPoints[0] = new Point(sd.widthWithRatio - quad.points[3].y, quad.points[3].x); // TopLeft
            sd.originalPoints[1] = new Point(sd.widthWithRatio - quad.points[0].y, quad.points[0].x); // TopRight
            sd.originalPoints[2] = new Point(sd.widthWithRatio - quad.points[1].y, quad.points[1].x); // BottomRight
            sd.originalPoints[3] = new Point(sd.widthWithRatio - quad.points[2].y, quad.points[2].x); // BottomLeft
            sd.quadrilateral = quad;
            sd.previewPoints = previewPoints;
            sd.previewSize = previewSize;
            doc = fourPointTransform(inputRgba, quad.points);
        } else {
            doc = new Mat(inputRgba.size(), CvType.CV_8UC4);
            inputRgba.copyTo(doc);
        }
        enhanceDocument(doc);
        return sd.setProcessed(doc);
    }

    private boolean detectPreviewDocument(Mat inputRgba) {
        ArrayList<MatOfPoint> contours = findContours(inputRgba);
        Quadrilateral quad = getQuadrilateral(contours, inputRgba.size());
        previewPoints = null;
        previewSize = inputRgba.size();
        if (quad != null) {
            Point[] rescaledPoints = new Point[4];
            double ratio = inputRgba.size().height / 500;
            for (int i = 0; i < 4; i++) {
                int x = Double.valueOf(quad.points[i].x * ratio).intValue();
                int y = Double.valueOf(quad.points[i].y * ratio).intValue();
                rescaledPoints[i] = new Point(x, y);
            }
            previewPoints = rescaledPoints;
            drawDocumentBox(previewPoints, previewSize);
            return true;
        }
        openNoteCameraView.getCanvasView().clear();
        openNoteCameraView.invalidateCanvasView();
        return false;
    }

    private void drawDocumentBox(Point[] points, Size stdSize) {
        Path path = new Path();
        CanvasView canvasView = openNoteCameraView.getCanvasView();
        float previewWidth = (float) stdSize.height;
        float previewHeight = (float) stdSize.width;

        path.moveTo(previewWidth - (float) points[0].y, (float) points[0].x);
        path.lineTo(previewWidth - (float) points[1].y, (float) points[1].x);
        path.lineTo(previewWidth - (float) points[2].y, (float) points[2].x);
        path.lineTo(previewWidth - (float) points[3].y, (float) points[3].x);
        path.close();

        PathShape newBox = new PathShape(path, previewWidth, previewHeight);

        Paint paint = new Paint();
        paint.setColor(openNoteCameraView.getOverlayColor());

        Paint border = new Paint();
        border.setColor(openNoteCameraView.getOverlayBorderColor());
        border.setStrokeWidth(5);

        canvasView.clear();
        canvasView.addShape(newBox, paint, border);
        openNoteCameraView.invalidateCanvasView();
    }

    private Quadrilateral getQuadrilateral(ArrayList<MatOfPoint> contours, Size srcSize) {
        double ratio = srcSize.height / 500;
        int height = Double.valueOf(srcSize.height / ratio).intValue();
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        Size size = new Size(width, height);
        for (MatOfPoint contour : contours) {
            MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            Point[] points = approx.toArray();
            // select biggest 4 angles polygon
            // if (points.length == 4) {
            Point[] foundPoints = sortPoints(points);
            if (insideArea(foundPoints, size)) {
                return new Quadrilateral(contour, foundPoints);
            }
        }
        return null;
    }

    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};
        Comparator<Point> sumComparator = (lhs, rhs) -> Double.compare(lhs.y + lhs.x, rhs.y + rhs.x);
        Comparator<Point> diffComparator = (lhs, rhs) -> Double.compare(lhs.y - lhs.x, rhs.y - rhs.x);

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private boolean insideArea(Point[] rp, Size size) {
        int width = Double.valueOf(size.width).intValue();

        int minimumSize = width / 10;

        boolean isANormalShape = rp[0].x != rp[1].x && rp[1].y != rp[0].y && rp[2].y != rp[3].y && rp[3].x != rp[2].x;
        boolean isBigEnough = ((rp[1].x - rp[0].x >= minimumSize) && (rp[2].x - rp[3].x >= minimumSize)
                && (rp[3].y - rp[0].y >= minimumSize) && (rp[2].y - rp[1].y >= minimumSize));

        double leftOffset = rp[0].x - rp[3].x;
        double rightOffset = rp[1].x - rp[2].x;
        double bottomOffset = rp[0].y - rp[1].y;
        double topOffset = rp[2].y - rp[3].y;

        boolean isAnActualRectangle = ((leftOffset <= minimumSize && leftOffset >= -minimumSize)
                && (rightOffset <= minimumSize && rightOffset >= -minimumSize)
                && (bottomOffset <= minimumSize && bottomOffset >= -minimumSize)
                && (topOffset <= minimumSize && topOffset >= -minimumSize));

        return isANormalShape && isAnActualRectangle && isBigEnough;
    }

    private void enhanceDocument(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        src.convertTo(src, CvType.CV_8UC1, colorGain, colorBias);
    }

    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / 500;

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB) * ratio;
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB) * ratio;
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x * ratio, tl.y * ratio, tr.x * ratio, tr.y * ratio, br.x * ratio, br.y * ratio,
                bl.x * ratio, bl.y * ratio);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

    private ArrayList<MatOfPoint> findContours(Mat src) {

        Mat grayImage;
        Mat cannedImage;
        Mat resizedImage;

        double ratio = src.size().height / 500;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();
        Size size = new Size(width, height);

        resizedImage = new Mat(size, CvType.CV_8UC4);
        grayImage = new Mat(size, CvType.CV_8UC4);
        cannedImage = new Mat(size, CvType.CV_8UC1);

        Imgproc.resize(src, resizedImage, size);
        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGBA2GRAY, 4);
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Imgproc.Canny(grayImage, cannedImage, 80, 100, 3, false);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        contours.sort((lhs, rhs) -> Double.compare(Imgproc.contourArea(rhs), Imgproc.contourArea(lhs)));

        resizedImage.release();
        grayImage.release();
        cannedImage.release();

        return contours;
    }

}