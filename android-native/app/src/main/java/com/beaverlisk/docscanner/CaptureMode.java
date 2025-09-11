package com.beaverlisk.docscanner;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgenia Grinkevich on 11, september, 2025
 **/
public enum CaptureMode {
    AUTO(0),
    MANUAL(1),
    AUTO_MANUAL(2);

    private final int value;
    private static final Map<Integer, CaptureMode> map = new HashMap<>();

    CaptureMode(int value) {
        this.value = value;
    }

    public static CaptureMode valueOf(int captureMode) {
        return (CaptureMode) map.get(captureMode);
    }

    public int getValue() {
        return this.value;
    }

    static {
        for (CaptureMode captureMode : values()) {
            map.put(captureMode.value, captureMode);
        }
    }
}