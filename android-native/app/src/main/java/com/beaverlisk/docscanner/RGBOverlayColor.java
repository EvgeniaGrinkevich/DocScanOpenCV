package com.beaverlisk.docscanner;

import android.graphics.Color;
import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class RGBOverlayColor {

    private static final String RGB_REGEX = "rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9]\\.?[0-9]?)*\\)";
    private static final int ALPHA_DEFAULT = 180;
    private static final int RED_DEFAULT = 66;
    private static final int GREEN_DEFAULT = 165;
    private static final int BLUE_DEFAULT = 245;

    private final Pattern rgbaPattern = Pattern.compile(RGB_REGEX);

    private int overlayColor = Color.argb(ALPHA_DEFAULT, RED_DEFAULT, GREEN_DEFAULT, BLUE_DEFAULT);
    private int borderColor = Color.argb(ALPHA_DEFAULT, RED_DEFAULT, GREEN_DEFAULT, BLUE_DEFAULT);

    public void setOverlayColor(int color) {
        overlayColor = color;
    }

    public void setBorderColor(int color) {
        borderColor = color;
    }

    public void setOverlayColor(String rgbColor) {
        overlayColor = parseColor(rgbColor);
    }

    public void setBorderColor(String rgbColor) {
        borderColor = parseColor(rgbColor);
    }

    public int getOverlayColor() {
        return overlayColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    private int parseColor(String rgb) {
        Matcher matcher = rgbaPattern.matcher(rgb);
        @Nullable String alphaGroup = null;
        @Nullable String redGroup = null;
        @Nullable String greenGroup = null;
        @Nullable String blueGroup = null;
        if (matcher.matches()) {
            alphaGroup = matcher.group(4);
            redGroup = matcher.group(1);
            greenGroup = matcher.group(2);
            blueGroup = matcher.group(3);
        }
        int alpha = alphaGroup != null ? (int) (255 * Float.parseFloat(alphaGroup)) : ALPHA_DEFAULT;
        int red = redGroup != null ? Integer.parseInt(redGroup) : RED_DEFAULT;
        int green = greenGroup != null ? Integer.parseInt(greenGroup) : GREEN_DEFAULT;
        int blue = blueGroup != null ? Integer.parseInt(blueGroup) : BLUE_DEFAULT;
        return Color.argb(alpha, red, green, blue);
    }

}
