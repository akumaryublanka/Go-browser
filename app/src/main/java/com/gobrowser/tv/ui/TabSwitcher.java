package com.gobrowser.tv.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gobrowser.tv.R;

import java.util.List;

/**
 * Full-screen tab switcher. Tabs are represented by their last URL, so the
 * switcher stays cheap on low-memory TV boxes.
 */
public final class TabSwitcher extends FrameLayout {
    public interface Listener {
        void onSelect(int index);

        void onNewTab();

        void onCloseTab(int index);

        void onDismiss();
    }

    public static final class Item {
        public final String title;
        public final String url;
        public final boolean active;

        public Item(String title, String url, boolean active) {
            this.title = title;
            this.url = url;
            this.active = active;
        }
    }

    private final Context context;
    private final Listener listener;
    private LinearLayout panel;
    private LinearLayout list;
    private TextView countLabel;
    private View firstFocus;

    public TabSwitcher(Context context, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        setBackgroundResource(R.drawable.bg_settings);
        setFocusable(true);
        setClickable(true);
        buildShell();
    }

    private void buildShell() {
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(200), dp(980));
        width = Math.max(width, dp(640));

        panel = new LinearLayout(context);
        panel.setOrientation(VERTICAL);
        panel.setPadding(dp(28), dp(24), dp(28), dp(24));
        addView(panel, new FrameLayout.LayoutParams(width,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
    }

    public void show(List<Item> items) {
        panel.removeAllViews();
        firstFocus = null;

        buildHeader(items.size());

        View divider = new View(context);
        divider.setBackgroundColor(TvUi.color(context, R.color.line_soft));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(18);
        panel.addView(divider, dividerParams);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        list = new LinearLayout(context);
        list.setOrientation(VERTICAL);
        list.setPadding(0, dp(16), 0, dp(10));
        scroll.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < items.size(); i++) {
            View row = buildRow(items.get(i), i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
            params.bottomMargin = dp(10);
            list.addView(row, params);
            if (firstFocus == null) firstFocus = row;
        }

        post(() -> {
            if (firstFocus != null) firstFocus.requestFocus();
        });
    }

    private void buildHeader(int count) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView close = TvUi.iconButton(context, R.drawable.ic_close, "Close tabs");
        close.setOnClickListener(v -> listener.onDismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(VERTICAL);
        titles.setPadding(dp(16), 0, 0, 0);

        TextView title = TvUi.text(context, "Tabs", 26,
                TvUi.color(context, R.color.cream), true);
        titles.addView(title);

        countLabel = TvUi.text(context, count + (count == 1 ? " open page" : " open pages"), 13,
                TvUi.color(context, R.color.cream_dim), false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countParams.topMargin = dp(3);
        titles.addView(countLabel, countParams);

        header.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView newTab = TvUi.text(context, "+  New tab", 14,
                TvUi.color(context, R.color.amber_ink), true);
        newTab.setGravity(Gravity.CENTER);
        newTab.setFocusable(true);
        newTab.setClickable(true);
        newTab.setPadding(dp(20), 0, dp(20), 0);
        newTab.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.amber),
                TvUi.color(context, R.color.amber_soft),
                TvUi.color(context, R.color.cream), 14));
        TvUi.attachFocusScale(newTab, 1.04f);
        newTab.setOnClickListener(v -> listener.onNewTab());
        header.addView(newTab, new LinearLayout.LayoutParams(dp(160), dp(52)));

        panel.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View buildRow(Item item, int index) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), 0, dp(12), 0);
        row.setFocusable(true);
        row.setClickable(true);
        row.setContentDescription(item.title);
        row.setBackground(item.active
                ? TvUi.stroked(context, TvUi.color(context, R.color.surface_focus), 15,
                TvUi.color(context, R.color.amber), 2)
                : TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 15));
        TvUi.attachFocusScale(row, 1.015f);
        row.setOnClickListener(v -> listener.onSelect(index));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_globe);
        icon.setColorFilter(TvUi.color(context, item.active
                ? R.color.amber : R.color.cream_muted));
        icon.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(26), dp(26));
        iconParams.rightMargin = dp(16);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(VERTICAL);

        TextView title = TvUi.text(context, emptyTitle(item), 16,
                TvUi.color(context, R.color.cream), true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(title);

        TextView url = TvUi.text(context, displayUrl(item.url), 12,
                TvUi.color(context, R.color.cream_muted), false);
        url.setSingleLine(true);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        urlParams.topMargin = dp(3);
        copy.addView(url, urlParams);

        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (item.active) {
            TextView badge = TvUi.text(context, "CURRENT", 10,
                    TvUi.color(context, R.color.amber_ink), true);
            badge.setLetterSpacing(0.1f);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(10), dp(5), dp(10), dp(5));
            badge.setBackground(TvUi.rounded(context,
                    TvUi.color(context, R.color.amber), 7));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeParams.rightMargin = dp(12);
            row.addView(badge, badgeParams);
        }

        ImageView close = new ImageView(context);
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(TvUi.color(context, R.color.cream_muted));
        close.setScaleType(ImageView.ScaleType.CENTER);
        close.setFocusable(true);
        close.setClickable(true);
        close.setContentDescription("Close tab");
        close.setPadding(dp(10), dp(10), dp(10), dp(10));
        close.setBackground(TvUi.focusBackground(context, android.graphics.Color.TRANSPARENT,
                TvUi.color(context, R.color.coral),
                TvUi.color(context, R.color.cream), 10));
        TvUi.attachFocusScale(close, 1.08f);
        close.setOnClickListener(v -> listener.onCloseTab(index));
        row.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));

        return row;
    }

    private String emptyTitle(Item item) {
        if (item.url == null || item.url.isEmpty()) return "New tab";
        String title = item.title == null ? "" : item.title.trim();
        return title.isEmpty() ? "Untitled page" : title;
    }

    private String displayUrl(String url) {
        if (url == null || url.isEmpty()) return "GO Browser home";
        return url;
    }

    private int dp(float value) {
        return TvUi.dp(context, value);
    }
}
