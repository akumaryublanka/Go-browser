package com.gobrowser.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
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

    private static final String MODE_SMART = "SMART";
    private static final String MODE_TRACKPAD = "TRACKPAD";
    private static final String MODE_DPAD = "DPAD";
    private static final String MODE_DISABLED = "DISABLED";
    private static final String PREFS = "go_browser_pointer";

    private FrameLayout root;
    private LinearLayout browserChrome;
    private LinearLayout homeScreen;
    private FrameLayout webContainer;
    private WebView webView;
    private EditText addressInput;
    private TextView engineButton;
    private TextView statusText;
    private TextView pointerButton;
    private ProgressBar progressBar;
    private RemotePointerView pointerView;
    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private SharedPreferences pointerPrefs;

    private int searchEngineIndex = 0;
    private String pointerMode = MODE_SMART;
    private String pointerShape = "ARROW";
    private String pointerColor = "CYAN";
    private int pointerSpeed = 22;
    private int pointerAcceleration = 18;
    private int pointerSize = 30;
    private boolean pointerOutline = true;
    private boolean pointerShadow = true;
    private boolean pointerClickFeedback = true;
    private boolean pointerVisible = true;
    private float cursorX;
    private float cursorY;
    private long cursorTouchDownTime = -1L;
    private boolean cursorHovering = false;

    private final String[] engines = {"Google", "Bing", "DuckDuckGo"};
    private final String[] searchTemplates = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q="
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pointerPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadPointerPreferences();
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
        pointerButton = actionButton(pointerButtonLabel(), CYAN);
        pointerButton.setTextSize(13);
        pointerButton.setOnClickListener(v -> togglePointerMode());
        LinearLayout.LayoutParams pointerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        pointerParams.rightMargin = dp(8);
        controls.addView(pointerButton, pointerParams);
        addControl(controls, "⚙ Cursor", v -> openPointerSettings());
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
        updatePointerButton();
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

        TextView pointerTip = text("NEW  •  Press MENU on your remote or choose Cursor settings to customize the smart pointer.", 14, CYAN, false);
        LinearLayout.LayoutParams pointerTipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pointerTipParams.topMargin = dp(14);
        homeScreen.addView(pointerTip, pointerTipParams);

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
                refreshPointerVisibility();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                addressInput.setText(url);
                progressBar.setVisibility(View.GONE);
                statusTextMessage(Uri.parse(url).getHost() == null ? "Ready" : Uri.parse(url).getHost());
                refreshPointerVisibility();
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
                refreshPointerVisibility();
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }
        });

        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.GONE);

        pointerView = new RemotePointerView(this);
        pointerView.setVisibility(View.GONE);
        webContainer.addView(pointerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyPointerPreferencesToView();

        statusText = text("Ready", 12, MUTED, false);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28), Gravity.BOTTOM | Gravity.RIGHT);
        statusParams.rightMargin = dp(12);
        statusParams.bottomMargin = dp(4);
        webContainer.addView(statusText, statusParams);

        pointerView.post(this::resetPointerToCenter);
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
        pointerView.post(this::resetPointerToCenter);
        refreshPointerVisibility();
    }

    private void showHome() {
        if (webView != null) webView.stopLoading();
        if (homeScreen != null) homeScreen.setVisibility(View.VISIBLE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (statusText != null) statusText.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (addressInput != null) addressInput.setText("");
        if (pointerView != null) pointerView.setVisibility(View.GONE);
        releasePointerTouch();
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
        refreshPointerVisibility();
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

    private String pointerButtonLabel() {
        return isPointerModeEnabled() ? "◎ Pointer ON" : "◎ Pointer OFF";
    }

    private boolean isPointerModeEnabled() {
        return MODE_SMART.equals(pointerMode) || MODE_TRACKPAD.equals(pointerMode);
    }

    private void updatePointerButton() {
        if (pointerButton != null) {
            pointerButton.setText(pointerButtonLabel());
            pointerButton.setTextColor(isPointerModeEnabled() ? CYAN : Color.WHITE);
        }
    }

    private void togglePointerMode() {
        releasePointerTouch();
        pointerMode = isPointerModeEnabled() ? MODE_DPAD : MODE_SMART;
        savePointerPreferences();
        applyPointerPreferencesToView();
        updatePointerButton();
        refreshPointerVisibility();
        Toast.makeText(this, isPointerModeEnabled()
                ? "Smart pointer enabled · Arrows move · OK clicks"
                : "D-pad focus mode enabled", Toast.LENGTH_LONG).show();
        if (isPointerModeEnabled()) {
            resetPointerToCenter();
            if (webView != null && webView.getVisibility() == View.VISIBLE) webView.requestFocus();
        }
    }

    private void loadPointerPreferences() {
        pointerMode = pointerPrefs.getString("mode", MODE_SMART);
        pointerShape = pointerPrefs.getString("shape", "ARROW");
        pointerColor = pointerPrefs.getString("color", "CYAN");
        pointerSpeed = pointerPrefs.getInt("speed", 22);
        pointerAcceleration = pointerPrefs.getInt("acceleration", 18);
        pointerSize = pointerPrefs.getInt("size", 30);
        pointerOutline = pointerPrefs.getBoolean("outline", true);
        pointerShadow = pointerPrefs.getBoolean("shadow", true);
        pointerClickFeedback = pointerPrefs.getBoolean("click_feedback", true);
        pointerVisible = pointerPrefs.getBoolean("visible", true);
    }

    private void savePointerPreferences() {
        pointerPrefs.edit()
                .putString("mode", pointerMode)
                .putString("shape", pointerShape)
                .putString("color", pointerColor)
                .putInt("speed", pointerSpeed)
                .putInt("acceleration", pointerAcceleration)
                .putInt("size", pointerSize)
                .putBoolean("outline", pointerOutline)
                .putBoolean("shadow", pointerShadow)
                .putBoolean("click_feedback", pointerClickFeedback)
                .putBoolean("visible", pointerVisible)
                .apply();
    }

    private void applyPointerPreferencesToView() {
        if (pointerView == null) return;
        pointerView.configure(
                pointerShape,
                cursorColor(pointerColor),
                pointerSize,
                pointerOutline,
                pointerShadow,
                pointerClickFeedback
        );
        refreshPointerVisibility();
    }

    private int cursorColor(String name) {
        if ("WHITE".equals(name)) return Color.WHITE;
        if ("YELLOW".equals(name)) return Color.rgb(255, 220, 55);
        if ("GREEN".equals(name)) return Color.rgb(65, 235, 145);
        if ("ORANGE".equals(name)) return Color.rgb(255, 153, 54);
        return CYAN;
    }

    private void refreshPointerVisibility() {
        if (pointerView == null) return;
        boolean browsing = webView != null && webView.getVisibility() == View.VISIBLE
                && customVideoView == null;
        boolean visible = browsing && isPointerModeEnabled() && pointerVisible;
        pointerView.setVisibility(visible ? View.VISIBLE : View.GONE);
        updatePointerButton();
    }

    private void resetPointerToCenter() {
        if (pointerView == null || pointerView.getWidth() <= 0 || pointerView.getHeight() <= 0) return;
        cursorX = pointerView.getWidth() / 2f;
        cursorY = pointerView.getHeight() / 2f;
        pointerView.setCursorPosition(cursorX, cursorY);
    }

    private void movePointer(int keyCode, int repeatCount) {
        if (pointerView == null) return;
        if (pointerView.getWidth() <= 0 || pointerView.getHeight() <= 0) {
            resetPointerToCenter();
        }
        float repeatFactor = 1f + Math.min(repeatCount, 12) * (pointerAcceleration / 100f);
        float modeFactor = MODE_TRACKPAD.equals(pointerMode) ? 1.65f : 1f;
        float distance = dp(pointerSpeed) * repeatFactor * modeFactor;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) cursorX -= distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) cursorX += distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) cursorY -= distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) cursorY += distance;

        float margin = dp(8);
        cursorX = Math.max(margin, Math.min(pointerView.getWidth() - margin, cursorX));
        cursorY = Math.max(margin, Math.min(pointerView.getHeight() - margin, cursorY));
        pointerView.setCursorPosition(cursorX, cursorY);

        long now = SystemClock.uptimeMillis();
        if (cursorTouchDownTime >= 0L) {
            dispatchPointerTouch(MotionEvent.ACTION_MOVE, now);
        } else {
            dispatchPointerHover(now);
        }
    }

    private void dispatchPointerHover(long eventTime) {
        if (webView == null || webView.getVisibility() != View.VISIBLE || pointerView == null) return;
        float x = cursorX - pointerView.getLeft() - webView.getLeft();
        float y = cursorY - pointerView.getTop() - webView.getTop();
        x = Math.max(0, Math.min(webView.getWidth() - 1, x));
        y = Math.max(0, Math.min(webView.getHeight() - 1, y));
        int action = cursorHovering ? MotionEvent.ACTION_HOVER_MOVE : MotionEvent.ACTION_HOVER_ENTER;
        MotionEvent hover = obtainMouseHover(eventTime, action, x, y);
        webView.dispatchGenericMotionEvent(hover);
        hover.recycle();
        cursorHovering = true;
    }

    private MotionEvent obtainMouseHover(long eventTime, int action, float x, float y) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[1];
        properties[0] = new MotionEvent.PointerProperties();
        properties[0].id = 0;
        properties[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;

        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[1];
        coordinates[0] = new MotionEvent.PointerCoords();
        coordinates[0].x = x;
        coordinates[0].y = y;
        coordinates[0].pressure = 1f;
        coordinates[0].size = 1f;

        return MotionEvent.obtain(
                eventTime, eventTime, action, 1, properties, coordinates,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
    }

    private void exitPointerHover() {
        if (!cursorHovering || webView == null || pointerView == null) return;
        float x = cursorX - pointerView.getLeft() - webView.getLeft();
        float y = cursorY - pointerView.getTop() - webView.getTop();
        long now = SystemClock.uptimeMillis();
        MotionEvent hover = obtainMouseHover(now, MotionEvent.ACTION_HOVER_EXIT, x, y);
        webView.dispatchGenericMotionEvent(hover);
        hover.recycle();
        cursorHovering = false;
    }

    private void beginPointerTouch(long eventTime) {
        if (webView == null || webView.getVisibility() != View.VISIBLE || cursorTouchDownTime >= 0L) return;
        dispatchPointerHover(eventTime);
        cursorTouchDownTime = eventTime;
        dispatchPointerTouch(MotionEvent.ACTION_DOWN, eventTime);
        if (pointerClickFeedback && pointerView != null) pointerView.showClickFeedback();
    }

    private void endPointerTouch(long eventTime) {
        if (cursorTouchDownTime < 0L) return;
        dispatchPointerTouch(MotionEvent.ACTION_UP, eventTime);
        cursorTouchDownTime = -1L;
    }

    private void releasePointerTouch() {
        if (cursorTouchDownTime >= 0L) {
            endPointerTouch(SystemClock.uptimeMillis());
        }
        exitPointerHover();
    }

    private void dispatchPointerTouch(int action, long eventTime) {
        if (webView == null || pointerView == null || webView.getVisibility() != View.VISIBLE) return;
        float x = cursorX - pointerView.getLeft() - webView.getLeft();
        float y = cursorY - pointerView.getTop() - webView.getTop();
        x = Math.max(0, Math.min(webView.getWidth() - 1, x));
        y = Math.max(0, Math.min(webView.getHeight() - 1, y));
        long downTime = cursorTouchDownTime >= 0L ? cursorTouchDownTime : eventTime;
        MotionEvent motion = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        webView.dispatchTouchEvent(motion);
        motion.recycle();
    }

    private boolean isWebPagePointerTarget() {
        return webView != null && webView.getVisibility() == View.VISIBLE
                && customVideoView == null && addressInput != null && !addressInput.hasFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getRepeatCount() == 0 &&
                (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS)) {
            openPointerSettings();
            return true;
        }

        if (isWebPagePointerTarget() && isPointerModeEnabled() && pointerVisible) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    movePointer(keyCode, event.getRepeatCount());
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) beginPointerTouch(event.getEventTime());
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    endPointerTouch(event.getEventTime());
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void openPointerSettings() {
        final String[] modeLabels = {"Smart Pointer", "Trackpad", "D-pad Focus", "Pointer Disabled"};
        final String[] modeValues = {MODE_SMART, MODE_TRACKPAD, MODE_DPAD, MODE_DISABLED};
        final String[] shapeLabels = {"Arrow", "Ring", "Crosshair"};
        final String[] shapeValues = {"ARROW", "RING", "CROSSHAIR"};
        final String[] colorLabels = {"Cyan", "White", "Yellow", "Green", "Orange"};
        final String[] colorValues = {"CYAN", "WHITE", "YELLOW", "GREEN", "ORANGE"};

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(12), dp(22), dp(22));
        panel.setBackgroundColor(BG);
        scroll.addView(panel);

        addSettingsIntro(panel, "GO POINTER STUDIO",
                "Tune the remote cursor for your TV, screen size and preferred feel.");

        addSpinnerSetting(panel, "CONTROL MODE", modeLabels, indexOf(modeValues, pointerMode), position -> {
            pointerMode = modeValues[position];
            savePointerPreferences();
            applyPointerPreferencesToView();
            updatePointerButton();
        });

        addSeekBarSetting(panel, "MOVEMENT SPEED", 8, 48, pointerSpeed, " dp / step", value -> {
            pointerSpeed = value;
            savePointerPreferences();
            applyPointerPreferencesToView();
        });

        addSeekBarSetting(panel, "ACCELERATION", 0, 40, pointerAcceleration, "% per repeat", value -> {
            pointerAcceleration = value;
            savePointerPreferences();
        });

        addSeekBarSetting(panel, "CURSOR SIZE", 20, 48, pointerSize, " dp", value -> {
            pointerSize = value;
            savePointerPreferences();
            applyPointerPreferencesToView();
        });

        addSpinnerSetting(panel, "CURSOR SHAPE", shapeLabels, indexOf(shapeValues, pointerShape), position -> {
            pointerShape = shapeValues[position];
            savePointerPreferences();
            applyPointerPreferencesToView();
        });

        addSpinnerSetting(panel, "CURSOR COLOUR", colorLabels, indexOf(colorValues, pointerColor), position -> {
            pointerColor = colorValues[position];
            savePointerPreferences();
            applyPointerPreferencesToView();
        });

        addSettingsSection(panel, "FINISH & FEEDBACK");
        addToggleSetting(panel, "Dark high-contrast outline", pointerOutline, checked -> {
            pointerOutline = checked;
            savePointerPreferences();
            applyPointerPreferencesToView();
        });
        addToggleSetting(panel, "Soft shadow", pointerShadow, checked -> {
            pointerShadow = checked;
            savePointerPreferences();
            applyPointerPreferencesToView();
        });
        addToggleSetting(panel, "Click ripple animation", pointerClickFeedback, checked -> {
            pointerClickFeedback = checked;
            savePointerPreferences();
            applyPointerPreferencesToView();
        });
        addToggleSetting(panel, "Show pointer while browsing", pointerVisible, checked -> {
            pointerVisible = checked;
            savePointerPreferences();
            refreshPointerVisibility();
        });

        TextView resetPosition = actionButton("◎  Reset pointer to centre", CYAN);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        resetParams.topMargin = dp(12);
        panel.addView(resetPosition, resetParams);
        resetPosition.setOnClickListener(v -> resetPointerToCenter());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cursor settings")
                .setView(scroll)
                .setNegativeButton("Reset defaults", (d, which) -> resetPointerPreferences())
                .setPositiveButton("Done", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getWindow().setBackgroundDrawable(round(PANEL, 18));
            dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(80), dp(760)),
                    Math.min(getResources().getDisplayMetrics().heightPixels - dp(80), dp(720)));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(CYAN);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
        });
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(round(PANEL, 18));
        dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(80), dp(760)),
                Math.min(getResources().getDisplayMetrics().heightPixels - dp(80), dp(720)));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(CYAN);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
    }

    private void addSettingsIntro(LinearLayout panel, String title, String description) {
        TextView eyebrow = text(title, 12, CYAN, true);
        panel.addView(eyebrow);
        TextView body = text(description, 15, MUTED, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        params.bottomMargin = dp(16);
        panel.addView(body, params);
    }

    private void addSettingsSection(LinearLayout panel, String title) {
        TextView heading = text(title, 12, CYAN, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(18);
        params.bottomMargin = dp(6);
        panel.addView(heading, params);
    }

    private void addSpinnerSetting(LinearLayout panel, String title, String[] choices,
                                   int selected, OnChoiceChanged listener) {
        TextView label = text(title, 12, MUTED, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(10);
        panel.addView(label, labelParams);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, selected));
        spinner.setBackground(round(PANEL_FOCUS, 10));
        spinner.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        spinnerParams.topMargin = dp(6);
        panel.addView(spinner, spinnerParams);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (first) {
                    first = false;
                    return;
                }
                listener.onChanged(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void addSeekBarSetting(LinearLayout panel, String title, int min, int max,
                                   int initial, String suffix, OnIntChanged listener) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = text(title, 12, MUTED, true);
        line.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text(initial + suffix, 12, Color.WHITE, true);
        value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        line.addView(value);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lineParams.topMargin = dp(14);
        panel.addView(line, lineParams);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(initial - min);
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        panel.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int newValue = min + progress;
                value.setText(newValue + suffix);
                if (fromUser) listener.onChanged(newValue);
            }
            @Override
            public void onStartTrackingTouch(SeekBar bar) { }
            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });
    }

    private void addToggleSetting(LinearLayout panel, String title, boolean initial,
                                  OnBooleanChanged listener) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(title);
        checkBox.setTextSize(14);
        checkBox.setTextColor(Color.WHITE);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(CYAN));
        checkBox.setPadding(0, dp(3), 0, dp(3));
        checkBox.setChecked(initial);
        panel.addView(checkBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        checkBox.setOnCheckedChangeListener((button, checked) -> listener.onChanged(checked));
    }

    private int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) return i;
        }
        return 0;
    }

    private void resetPointerPreferences() {
        pointerMode = MODE_SMART;
        pointerShape = "ARROW";
        pointerColor = "CYAN";
        pointerSpeed = 22;
        pointerAcceleration = 18;
        pointerSize = 30;
        pointerOutline = true;
        pointerShadow = true;
        pointerClickFeedback = true;
        pointerVisible = true;
        savePointerPreferences();
        applyPointerPreferencesToView();
        updatePointerButton();
        refreshPointerVisibility();
        resetPointerToCenter();
        Toast.makeText(this, "Pointer settings restored", Toast.LENGTH_SHORT).show();
    }

    private interface OnChoiceChanged { void onChanged(int position); }
    private interface OnIntChanged { void onChanged(int value); }
    private interface OnBooleanChanged { void onChanged(boolean value); }

    @Override
    public void onBackPressed() {
        if (customVideoView != null) {
            exitFullscreenVideo();
        } else {
            releasePointerTouch();
            goBack();
        }
    }

    @Override
    protected void onPause() {
        releasePointerTouch();
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
        releasePointerTouch();
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
