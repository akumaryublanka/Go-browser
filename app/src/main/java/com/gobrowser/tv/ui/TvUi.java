package com.gobrowser.tv.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.gobrowser.tv.R;

/**
 * Shared visual language for the app: spacing, type, focus states and shapes.
 * Every screen goes through here so the app keeps one voice.
 */
public final class TvUi {
    private static final String TYPEFACE = "sans-serif";
    private static final String TYPEFACE_MEDIUM = "sans-serif-medium";

    private TvUi() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(Context context, int colorRes) {
        return context.getColor(colorRes);
    }

    public static TextView text(Context context, CharSequence value, float sizeSp,
                                int color, boolean medium) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(medium ? TYPEFACE_MEDIUM : TYPEFACE, Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    public static TextView label(Context context, CharSequence value, float sizeSp, int color) {
        TextView view = text(context, value, sizeSp, color, true);
        view.setLetterSpacing(0.16f);
        view.setAllCaps(true);
        return view;
    }

    public static ImageView iconButton(Context context, int drawableRes, String description) {
        ImageView button = new ImageView(context);
        button.setImageResource(drawableRes);
        button.setColorFilter(color(context, R.color.cream));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        button.setFocusable(true);
        button.setClickable(true);
        button.setContentDescription(description);
        button.setBackground(focusBackground(context,
                color(context, R.color.surface),
                color(context, R.color.surface_focus),
                color(context, R.color.amber), 14f));
        attachFocusScale(button, 1.08f);
        return button;
    }

    public static GradientDrawable rounded(Context context, int fill, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, radiusDp));
        return shape;
    }

    public static GradientDrawable stroked(Context context, int fill, float radiusDp,
                                           int strokeColor, float strokeDp) {
        GradientDrawable shape = rounded(context, fill, radiusDp);
        shape.setStroke(dp(context, strokeDp), strokeColor);
        return shape;
    }

    public static GradientDrawable oval(Context context, int fill, int strokeColor, float strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(fill);
        shape.setStroke(dp(context, strokeDp), strokeColor);
        return shape;
    }

    public static StateListDrawable focusBackground(Context context, int normalFill, int focusFill,
                                                    int focusStroke, float radiusDp) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                stroked(context, focusFill, radiusDp, focusStroke, 2f));
        states.addState(new int[]{android.R.attr.state_pressed},
                stroked(context, focusFill, radiusDp, focusStroke, 2f));
        states.addState(new int[]{}, rounded(context, normalFill, radiusDp));
        return states;
    }

    public static void attachFocusScale(final View view, final float scale) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().cancel();
            v.animate()
                    .scaleX(hasFocus ? scale : 1f)
                    .scaleY(hasFocus ? scale : 1f)
                    .setDuration(hasFocus ? 150 : 110)
                    .start();
        });
    }

    public static void attachFocusScale(final View view, final float scale,
                                        final View.OnFocusChangeListener extra) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().cancel();
            v.animate()
                    .scaleX(hasFocus ? scale : 1f)
                    .scaleY(hasFocus ? scale : 1f)
                    .setDuration(hasFocus ? 150 : 110)
                    .start();
            if (extra != null) extra.onFocusChange(v, hasFocus);
        });
    }

    public static ColorStateList tint(int color) {
        return ColorStateList.valueOf(color);
    }

    /** Full-screen dim layer used behind tab and settings overlays. */
    public static View scrim(Context context, int alpha) {
        View view = new View(context);
        view.setBackgroundColor((alpha << 24) | 0x000000);
        view.setClickable(true);
        view.setFocusable(false);
        return view;
    }

    public static void showKeyboard(final View view) {
        if (view == null) return;
        view.requestFocus();
        view.postDelayed(() -> {
            try {
                InputMethodManager manager = (InputMethodManager)
                        view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (manager != null) {
                    // SHOW_FORCED is important on Android TV: many TV keyboards
                    // ignore SHOW_IMPLICIT because the view is not in touch mode.
                    manager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
                }
            } catch (Exception ignored) {
                // A TV without a software keyboard is a normal case.
            }
        }, 80L);
    }

    public static void hideKeyboard(View view) {
        try {
            InputMethodManager manager = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        } catch (Exception ignored) {
            // Ignore missing IME services.
        }
    }
}
