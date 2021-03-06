package com.beaverlisk.docscanner;

import org.opencv.core.Mat;

/**
 * Created by Evgenia Grinkevich on 15, March, 2021
 **/
public class PreviewFrame {

    private final boolean previewOnly;
    private Mat frame;
    private boolean autoMode;

    public PreviewFrame(Mat frame, boolean autoMode, boolean previewOnly) {
        this.frame = frame;
        this.autoMode = autoMode;
        this.previewOnly = previewOnly;
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }
}
