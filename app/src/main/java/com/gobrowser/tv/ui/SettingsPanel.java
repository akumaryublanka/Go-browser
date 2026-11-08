package com.gobrowser.tv.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gobrowser.tv.PointerSettings;
import com.gobrowser.tv.R;

/**
 * Full-screen cursor settings. Built from the same primitives as the rest of the
 * app so it reads like a TV screen, not a phone dialog.
 */
public final class SettingsPanel extends FrameLayout {
    public interface Listener {
        void onSettingsChanged();

        void onClose();

        boolean isDesktopMode();

        void onDesktopModeChanged(boolean enabled);

        void onClearHistory();
    }

    private interface StringChanged {
        void onChanged(String value);
    }

    private interface IntChanged {
        void onChanged(int value);
    }

    private interface BooleanChanged {
        void onChanged(boolean value);
    }

    private final Context context;
    private final PointerSettings settings;
    private final Listener listener;
    private LinearLayout column;
    private View firstFocus;

    public SettingsPanel(Context context, PointerSettings settings, Listener listener) {
        super(context);
        this.context = context;
        this.settings = settings;
        this.listener = listener;
        setBackgroundResource(R.drawable.bg_settings);
        setClipToPadding(false);
        build();
    }

    private void build() {
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(160), dp(1000));
        width = Math.max(width, dp(520));

        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, dp(30), 0, dp(28));
        addView(column, new FrameLayout.LayoutParams(width,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        buildHeader();

        View divider = new View(context);
        divider.setBackgroundColor(TvUi.color(context, R.color.line_soft));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(20);
        column.addView(divider, dividerParams);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        column.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(20));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSection(content, "Browsing");
        addToggle(content, "Desktop site",
                "Ask websites for their full desktop layout", listener.isDesktopMode(),
                value -> listener.onDesktopModeChanged(value));
        addActionRow(content, "Clear browsing history",
                "Removes recent pages from the home screen", R.drawable.ic_trash,
                () -> {
                    listener.onClearHistory();
                    android.widget.Toast.makeText(context,
                            "Browsing history cleared", android.widget.Toast.LENGTH_SHORT).show();
                });

        addSection(content, "Pointer");
        addSegmented(content, "Control mode",
                new String[]{"Smart", "Trackpad", "D-pad", "Off"},
                new String[]{PointerSettings.MODE_SMART, PointerSettings.MODE_TRACKPAD,
                        PointerSettings.MODE_DPAD, PointerSettings.MODE_DISABLED},
                settings.mode,
                value -> {
                    settings.mode = value;
                    persist();
                });

        addSlider(content, "Movement speed", 8, 48, settings.speed, " dp", value -> {
            settings.speed = value;
            persist();
        });

        addSlider(content, "Acceleration", 0, 40, settings.acceleration, "%", value -> {
            settings.acceleration = value;
            persist();
        });

        addSlider(content, "Cursor size", 20, 48, settings.size, " dp", value -> {
            settings.size = value;
            persist();
        });

        addSection(content, "Appearance");
        addSegmented(content, "Cursor shape",
                new String[]{"Arrow", "Ring", "Crosshair"},
                new String[]{PointerSettings.SHAPE_ARROW, PointerSettings.SHAPE_RING,
                        PointerSettings.SHAPE_CROSSHAIR},
                settings.shape,
                value -> {
                    settings.shape = value;
                    persist();
                });
        addColors(content);

        addSection(content, "Feedback");
        addToggle(content, "High-contrast outline", "Keeps the cursor readable on busy pages",
                settings.outline, value -> {
                    settings.outline = value;
                    persist();
                });
        addToggle(content, "Soft shadow", "Lifts the cursor off the page",
                settings.shadow, value -> {
                    settings.shadow = value;
                    persist();
                });
        addToggle(content, "Click ripple", "Shows a ring where you press OK",
                settings.clickFeedback, value -> {
                    settings.clickFeedback = value;
                    persist();
                });
        addToggle(content, "Show while browsing", "Hide it when you want a clean screen",
                settings.visible, value -> {
                    settings.visible = value;
                    persist();
                });

        buildFooter();
    }

    private void buildHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = TvUi.iconButton(context, R.drawable.ic_back, "Back");
        back.setOnClickListener(v -> listener.onClose());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(18), 0, 0, 0);

        TextView title = TvUi.text(context, "Settings", 28,
                TvUi.color(context, R.color.cream), true);
        titles.addView(title);

        TextView subtitle = TvUi.text(context, "Tune the cursor and the feel of browsing.", 14,
                TvUi.color(context, R.color.cream_dim), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(4);
        titles.addView(subtitle, subtitleParams);

        header.addView(titles, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(context);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView meta = TvUi.text(context, "GO BROWSER  ·  0.4.0", 11,
                TvUi.color(context, R.color.cream_muted), true);
        meta.setLetterSpacing(0.12f);
        header.addView(meta);

        column.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void buildFooter() {
        View divider = new View(context);
        divider.setBackgroundColor(TvUi.color(context, R.color.line_soft));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(12);
        column.addView(divider, dividerParams);

        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(18);
        column.addView(footer, footerParams);

        TextView reset = TvUi.text(context, "Reset defaults", 14,
                TvUi.color(context, R.color.cream_dim), true);
        reset.setGravity(Gravity.CENTER);
        reset.setFocusable(true);
        reset.setClickable(true);
        reset.setPadding(dp(20), 0, dp(20), 0);
        reset.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 12));
        TvUi.attachFocusScale(reset, 1.03f);
        reset.setOnClickListener(v -> {
            settings.reset();
            settings.save(context);
            listener.onSettingsChanged();
            rebuildAfterReset();
        });
        footer.addView(reset, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));

        View spacer = new View(context);
        footer.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView done = TvUi.text(context, "Done", 15,
                TvUi.color(context, R.color.amber_ink), true);
        done.setGravity(Gravity.CENTER);
        done.setFocusable(true);
        done.setClickable(true);
        done.setPadding(dp(28), 0, dp(28), 0);
        done.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.amber),
                TvUi.color(context, R.color.amber_soft),
                TvUi.color(context, R.color.cream), 14));
        TvUi.attachFocusScale(done, 1.03f);
        done.setOnClickListener(v -> listener.onClose());
        footer.addView(done, new LinearLayout.LayoutParams(dp(160), dp(52)));
    }

    private void addSection(LinearLayout parent, String title) {
        TextView label = TvUi.label(context, title, 11,
                TvUi.color(context, R.color.amber));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(26);
        params.bottomMargin = dp(10);
        parent.addView(label, params);
    }

    private void addFieldLabel(LinearLayout parent, String title) {
        TextView label = TvUi.text(context, title, 14,
                TvUi.color(context, R.color.cream_dim), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        params.bottomMargin = dp(8);
        parent.addView(label, params);
    }

    private void addSegmented(LinearLayout parent, String title, String[] labels,
                              String[] values, String current, StringChanged callback) {
        addFieldLabel(parent, title);
        SegmentedControl control = new SegmentedControl(labels, values, current, callback);
        parent.addView(control, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (firstFocus == null) firstFocus = control.firstOption();
    }

    private void addSlider(LinearLayout parent, String title, int min, int max, int initial,
                           String suffix, IntChanged callback) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.topMargin = dp(20);
        parent.addView(header, headerParams);

        TextView label = TvUi.text(context, title, 14,
                TvUi.color(context, R.color.cream_dim), true);
        header.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView valueText = TvUi.text(context, initial + suffix, 13,
                TvUi.color(context, R.color.amber), true);
        valueText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(valueText);

        SeekBar bar = new SeekBar(context);
        bar.setFocusable(true);
        bar.setMax(max - min);
        bar.setProgress(initial - min);
        bar.setProgressTintList(ColorStateList.valueOf(
                TvUi.color(context, R.color.amber)));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(
                TvUi.color(context, R.color.line)));
        bar.setThumbTintList(ColorStateList.valueOf(
                TvUi.color(context, R.color.amber)));
        bar.setPadding(dp(16), dp(6), dp(16), dp(6));
        bar.setBackground(TvUi.focusBackground(context, Color.TRANSPARENT,
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 10));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        barParams.topMargin = dp(2);
        parent.addView(bar, barParams);

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = min + progress;
                valueText.setText(newValue + suffix);
                if (fromUser) callback.onChanged(newValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void addColors(LinearLayout parent) {
        addFieldLabel(parent, "Cursor colour");

        final String[] names = {"Amber", "White", "Sky", "Green", "Coral"};
        final String[] values = {PointerSettings.COLOR_AMBER, PointerSettings.COLOR_WHITE,
                PointerSettings.COLOR_SKY, PointerSettings.COLOR_GREEN,
                PointerSettings.COLOR_CORAL};
        final int[] colors = {
                TvUi.color(context, R.color.amber),
                TvUi.color(context, R.color.cream),
                TvUi.color(context, R.color.sky),
                TvUi.color(context, R.color.green),
                TvUi.color(context, R.color.coral)
        };

        final View[] dots = new View[values.length];
        final TextView[] labels = new TextView[values.length];
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < values.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setFocusable(true);
            item.setClickable(true);
            item.setPadding(dp(8), dp(10), dp(8), dp(8));
            item.setBackground(TvUi.focusBackground(context, Color.TRANSPARENT,
                    TvUi.color(context, R.color.surface_focus),
                    TvUi.color(context, R.color.amber), 12));
            TvUi.attachFocusScale(item, 1.04f);

            View dot = new View(context);
            item.addView(dot, new LinearLayout.LayoutParams(dp(34), dp(34)));

            TextView name = TvUi.text(context, names[i], 12,
                    TvUi.color(context, R.color.cream_muted), false);
            name.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = dp(8);
            item.addView(name, nameParams);

            item.setOnClickListener(v -> {
                settings.color = values[index];
                persist();
                updateSwatches(dots, labels, values, colors);
            });

            row.addView(item, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            dots[i] = dot;
            labels[i] = name;
        }

        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        updateSwatches(dots, labels, values, colors);
    }

    private void updateSwatches(View[] dots, TextView[] labels, String[] values, int[] colors) {
        for (int i = 0; i < values.length; i++) {
            boolean chosen = values[i].equals(settings.color);
            int stroke = chosen
                    ? (PointerSettings.COLOR_WHITE.equals(values[i])
                    ? TvUi.color(context, R.color.amber)
                    : TvUi.color(context, R.color.cream))
                    : TvUi.color(context, R.color.line);
            dots[i].setBackground(TvUi.oval(context, colors[i], stroke, chosen ? 3f : 1f));
            labels[i].setTextColor(chosen
                    ? TvUi.color(context, R.color.cream)
                    : TvUi.color(context, R.color.cream_muted));
        }
    }

    private void addActionRow(LinearLayout parent, String title, String hint, int iconRes,
                              Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(14), 0);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 12));
        TvUi.attachFocusScale(row, 1.01f);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(TvUi.color(context, R.color.cream_dim));
        icon.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.rightMargin = dp(14);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(VERTICAL);

        TextView label = TvUi.text(context, title, 15,
                TvUi.color(context, R.color.cream), true);
        copy.addView(label);

        TextView detail = TvUi.text(context, hint, 12,
                TvUi.color(context, R.color.cream_muted), false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(3);
        copy.addView(detail, detailParams);

        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_forward);
        chevron.setColorFilter(TvUi.color(context, R.color.cream_muted));
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(20), dp(20)));

        row.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        rowParams.topMargin = dp(8);
        parent.addView(row, rowParams);
    }

    private void addToggle(LinearLayout parent, String title, String hint, boolean initial,
                           BooleanChanged callback) {
        final boolean[] value = {initial};

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(14), 0);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 12));

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView label = TvUi.text(context, title, 15,
                TvUi.color(context, R.color.cream), true);
        copy.addView(label);

        TextView detail = TvUi.text(context, hint, 12,
                TvUi.color(context, R.color.cream_muted), false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(3);
        copy.addView(detail, detailParams);

        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView badge = TvUi.text(context, initial ? "ON" : "OFF", 11,
                TvUi.color(context, initial ? R.color.amber_ink : R.color.cream_muted), true);
        badge.setLetterSpacing(0.1f);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), dp(5), dp(12), dp(5));
        updateToggleBadge(badge, initial);
        row.addView(badge);

        row.setOnClickListener(v -> {
            value[0] = !value[0];
            updateToggleBadge(badge, value[0]);
            callback.onChanged(value[0]);
        });

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        rowParams.topMargin = dp(8);
        parent.addView(row, rowParams);
    }

    private void updateToggleBadge(TextView badge, boolean on) {
        badge.setText(on ? "ON" : "OFF");
        badge.setTextColor(TvUi.color(context,
                on ? R.color.amber_ink : R.color.cream_muted));
        badge.setBackground(on
                ? TvUi.rounded(context, TvUi.color(context, R.color.amber), 7)
                : TvUi.stroked(context, TvUi.color(context, R.color.surface_focus), 7,
                TvUi.color(context, R.color.line), 1));
    }

    private void persist() {
        settings.save(context);
        listener.onSettingsChanged();
    }

    private void rebuildAfterReset() {
        removeAllViews();
        firstFocus = null;
        build();
        requestInitialFocus();
    }

    public void requestInitialFocus() {
        if (firstFocus != null) {
            firstFocus.post(() -> firstFocus.requestFocus());
        }
    }

    private final class SegmentedControl extends LinearLayout {
        private final String[] values;
        private final TextView[] options;
        private final StringChanged callback;
        private String selected;

        SegmentedControl(String[] labels, String[] values, String selected,
                         StringChanged callback) {
            super(context);
            this.values = values;
            this.selected = selected;
            this.callback = callback;
            this.options = new TextView[labels.length];

            setOrientation(HORIZONTAL);
            setBackground(TvUi.rounded(context, TvUi.color(context, R.color.surface), 15));
            setPadding(dp(4), dp(4), dp(4), dp(4));

            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                TextView option = TvUi.text(context, labels[i], 14,
                        TvUi.color(context, R.color.cream_dim), true);
                option.setGravity(Gravity.CENTER);
                option.setSingleLine(true);
                option.setFocusable(true);
                option.setClickable(true);
                option.setOnClickListener(v -> select(values[index]));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
                if (i < labels.length - 1) params.rightMargin = dp(4);
                addView(option, params);
                options[i] = option;
            }

            updateStyles();
        }

        private void select(String value) {
            if (value.equals(selected)) return;
            selected = value;
            updateStyles();
            callback.onChanged(value);
        }

        private void updateStyles() {
            for (int i = 0; i < options.length; i++) {
                boolean isSelected = values[i].equals(selected);
                TextView option = options[i];
                option.setTextColor(TvUi.color(context, isSelected
                        ? R.color.amber_ink : R.color.cream_dim));
                option.setBackground(isSelected
                        ? TvUi.focusBackground(context,
                        TvUi.color(context, R.color.amber),
                        TvUi.color(context, R.color.amber_soft),
                        TvUi.color(context, R.color.cream), 11)
                        : TvUi.focusBackground(context, Color.TRANSPARENT,
                        TvUi.color(context, R.color.surface_focus),
                        TvUi.color(context, R.color.amber), 11));
            }
        }

        View firstOption() {
            return options[0];
        }
    }

    private int dp(float value) {
        return TvUi.dp(context, value);
    }
}
