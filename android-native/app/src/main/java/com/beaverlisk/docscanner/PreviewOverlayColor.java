package com.beaverlisk.docscanner;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Evgenia Grinkevich on 17, March, 2021
 **/
public class PreviewOverlayColor {

    private static final String TAG = "PreviewOverlayColor";

    private static final String RGB_REGEX = "rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9]\\.?[0-9]?)*\\)";
    private final Pattern rgbaPattern = Pattern.compile(RGB_REGEX);

    private static final int ALPHA_DEFAULT = 180;
    private static final int RED_DEFAULT = 66;
    private static final int GREEN_DEFAULT = 165;
    private static final int BLUE_DEFAULT = 245;

    private int overlayColor = Color.argb(ALPHA_DEFAULT, RED_DEFAULT, GREEN_DEFAULT, BLUE_DEFAULT);
    private int borderColor = Color.argb(ALPHA_DEFAULT, RED_DEFAULT, GREEN_DEFAULT, BLUE_DEFAULT);

    public void setOverlayColor(int color) {
        overlayColor = color;
    }

    public void setBorderColor(int color) {
        borderColor = color;
    }

    /**
     * Supported formats are:
     * #RRGGBB #AARRGGBB rgba(RRR, GGG, BBB, A.A);
     * <p>
     * The following names are also accepted:
     * red, blue, green, black, white, gray, cyan, magenta, yellow, lightgray, darkgray, grey,
     * lightgrey, darkgrey, aqua, fuchsia, lime, maroon, navy, olive, purple, silver, and teal.
     */
    public void setOverlayColor(String color) {
        overlayColor = parseColor(color);
    }

    /**
     * Supported formats are:
     * #RRGGBB #AARRGGBB rgba(RRR, GGG, BBB, A.A);
     * <p>
     * The following names are also accepted:
     * red, blue, green, black, white, gray, cyan, magenta, yellow, lightgray, darkgray, grey,
     * lightgrey, darkgrey, aqua, fuchsia, lime, maroon, navy, olive, purple, silver, and teal.
     */
    public void setBorderColor(String color) {
        borderColor = parseColor(color);
    }

    public int getOverlayColor() {
        return overlayColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    private int parseColor(String color) {
        int fallbackColor = Color.argb(ALPHA_DEFAULT, RED_DEFAULT, GREEN_DEFAULT, BLUE_DEFAULT);
        if (color == null || color.isEmpty()) return fallbackColor;
        Matcher matcher = rgbaPattern.matcher(color);
        try {
            if (matcher.matches()) {
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
                int alpha = alphaGroup != null ? (int) (255.0F * Float.parseFloat(alphaGroup)) : ALPHA_DEFAULT;
                int red = redGroup != null ? Integer.parseInt(redGroup) : RED_DEFAULT;
                int green = greenGroup != null ? Integer.parseInt(greenGroup) : GREEN_DEFAULT;
                int blue = blueGroup != null ? Integer.parseInt(blueGroup) : BLUE_DEFAULT;
                return Color.argb(alpha, red, green, blue);
            } else return Color.parseColor(color);
        } catch (Exception exception) {
            Log.e(TAG, "Unable to parse color, fallback to default " + color);
            return fallbackColor;
        }
    }
}
