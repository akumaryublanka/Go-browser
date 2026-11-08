package com.gobrowser.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 11, 24);
    private static final int PANEL = Color.rgb(19, 24, 47);
    private static final int PANEL_FOCUS = Color.rgb(38, 45, 82);
    private static final int PURPLE = Color.rgb(139, 124, 255);
    private static final int CYAN = Color.rgb(25, 215, 232);
    private static final int MUTED = Color.rgb(164, 174, 204);

    private FrameLayout root;
    private LinearLayout browserChrome;
    private LinearLayout homeScreen;
    private FrameLayout webContainer;
    private WebView webView;
    private EditText addressInput;
    private TextView engineButton;
    private TextView statusText;
    private ProgressBar progressBar;
    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int searchEngineIndex = 0;

    private final String[] engines = {
            "Google", "Bing", "DuckDuckGo"
    };

    private final String[] searchTemplates = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q="
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        buildInterface();
        configureWebView();
        showHome();
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(28), dp(18), dp(28), dp(18));
        root.addView(main, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        browserChrome = new LinearLayout(this);
        browserChrome.setGravity(Gravity.CENTER_VERTICAL);
        browserChrome.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text("GO", 26, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(PURPLE, 14));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(64), dp(54));
        logoParams.rightMargin = dp(14);
        browserChrome.addView(logo, logoParams);

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextSize(17);
        addressInput.setTextColor(Color.WHITE);
        addressInput.setHintTextColor(MUTED);
        addressInput.setHint("Search or enter website address");
        addressInput.setPadding(dp(18), 0, dp(18), 0);
        addressInput.setSelectAllOnFocus(true);
        addressInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressInput.setBackground(round(PANEL, 14));
        addressInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadFromAddressBar();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        addressParams.rightMargin = dp(10);
        browserChrome.addView(addressInput, addressParams);

        engineButton = actionButton(engines[searchEngineIndex], CYAN);
        engineButton.setOnClickListener(v -> cycleSearchEngine());
        browserChrome.addView(engineButton, new LinearLayout.LayoutParams(dp(132), dp(54)));

        main.addView(browserChrome, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        addControl(controls, "‹  Back", v -> goBack());
        addControl(controls, "Forward  ›", v -> goForward());
        addControl(controls, "↻  Reload", v -> reload());
        addControl(controls, "⌂  Home", v -> showHome());
        main.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        main.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webContainer = new FrameLayout(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        webParams.topMargin = dp(10);
        main.addView(webContainer, webParams);

        buildHomeScreen();
        setContentView(root);
    }

    private void buildHomeScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        homeScreen = new LinearLayout(this);
        homeScreen.setOrientation(LinearLayout.VERTICAL);
        homeScreen.setPadding(dp(8), dp(12), dp(8), dp(28));

        TextView eyebrow = text("YOUR BIG-SCREEN WEB", 13, CYAN, true);
        homeScreen.addView(eyebrow);

        TextView heading = text("Browse without limits.", 34, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingParams.topMargin = dp(8);
        homeScreen.addView(heading, headingParams);

        TextView subtitle = text("A clean, remote-friendly starting point for the open web.", 17, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        homeScreen.addView(subtitle, subtitleParams);

        TextView quickTitle = text("QUICK LAUNCH", 13, MUTED, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(30);
        homeScreen.addView(quickTitle, titleParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addShortcut(row, "YouTube", "https://www.youtube.com");
        addShortcut(row, "Google", "https://www.google.com");
        addShortcut(row, "Wikipedia", "https://www.wikipedia.org");
        addShortcut(row, "Netflix", "https://www.netflix.com");
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(92));
        rowParams.topMargin = dp(12);
        homeScreen.addView(row, rowParams);

        TextView note = text("Tip: use your TV remote to move between controls, then press OK to select.", 15, MUTED, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(24);
        homeScreen.addView(note, noteParams);

        scroll.addView(homeScreen);
        webContainer.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void addShortcut(LinearLayout row, String title, String url) {
        TextView tile = text(title, 18, Color.WHITE, true);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setBackground(focusBackground());
        tile.setOnClickListener(v -> loadUrl(url));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(76), 1f);
        params.rightMargin = dp(12);
        row.addView(tile, params);
    }

    private void configureWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString());

        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(view, request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                addressInput.setText(url);
                progressBar.setVisibility(View.VISIBLE);
                statusTextMessage("Loading " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                addressInput.setText(url);
                progressBar.setVisibility(View.GONE);
                statusTextMessage(Uri.parse(url).getHost() == null ? "Ready" : Uri.parse(url).getHost());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    statusTextMessage("Page couldn't load. Check your connection and retry.");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;
                root.addView(customVideoView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                browserChrome.setVisibility(View.GONE);
                webContainer.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }
        });

        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.GONE);

        statusText = text("Ready", 12, MUTED, false);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28), Gravity.BOTTOM | Gravity.RIGHT);
        statusParams.rightMargin = dp(12);
        statusParams.bottomMargin = dp(4);
        webContainer.addView(statusText, statusParams);
    }

    private boolean handleUrl(WebView view, String value) {
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ||
                "about".equalsIgnoreCase(scheme) || scheme == null) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void loadFromAddressBar() {
        String input = addressInput.getText().toString().trim();
        if (input.isEmpty()) return;
        hideKeyboard();
        String target;
        if (input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            target = input;
        } else if (input.contains(" ") || !input.contains(".")) {
            try {
                target = searchTemplates[searchEngineIndex] +
                        URLEncoder.encode(input, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                target = searchTemplates[0] + Uri.encode(input);
            }
        } else {
            target = "https://" + input;
        }
        loadUrl(target);
    }

    private void loadUrl(String url) {
        homeScreen.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void showHome() {
        if (webView != null) webView.stopLoading();
        if (homeScreen != null) homeScreen.setVisibility(View.VISIBLE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (statusText != null) statusText.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (addressInput != null) addressInput.setText("");
    }

    private void goBack() {
        if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else {
            showHome();
        }
    }

    private void goForward() {
        if (webView != null && webView.canGoForward()) webView.goForward();
    }

    private void reload() {
        if (webView != null && webView.getVisibility() == View.VISIBLE) webView.reload();
    }

    private void cycleSearchEngine() {
        searchEngineIndex = (searchEngineIndex + 1) % engines.length;
        engineButton.setText(engines[searchEngineIndex]);
        Toast.makeText(this, "Search: " + engines[searchEngineIndex], Toast.LENGTH_SHORT).show();
    }

    private void exitFullscreenVideo() {
        if (customVideoView == null) return;
        root.removeView(customVideoView);
        customVideoView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        browserChrome.setVisibility(View.VISIBLE);
        webContainer.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void statusTextMessage(String message) {
        if (statusText != null) statusText.setText(message);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(addressInput.getWindowToken(), 0);
            addressInput.clearFocus();
        } catch (Exception ignored) {
            // The TV may not have a software keyboard.
        }
    }

    private void addControl(LinearLayout parent, String label, View.OnClickListener listener) {
        TextView button = actionButton(label, Color.WHITE);
        button.setTextSize(14);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        params.rightMargin = dp(8);
        parent.addView(button, params);
    }

    private TextView actionButton(String label, int color) {
        TextView button = text(label, 15, color, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(focusBackground());
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    private android.graphics.drawable.Drawable focusBackground() {
        android.graphics.drawable.Drawable normal = round(PANEL, 14);
        android.graphics.drawable.Drawable focused = round(PANEL_FOCUS, 14);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (customVideoView != null) {
            exitFullscreenVideo();
        } else {
            goBack();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
