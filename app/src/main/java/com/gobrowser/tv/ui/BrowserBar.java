package com.gobrowser.tv.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gobrowser.tv.R;

/**
 * Compact browser chrome. Navigation on the left, address in the middle,
 * tabs / bookmark / pointer / settings on the right.
 */
public final class BrowserBar extends LinearLayout {
    public interface Listener {
        void onBack();

        void onForward();

        void onHome();

        void onReload();

        void onCycleEngine();

        void onTogglePointer();

        void onOpenSettings();

        void onToggleBookmark();

        void onOpenTabs();

        void onSubmit(String input);
    }

    private final Context context;
    private final Listener listener;
    private EditText addressInput;
    private TextView engineLabel;
    private ImageView pointerButton;
    private ImageView bookmarkButton;
    private TextView tabBadge;
    private LinearLayout addressPill;

    public BrowserBar(Context context, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(TvUi.color(context, R.color.ink));
        setPadding(dp(32), dp(10), dp(32), dp(10));
        build();
    }

    private void build() {
        ImageView back = TvUi.iconButton(context, R.drawable.ic_back, "Back");
        back.setOnClickListener(v -> listener.onBack());
        addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        ImageView forward = TvUi.iconButton(context, R.drawable.ic_forward, "Forward");
        forward.setOnClickListener(v -> listener.onForward());
        LinearLayout.LayoutParams forwardParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        forwardParams.leftMargin = dp(4);
        addView(forward, forwardParams);

        ImageView home = TvUi.iconButton(context, R.drawable.ic_home, "Home");
        home.setOnClickListener(v -> listener.onHome());
        LinearLayout.LayoutParams homeParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        homeParams.leftMargin = dp(4);
        addView(home, homeParams);

        View gap = new View(context);
        addView(gap, new LinearLayout.LayoutParams(dp(12), 1));

        buildAddressPill();

        View gap2 = new View(context);
        addView(gap2, new LinearLayout.LayoutParams(dp(10), 1));

        addView(buildTabsButton(), new LinearLayout.LayoutParams(dp(48), dp(48)));

        bookmarkButton = TvUi.iconButton(context, R.drawable.ic_star_border, "Bookmark");
        bookmarkButton.setOnClickListener(v -> listener.onToggleBookmark());
        LinearLayout.LayoutParams bookmarkParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        bookmarkParams.leftMargin = dp(4);
        addView(bookmarkButton, bookmarkParams);

        pointerButton = TvUi.iconButton(context, R.drawable.ic_pointer, "Turn pointer on");
        pointerButton.setOnClickListener(v -> listener.onTogglePointer());
        LinearLayout.LayoutParams pointerParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        pointerParams.leftMargin = dp(4);
        addView(pointerButton, pointerParams);

        ImageView settings = TvUi.iconButton(context, R.drawable.ic_settings, "Settings");
        settings.setOnClickListener(v -> listener.onOpenSettings());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        settingsParams.leftMargin = dp(4);
        addView(settings, settingsParams);
    }

    private View buildTabsButton() {
        FrameLayout holder = new FrameLayout(context);
        ImageView tabs = TvUi.iconButton(context, R.drawable.ic_tabs, "Tabs");
        tabs.setOnClickListener(v -> listener.onOpenTabs());
        holder.addView(tabs, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        tabBadge = TvUi.text(context, "1", 10,
                TvUi.color(context, R.color.amber_ink), true);
        tabBadge.setGravity(Gravity.CENTER);
        tabBadge.setBackground(TvUi.oval(context,
                TvUi.color(context, R.color.amber), Color.TRANSPARENT, 0f));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                dp(18), dp(18), Gravity.TOP | Gravity.END);
        badgeParams.topMargin = dp(2);
        badgeParams.rightMargin = dp(0);
        holder.addView(tabBadge, badgeParams);
        return holder;
    }

    private void buildAddressPill() {
        addressPill = new LinearLayout(context);
        addressPill.setOrientation(HORIZONTAL);
        addressPill.setGravity(Gravity.CENTER_VERTICAL);
        addressPill.setPadding(dp(18), 0, dp(8), 0);
        addressPill.setBackground(TvUi.stroked(context,
                TvUi.color(context, R.color.surface), 16,
                TvUi.color(context, R.color.line), 1));
        addView(addressPill, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(TvUi.color(context, R.color.cream_muted));
        searchIcon.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconParams.rightMargin = dp(12);
        addressPill.addView(searchIcon, iconParams);

        addressInput = new EditText(context);
        addressInput.setSingleLine(true);
        addressInput.setTextSize(16);
        addressInput.setTextColor(TvUi.color(context, R.color.cream));
        addressInput.setHintTextColor(TvUi.color(context, R.color.cream_muted));
        addressInput.setHint("Search or type a website");
        addressInput.setBackground(null);
        addressInput.setPadding(0, 0, 0, 0);
        addressInput.setIncludeFontPadding(false);
        addressInput.setGravity(Gravity.CENTER_VERTICAL);
        addressInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressInput.setSelectAllOnFocus(true);
        addressInput.setOnFocusChangeListener((v, hasFocus) -> {
            addressPill.setBackground(hasFocus
                    ? TvUi.stroked(context, TvUi.color(context, R.color.surface),
                    16, TvUi.color(context, R.color.amber), 2)
                    : TvUi.stroked(context, TvUi.color(context, R.color.surface),
                    16, TvUi.color(context, R.color.line), 1));
            if (hasFocus) TvUi.showKeyboard(addressInput);
        });
        addressInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                submitAddress();
                return true;
            }
            return false;
        });
        addressPill.addView(addressInput, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout engineChip = new LinearLayout(context);
        engineChip.setOrientation(HORIZONTAL);
        engineChip.setGravity(Gravity.CENTER_VERTICAL);
        engineChip.setFocusable(true);
        engineChip.setClickable(true);
        engineChip.setContentDescription("Change search engine");
        engineChip.setPadding(dp(12), 0, dp(8), 0);
        engineChip.setBackground(TvUi.focusBackground(context, Color.TRANSPARENT,
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 10));
        TvUi.attachFocusScale(engineChip, 1.04f);
        engineChip.setOnClickListener(v -> listener.onCycleEngine());

        engineLabel = TvUi.text(context, "Google", 12,
                TvUi.color(context, R.color.cream_dim), true);
        engineChip.addView(engineLabel);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_chevron_down);
        chevron.setColorFilter(TvUi.color(context, R.color.cream_muted));
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(14), dp(14));
        chevronParams.leftMargin = dp(4);
        engineChip.addView(chevron, chevronParams);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        chipParams.rightMargin = dp(6);
        addressPill.addView(engineChip, chipParams);

        ImageView reload = new ImageView(context);
        reload.setImageResource(R.drawable.ic_reload);
        reload.setColorFilter(TvUi.color(context, R.color.cream_dim));
        reload.setScaleType(ImageView.ScaleType.CENTER);
        reload.setFocusable(true);
        reload.setClickable(true);
        reload.setContentDescription("Reload");
        reload.setPadding(dp(10), dp(10), dp(10), dp(10));
        reload.setBackground(TvUi.focusBackground(context, Color.TRANSPARENT,
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 10));
        TvUi.attachFocusScale(reload, 1.06f);
        reload.setOnClickListener(v -> listener.onReload());
        addressPill.addView(reload, new LinearLayout.LayoutParams(dp(40), dp(40)));
    }

    public void submitAddress() {
        String value = addressInput.getText().toString().trim();
        if (value.isEmpty()) {
            TvUi.showKeyboard(addressInput);
            return;
        }
        TvUi.hideKeyboard(addressInput);
        listener.onSubmit(value);
    }

    public void focusAddress() {
        if (addressInput == null) return;
        addressInput.requestFocus();
        TvUi.showKeyboard(addressInput);
    }

    public void setUrl(String url) {
        if (addressInput != null && !addressInput.hasFocus()) {
            addressInput.setText(url == null ? "" : url);
        }
    }

    public boolean isAddressFocused() {
        return addressInput != null && addressInput.hasFocus();
    }

    public void clearAddress() {
        if (addressInput != null) {
            addressInput.setText("");
            addressInput.clearFocus();
        }
    }

    public void setEngineName(String name) {
        if (engineLabel != null) engineLabel.setText(name);
    }

    public void setPointerEnabled(boolean enabled) {
        if (pointerButton == null) return;
        pointerButton.setColorFilter(enabled
                ? TvUi.color(context, R.color.amber)
                : TvUi.color(context, R.color.cream));
        pointerButton.setContentDescription(enabled ? "Turn pointer off" : "Turn pointer on");
    }

    public void setBookmarked(boolean bookmarked) {
        if (bookmarkButton == null) return;
        bookmarkButton.setImageResource(bookmarked
                ? R.drawable.ic_star : R.drawable.ic_star_border);
        bookmarkButton.setColorFilter(TvUi.color(context,
                bookmarked ? R.color.amber : R.color.cream));
        bookmarkButton.setContentDescription(bookmarked
                ? "Remove bookmark" : "Add bookmark");
    }

    public void setTabCount(int count) {
        if (tabBadge == null) return;
        tabBadge.setText(String.valueOf(Math.max(1, count)));
    }

    public EditText addressField() {
        return addressInput;
    }

    private int dp(float value) {
        return TvUi.dp(context, value);
    }
}
