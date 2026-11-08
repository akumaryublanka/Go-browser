package com.gobrowser.tv.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gobrowser.tv.R;

/**
 * Bottom action bar shown when the remote's MENU key is pressed. It gives every
 * browser action a large, D-pad-friendly target even while the smart pointer is
 * owning the arrow keys.
 */
public final class QuickBar extends LinearLayout {
    public interface Listener {
        void onBack();

        void onForward();

        void onReload();

        void onHome();

        void onSearch();

        void onTabs();

        void onBookmark();

        void onSettings();
    }

    private final Context context;
    private final Listener listener;
    private Action back;
    private Action forward;
    private Action bookmark;

    public QuickBar(Context context, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface_raised),
                TvUi.color(context, R.color.surface_raised),
                TvUi.color(context, R.color.line), 20));
        build();
    }

    private void build() {
        back = addAction(R.drawable.ic_back, "Back", v -> listener.onBack());
        forward = addAction(R.drawable.ic_forward, "Forward", v -> listener.onForward());
        addAction(R.drawable.ic_reload, "Reload", v -> listener.onReload());
        addAction(R.drawable.ic_home, "Home", v -> listener.onHome());
        addAction(R.drawable.ic_search, "Search", v -> listener.onSearch());
        addAction(R.drawable.ic_tabs, "Tabs", v -> listener.onTabs());
        bookmark = addAction(R.drawable.ic_star_border, "Save", v -> listener.onBookmark());
        addAction(R.drawable.ic_settings, "Settings", v -> listener.onSettings());
    }

    private Action addAction(int iconRes, String label, OnClickListener click) {
        Action action = new Action(iconRes, label);
        action.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(82), dp(70));
        params.rightMargin = dp(6);
        addView(action, params);
        return action;
    }

    public void setState(boolean canBack, boolean canForward,
                         boolean bookmarked, int tabCount) {
        back.setAvailable(canBack);
        forward.setAvailable(canForward);
        bookmark.setIcon(bookmarked ? R.drawable.ic_star : R.drawable.ic_star_border);
        bookmark.setLabel(bookmarked ? "Saved" : "Save");
        bookmark.setMarked(bookmarked);
    }

    public void requestInitialFocus() {
        post(() -> {
            if (back.isFocusable()) {
                back.requestFocus();
            } else if (forward.isFocusable()) {
                forward.requestFocus();
            } else {
                getChildAt(2).requestFocus();
            }
        });
    }

    private int dp(float value) {
        return TvUi.dp(context, value);
    }

    private final class Action extends LinearLayout {
        private final ImageView icon;
        private final TextView label;
        private boolean marked;

        Action(int iconRes, String text) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setFocusable(true);
            setClickable(true);
            setBackground(TvUi.focusBackground(context, android.graphics.Color.TRANSPARENT,
                    TvUi.color(context, R.color.surface_focus),
                    TvUi.color(context, R.color.amber), 14));
            TvUi.attachFocusScale(this, 1.06f);
            setPadding(dp(4), dp(6), dp(4), dp(6));

            icon = new ImageView(context);
            icon.setImageResource(iconRes);
            icon.setColorFilter(TvUi.color(context, R.color.cream));
            icon.setScaleType(ImageView.ScaleType.CENTER);
            addView(icon, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

            label = TvUi.text(context, text, 11,
                    TvUi.color(context, R.color.cream_dim), true);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void setAvailable(boolean available) {
            setEnabled(available);
            setFocusable(available);
            setAlpha(available ? 1f : 0.34f);
        }

        void setIcon(int res) {
            icon.setImageResource(res);
        }

        void setLabel(String text) {
            label.setText(text);
        }

        void setMarked(boolean value) {
            marked = value;
            icon.setColorFilter(TvUi.color(context,
                    value ? R.color.amber : R.color.cream));
            label.setTextColor(TvUi.color(context,
                    value ? R.color.amber : R.color.cream_dim));
        }
    }
}
