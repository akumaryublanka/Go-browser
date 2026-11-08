package com.gobrowser.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Small value object for the remote pointer. Kept separate from the activity so the
 * settings screen can read and write it without knowing about the WebView.
 */
public final class PointerSettings {
    public static final String MODE_SMART = "SMART";
    public static final String MODE_TRACKPAD = "TRACKPAD";
    public static final String MODE_DPAD = "DPAD";
    public static final String MODE_DISABLED = "DISABLED";

    public static final String SHAPE_ARROW = "ARROW";
    public static final String SHAPE_RING = "RING";
    public static final String SHAPE_CROSSHAIR = "CROSSHAIR";

    public static final String COLOR_AMBER = "AMBER";
    public static final String COLOR_WHITE = "WHITE";
    public static final String COLOR_SKY = "SKY";
    public static final String COLOR_GREEN = "GREEN";
    public static final String COLOR_CORAL = "CORAL";

    private static final String PREFS = "go_browser_pointer";

    public String mode = MODE_SMART;
    public String shape = SHAPE_ARROW;
    public String color = COLOR_AMBER;
    public int speed = 22;
    public int acceleration = 18;
    public int size = 30;
    public boolean outline = true;
    public boolean shadow = true;
    public boolean clickFeedback = true;
    public boolean visible = true;

    public static PointerSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        PointerSettings settings = new PointerSettings();
        settings.mode = prefs.getString("mode", MODE_SMART);
        settings.shape = prefs.getString("shape", SHAPE_ARROW);
        settings.color = normalizeColor(prefs.getString("color", COLOR_AMBER));
        settings.speed = prefs.getInt("speed", 22);
        settings.acceleration = prefs.getInt("acceleration", 18);
        settings.size = prefs.getInt("size", 30);
        settings.outline = prefs.getBoolean("outline", true);
        settings.shadow = prefs.getBoolean("shadow", true);
        settings.clickFeedback = prefs.getBoolean("click_feedback", true);
        settings.visible = prefs.getBoolean("visible", true);
        return settings;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("mode", mode)
                .putString("shape", shape)
                .putString("color", color)
                .putInt("speed", speed)
                .putInt("acceleration", acceleration)
                .putInt("size", size)
                .putBoolean("outline", outline)
                .putBoolean("shadow", shadow)
                .putBoolean("click_feedback", clickFeedback)
                .putBoolean("visible", visible)
                .apply();
    }

    public void reset() {
        mode = MODE_SMART;
        shape = SHAPE_ARROW;
        color = COLOR_AMBER;
        speed = 22;
        acceleration = 18;
        size = 30;
        outline = true;
        shadow = true;
        clickFeedback = true;
        visible = true;
    }

    public boolean isPointerEnabled() {
        return MODE_SMART.equals(mode) || MODE_TRACKPAD.equals(mode);
    }

    private static String normalizeColor(String value) {
        if (COLOR_AMBER.equals(value) || COLOR_WHITE.equals(value)
                || COLOR_SKY.equals(value) || COLOR_GREEN.equals(value)
                || COLOR_CORAL.equals(value)) {
            return value;
        }
        if ("CYAN".equals(value)) return COLOR_SKY;
        if ("YELLOW".equals(value) || "ORANGE".equals(value)) return COLOR_AMBER;
        return COLOR_AMBER;
    }

    public int colorInt() {
        if (COLOR_WHITE.equals(color)) return Color.WHITE;
        if (COLOR_SKY.equals(color)) return Color.rgb(87, 182, 217);
        if (COLOR_GREEN.equals(color)) return Color.rgb(70, 192, 138);
        if (COLOR_CORAL.equals(color)) return Color.rgb(224, 89, 63);
        // Legacy names kept so an existing install keeps its chosen colour.
        if ("CYAN".equals(color)) return Color.rgb(25, 215, 232);
        if ("YELLOW".equals(color)) return Color.rgb(255, 220, 55);
        if ("ORANGE".equals(color)) return Color.rgb(255, 153, 54);
        return Color.rgb(240, 163, 58);
    }
}
