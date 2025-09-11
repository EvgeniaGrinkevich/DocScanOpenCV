package com.beaverlisk.docscanner;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class ImageProcessorMessage {

    public static final String MSG_PREVIEW_FRAME = "previewFrame";
    public static final String MSG_PICTURE_TAKEN = "pictureTaken";

    private final String command;
    private final Object obj;

    public ImageProcessorMessage(String command, Object obj) {
        this.command = command;
        this.obj = obj;
    }

    public String getCommand() {
        return this.command;
    }

    public Object getObj() {
        return this.obj;
    }
}
