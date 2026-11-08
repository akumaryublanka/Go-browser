package com.gobrowser.tv.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gobrowser.tv.BrowserStore;
import com.gobrowser.tv.R;

import java.util.List;

/**
 * The start screen: search, recent pages, a small set of shortcuts and saved
 * bookmarks. Everything is reachable with a D-pad and every target is sized for
 * a 10-foot viewing distance.
 */
public final class HomeView extends FrameLayout {
    public interface Listener {
        void onSubmit(String input);

        void onOpen(String url);

        void onCycleEngine();

        void onOpenSettings();
    }

    private static final class Shortcut {
        final String name;
        final String domain;
        final String url;
        final int iconRes;
        final int colorRes;
        final boolean darkIcon;

        Shortcut(String name, String domain, String url, int iconRes,
                 int colorRes, boolean darkIcon) {
            this.name = name;
            this.domain = domain;
            this.url = url;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
            this.darkIcon = darkIcon;
        }
    }

    private final Context context;
    private final BrowserStore store;
    private final Listener listener;
    private EditText searchInput;
    private TextView engineLabel;
    private LinearLayout searchPill;
    private LinearLayout recentSection;
    private LinearLayout bookmarkSection;
    private boolean suppressKeyboard;

    public HomeView(Context context, BrowserStore store, Listener listener) {
        super(context);
        this.context = context;
        this.store = store;
        this.listener = listener;
        setBackgroundResource(R.drawable.bg_home);
        setClipToPadding(false);
        build();
    }

    private void build() {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(56), dp(24), dp(56), dp(20));
        addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildHeader(page);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(22), 0, dp(18));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buildHero(content);
        buildSearch(content);
        buildShortcuts(content);

        recentSection = new LinearLayout(context);
        recentSection.setOrientation(LinearLayout.VERTICAL);
        content.addView(recentSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bookmarkSection = new LinearLayout(context);
        bookmarkSection.setOrientation(LinearLayout.VERTICAL);
        content.addView(bookmarkSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buildFooter(page);
        refreshLists();
    }

    private void buildHeader(LinearLayout page) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = TvUi.text(context, "GO", 17,
                TvUi.color(context, R.color.amber_ink), true);
        mark.setGravity(Gravity.CENTER);
        mark.setLetterSpacing(0.04f);
        mark.setBackground(TvUi.rounded(context, TvUi.color(context, R.color.amber), 13));
        header.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout brand = new LinearLayout(context);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(14), 0, 0, 0);

        TextView name = TvUi.text(context, "GO BROWSER", 15,
                TvUi.color(context, R.color.cream), true);
        name.setLetterSpacing(0.18f);
        brand.addView(name);

        TextView tag = TvUi.text(context, "ANDROID TV", 10,
                TvUi.color(context, R.color.cream_muted), true);
        tag.setLetterSpacing(0.28f);
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagParams.topMargin = dp(3);
        brand.addView(tag, tagParams);

        header.addView(brand, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(context);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        ImageView settings = TvUi.iconButton(context, R.drawable.ic_settings, "Settings");
        settings.setOnClickListener(v -> listener.onOpenSettings());
        header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));

        page.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
    }

    private void buildHero(LinearLayout content) {
        TextView eyebrow = TvUi.label(context, "The open web, on your TV", 12,
                TvUi.color(context, R.color.amber));
        content.addView(eyebrow);

        TextView headline = TvUi.text(context, "The whole web,\nfrom your couch.", 36,
                TvUi.color(context, R.color.cream), true);
        headline.setLineSpacing(0, 0.94f);
        LinearLayout.LayoutParams headlineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headlineParams.topMargin = dp(10);
        content.addView(headline, headlineParams);

        TextView subtitle = TvUi.text(context,
                "Search, browse and stream \u2014 built for a remote, not a mouse.", 15,
                TvUi.color(context, R.color.cream_dim), false);
        subtitle.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(8);
        content.addView(subtitle, subtitleParams);
    }

    private void buildSearch(LinearLayout content) {
        searchPill = new LinearLayout(context);
        searchPill.setOrientation(LinearLayout.HORIZONTAL);
        searchPill.setGravity(Gravity.CENTER_VERTICAL);
        searchPill.setPadding(dp(22), 0, dp(10), 0);
        searchPill.setBackground(TvUi.stroked(context,
                TvUi.color(context, R.color.surface_raised), 20,
                TvUi.color(context, R.color.line), 1));
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        pillParams.topMargin = dp(20);
        content.addView(searchPill, pillParams);

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(TvUi.color(context, R.color.cream_muted));
        searchIcon.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.rightMargin = dp(16);
        searchPill.addView(searchIcon, iconParams);

        searchInput = new EditText(context);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(18);
        searchInput.setTextColor(TvUi.color(context, R.color.cream));
        searchInput.setHintTextColor(TvUi.color(context, R.color.cream_muted));
        searchInput.setHint("Search or type a website");
        searchInput.setBackground(null);
        searchInput.setPadding(0, 0, 0, 0);
        searchInput.setIncludeFontPadding(false);
        searchInput.setGravity(Gravity.CENTER_VERTICAL);
        searchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        searchInput.setSelectAllOnFocus(true);
        searchInput.setOnFocusChangeListener((v, hasFocus) ->
                searchPill.setBackground(hasFocus
                        ? TvUi.stroked(context, TvUi.color(context, R.color.surface_raised),
                        20, TvUi.color(context, R.color.amber), 2)
                        : TvUi.stroked(context, TvUi.color(context, R.color.surface_raised),
                        20, TvUi.color(context, R.color.line), 1)));
        searchInput.setOnClickListener(v -> openSearchKeyboard());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN;
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO || enter) {
                submitSearch();
                return true;
            }
            return false;
        });
        searchPill.addView(searchInput, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView go = TvUi.text(context, "Go", 14,
                TvUi.color(context, R.color.amber_ink), true);
        go.setGravity(Gravity.CENTER);
        go.setFocusable(true);
        go.setClickable(true);
        go.setContentDescription("Search");
        go.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.amber),
                TvUi.color(context, R.color.amber_soft),
                TvUi.color(context, R.color.cream), 12));
        TvUi.attachFocusScale(go, 1.05f);
        go.setOnClickListener(v -> submitSearch());
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(64), dp(44));
        goParams.rightMargin = dp(8);
        searchPill.addView(go, goParams);

        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setFocusable(true);
        chip.setClickable(true);
        chip.setContentDescription("Change search engine");
        chip.setPadding(dp(14), 0, dp(10), 0);
        chip.setBackground(TvUi.focusBackground(context,
                android.graphics.Color.TRANSPARENT,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.amber), 12));
        TvUi.attachFocusScale(chip, 1.04f);
        chip.setOnClickListener(v -> listener.onCycleEngine());

        engineLabel = TvUi.text(context, "Google", 13,
                TvUi.color(context, R.color.cream_dim), true);
        chip.addView(engineLabel);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_chevron_down);
        chevron.setColorFilter(TvUi.color(context, R.color.cream_muted));
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        chevronParams.leftMargin = dp(6);
        chip.addView(chevron, chevronParams);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        chipParams.rightMargin = dp(6);
        searchPill.addView(chip, chipParams);
    }

    private void buildShortcuts(LinearLayout content) {
        LinearLayout quickHeader = new LinearLayout(context);
        quickHeader.setOrientation(LinearLayout.HORIZONTAL);
        quickHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = TvUi.label(context, "Start here", 12,
                TvUi.color(context, R.color.cream_muted));
        quickHeader.addView(label);

        View spacer = new View(context);
        quickHeader.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView hint = TvUi.text(context, "Press OK to open", 13,
                TvUi.color(context, R.color.cream_muted), false);
        quickHeader.addView(hint);

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.topMargin = dp(26);
        content.addView(quickHeader, headerParams);

        Shortcut[] shortcuts = {
                new Shortcut("YouTube", "youtube.com", "https://www.youtube.com",
                        R.drawable.ic_play, R.color.brand_youtube, false),
                new Shortcut("Google", "google.com", "https://www.google.com",
                        R.drawable.ic_globe, R.color.brand_google, false),
                new Shortcut("Netflix", "netflix.com", "https://www.netflix.com",
                        R.drawable.ic_film, R.color.brand_netflix, false),
                new Shortcut("Wikipedia", "wikipedia.org", "https://www.wikipedia.org",
                        R.drawable.ic_book, R.color.brand_wikipedia, true),
                new Shortcut("Prime Video", "primevideo.com", "https://www.primevideo.com",
                        R.drawable.ic_play, R.color.brand_prime, false),
                new Shortcut("Spotify", "spotify.com", "https://open.spotify.com",
                        R.drawable.ic_music, R.color.brand_spotify, true)
        };

        LinearLayout tiles = new LinearLayout(context);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setGravity(Gravity.TOP);
        for (int i = 0; i < shortcuts.length; i++) {
            View tile = createTile(shortcuts[i]);
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(
                    0, dp(124), 1f);
            if (i < shortcuts.length - 1) tileParams.rightMargin = dp(12);
            tiles.addView(tile, tileParams);
        }

        LinearLayout.LayoutParams tilesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(124));
        tilesParams.topMargin = dp(12);
        content.addView(tiles, tilesParams);
    }

    private View createTile(Shortcut shortcut) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setContentDescription(shortcut.name);
        tile.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 18));
        TvUi.attachFocusScale(tile, 1.045f);
        tile.setOnClickListener(v -> listener.onOpen(shortcut.url));

        ImageView icon = new ImageView(context);
        icon.setImageResource(shortcut.iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setColorFilter(TvUi.color(context, shortcut.darkIcon
                ? R.color.ink : R.color.cream));
        icon.setBackground(TvUi.rounded(context,
                TvUi.color(context, shortcut.colorRes), 16));
        icon.setPadding(dp(13), dp(13), dp(13), dp(13));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView name = TvUi.text(context, shortcut.name, 15,
                TvUi.color(context, R.color.cream), true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(10);
        tile.addView(name, nameParams);

        TextView domain = TvUi.text(context, shortcut.domain, 11,
                TvUi.color(context, R.color.cream_muted), false);
        LinearLayout.LayoutParams domainParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        domainParams.topMargin = dp(3);
        tile.addView(domain, domainParams);

        return tile;
    }

    private void buildFooter(LinearLayout page) {
        View divider = new View(context);
        divider.setBackgroundColor(TvUi.color(context, R.color.line_soft));
        page.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(12), 0, 0);

        addKeyHint(footer, "D-PAD", "Move");
        addKeyHint(footer, "OK", "Select");
        addKeyHint(footer, "MENU", "Browser menu");

        View spacer = new View(context);
        footer.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView version = TvUi.text(context, "0.4.0", 12,
                TvUi.color(context, R.color.cream_muted), false);
        version.setLetterSpacing(0.08f);
        footer.addView(version);

        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
    }

    private void addKeyHint(LinearLayout footer, String keyLabel, String label) {
        TextView key = TvUi.text(context, keyLabel, 10,
                TvUi.color(context, R.color.cream_dim), true);
        key.setLetterSpacing(0.1f);
        key.setGravity(Gravity.CENTER);
        key.setPadding(dp(9), dp(5), dp(9), dp(5));
        key.setBackground(TvUi.stroked(context,
                TvUi.color(context, R.color.surface_raised), 6,
                TvUi.color(context, R.color.line), 1));
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyParams.rightMargin = dp(8);
        footer.addView(key, keyParams);

        TextView labelView = TvUi.text(context, label, 13,
                TvUi.color(context, R.color.cream_muted), false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.rightMargin = dp(28);
        footer.addView(labelView, labelParams);
    }

    /** Rebuilds the recent and bookmarks rows from the store. Called on home show. */
    public void refreshLists() {
        if (recentSection == null || bookmarkSection == null) return;

        recentSection.removeAllViews();
        List<BrowserStore.Entry> history = store.history();
        if (history.isEmpty()) {
            recentSection.setVisibility(GONE);
        } else {
            recentSection.setVisibility(VISIBLE);
            recentSection.addView(sectionHeader("Continue browsing",
                    "Your last " + Math.min(history.size(), 8) + " pages"));
            recentSection.addView(siteRow(history, 8));
        }

        bookmarkSection.removeAllViews();
        List<BrowserStore.Entry> bookmarks = store.bookmarks();
        if (bookmarks.isEmpty()) {
            bookmarkSection.setVisibility(GONE);
        } else {
            bookmarkSection.setVisibility(VISIBLE);
            bookmarkSection.addView(sectionHeader("Bookmarks",
                    "Saved from the browser"));
            bookmarkSection.addView(siteRow(bookmarks, 12));
        }
    }

    private LinearLayout sectionHeader(String title, String hint) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(34);
        row.setLayoutParams(params);

        TextView label = TvUi.label(context, title, 12,
                TvUi.color(context, R.color.cream_muted));
        row.addView(label);

        View spacer = new View(context);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView hintView = TvUi.text(context, hint, 12,
                TvUi.color(context, R.color.cream_muted), false);
        row.addView(hintView);
        return row;
    }

    private HorizontalScrollView siteRow(List<BrowserStore.Entry> entries, int limit) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setFocusable(false);
        scroll.setPadding(0, dp(14), 0, 0);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int count = Math.min(entries.size(), limit);
        for (int i = 0; i < count; i++) {
            View card = createSiteCard(entries.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(238), dp(76));
            if (i < count - 1) params.rightMargin = dp(12);
            row.addView(card, params);
        }
        scroll.addView(row, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(76)));
        return scroll;
    }

    private View createSiteCard(BrowserStore.Entry entry) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(dp(16), 0, dp(16), 0);
        card.setContentDescription(entry.title);
        card.setBackground(TvUi.focusBackground(context,
                TvUi.color(context, R.color.surface),
                TvUi.color(context, R.color.surface_focus),
                TvUi.color(context, R.color.amber), 14));
        TvUi.attachFocusScale(card, 1.03f);
        card.setOnClickListener(v -> listener.onOpen(entry.url));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_globe);
        icon.setColorFilter(TvUi.color(context, R.color.cream_muted));
        icon.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.rightMargin = dp(12);
        card.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = TvUi.text(context, entry.title, 14,
                TvUi.color(context, R.color.cream), true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title);

        TextView domain = TvUi.text(context, domainOf(entry.url), 11,
                TvUi.color(context, R.color.cream_muted), false);
        domain.setSingleLine(true);
        domain.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams domainParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        domainParams.topMargin = dp(3);
        copy.addView(domain, domainParams);

        card.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private String domainOf(String url) {
        try {
            String host = android.net.Uri.parse(url).getHost();
            if (host == null) return url;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return url;
        }
    }

    public void setEngineName(String name) {
        if (engineLabel != null) engineLabel.setText(name);
    }

    public EditText searchField() {
        return searchInput;
    }

    public boolean isSearchFocused() {
        return searchInput != null && searchInput.hasFocus();
    }

    public void openSearchKeyboard() {
        if (searchInput == null) return;
        searchInput.requestFocus();
        TvUi.showKeyboard(searchInput);
    }

    public void submitSearch() {
        if (searchInput == null) return;
        String value = searchInput.getText().toString().trim();
        if (value.isEmpty()) {
            openSearchKeyboard();
            return;
        }
        TvUi.hideKeyboard(searchInput);
        listener.onSubmit(value);
    }

    public void clearSearch() {
        if (searchInput != null) searchInput.setText("");
    }

    public void clearSearchFocus() {
        if (searchInput != null) searchInput.clearFocus();
    }

    /**
     * Puts the D-pad ring on the search field without forcing the TV keyboard open.
     * Pressing OK on the field then opens the keyboard through the activity's key
     * handling.
     */
    public void focusDefault(boolean withKeyboard) {
        if (searchInput == null) return;
        suppressKeyboard = !withKeyboard;
        searchInput.requestFocus();
        suppressKeyboard = false;
        if (withKeyboard) TvUi.showKeyboard(searchInput);
    }

    private int dp(float value) {
        return TvUi.dp(context, value);
    }
}
