package com.gobrowser.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.gobrowser.tv.ui.BrowserBar;
import com.gobrowser.tv.ui.HomeView;
import com.gobrowser.tv.ui.QuickBar;
import com.gobrowser.tv.ui.SettingsPanel;
import com.gobrowser.tv.ui.TabSwitcher;
import com.gobrowser.tv.ui.TvUi;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String APP_PREFS = "go_browser";
    private static final int MAX_TABS = 8;

    private static final String[] ENGINES = {"Google", "Bing", "DuckDuckGo"};
    private static final String[] ENGINE_SHORT = {"Google", "Bing", "DDG"};
    private static final String[] SEARCH_TEMPLATES = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q="
    };

    private FrameLayout root;
    private LinearLayout browserArea;
    private FrameLayout webContainer;
    private WebView webView;
    private HomeView homeView;
    private BrowserBar browserBar;
    private SettingsPanel settingsPanel;
    private QuickBar quickBar;
    private TabSwitcher tabSwitcher;
    private ProgressBar progressBar;
    private RemotePointerView pointerView;
    private PointerSettings settings;
    private SharedPreferences appPrefs;
    private BrowserStore store;

    private int searchEngineIndex = 0;
    private boolean browsing = false;
    private boolean settingsOpen = false;
    private boolean quickBarOpen = false;
    private boolean tabsOpen = false;
    private boolean showingError = false;
    private boolean desktopMode = false;
    private String errorUrl;
    private String mobileUserAgent;
    private String desktopUserAgent;

    private static final class Tab {
        String url = "";
        String title = "New tab";
    }

    private final ArrayList<Tab> tabs = new ArrayList<>();
    private int activeTab = 0;

    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private WebView popupWebView;

    private float cursorX;
    private float cursorY;
    private long cursorTouchDownTime = -1L;
    private boolean cursorHovering = false;

    private final HomeView.Listener homeListener = new HomeView.Listener() {
        @Override
        public void onSubmit(String input) {
            navigate(input);
        }

        @Override
        public void onOpen(String url) {
            loadUrl(url);
        }

        @Override
        public void onCycleEngine() {
            cycleSearchEngine();
        }

        @Override
        public void onOpenSettings() {
            openSettings();
        }
    };

    private final BrowserBar.Listener browserListener = new BrowserBar.Listener() {
        @Override
        public void onBack() {
            goBack();
        }

        @Override
        public void onForward() {
            if (webView != null && webView.canGoForward()) webView.goForward();
        }

        @Override
        public void onHome() {
            showHome();
        }

        @Override
        public void onReload() {
            if (webView != null && browsing) webView.reload();
        }

        @Override
        public void onCycleEngine() {
            cycleSearchEngine();
        }

        @Override
        public void onTogglePointer() {
            togglePointerMode();
        }

        @Override
        public void onOpenSettings() {
            openSettings();
        }

        @Override
        public void onToggleBookmark() {
            toggleBookmark();
        }

        @Override
        public void onOpenTabs() {
            openTabSwitcher();
        }

        @Override
        public void onSubmit(String input) {
            navigate(input);
        }
    };

    private final QuickBar.Listener quickListener = new QuickBar.Listener() {
        @Override
        public void onBack() {
            closeQuickBar();
            goBack();
        }

        @Override
        public void onForward() {
            closeQuickBar();
            if (webView != null && webView.canGoForward()) webView.goForward();
        }

        @Override
        public void onReload() {
            closeQuickBar();
            if (webView != null && browsing) webView.reload();
        }

        @Override
        public void onHome() {
            closeQuickBar();
            showHome();
        }

        @Override
        public void onSearch() {
            closeQuickBar();
            if (browsing) {
                browserBar.focusAddress();
                refreshPointerVisibility();
            } else {
                homeView.focusDefault(true);
            }
        }

        @Override
        public void onTabs() {
            closeQuickBar();
            openTabSwitcher();
        }

        @Override
        public void onBookmark() {
            closeQuickBar();
            toggleBookmark();
        }

        @Override
        public void onSettings() {
            closeQuickBar();
            openSettings();
        }
    };

    private final TabSwitcher.Listener tabsListener = new TabSwitcher.Listener() {
        @Override
        public void onSelect(int index) {
            selectTab(index);
        }

        @Override
        public void onNewTab() {
            newTab();
        }

        @Override
        public void onCloseTab(int index) {
            closeTab(index);
        }

        @Override
        public void onDismiss() {
            closeTabSwitcher();
        }
    };

    private final SettingsPanel.Listener settingsListener = new SettingsPanel.Listener() {
        @Override
        public void onSettingsChanged() {
            applyPointerSettings();
        }

        @Override
        public void onClose() {
            closeSettings();
        }

        @Override
        public boolean isDesktopMode() {
            return desktopMode;
        }

        @Override
        public void onDesktopModeChanged(boolean enabled) {
            setDesktopMode(enabled);
        }

        @Override
        public void onClearHistory() {
            store.clearHistory();
            homeView.refreshLists();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PointerSettings.load(this);
        appPrefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        store = new BrowserStore(this);
        searchEngineIndex = appPrefs.getInt("engine", 0);
        if (searchEngineIndex < 0 || searchEngineIndex >= ENGINES.length) searchEngineIndex = 0;
        desktopMode = appPrefs.getBoolean("desktop_mode", false);
        if (tabs.isEmpty()) {
            tabs.add(new Tab());
            activeTab = 0;
        }

        buildInterface();
        configureWebView();
        applyPointerSettings();
        applyUserAgent();
        updateEngineLabels();
        showHome();
        openFromIntent(getIntent());
        enterImmersive();
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(TvUi.color(this, R.color.ink));

        browserArea = new LinearLayout(this);
        browserArea.setOrientation(LinearLayout.VERTICAL);
        browserArea.setVisibility(View.GONE);

        browserBar = new BrowserBar(this, browserListener);
        browserArea.addView(browserBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        browserBar.setTabCount(tabs.size());

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(ColorStateList.valueOf(
                TvUi.color(this, R.color.amber)));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(
                TvUi.color(this, R.color.line_soft)));
        progressBar.setVisibility(View.GONE);
        browserArea.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TvUi.dp(this, 2)));

        webContainer = new FrameLayout(this);
        webContainer.setBackgroundColor(Color.WHITE);
        browserArea.addView(webContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(browserArea, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        homeView = new HomeView(this, store, homeListener);
        root.addView(homeView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        quickBar = new QuickBar(this, quickListener);
        quickBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams quickParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
        quickParams.bottomMargin = TvUi.dp(this, 28);
        root.addView(quickBar, quickParams);

        tabSwitcher = new TabSwitcher(this, tabsListener);
        tabSwitcher.setVisibility(View.GONE);
        root.addView(tabSwitcher, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        settingsPanel = new SettingsPanel(this, settings, settingsListener);
        settingsPanel.setVisibility(View.GONE);
        root.addView(settingsPanel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private void configureWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setGeolocationEnabled(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webSettings.setTextZoom(105);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

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
                if (!showingError && !browserBar.isAddressFocused()) browserBar.setUrl(url);
                progressBar.setVisibility(View.VISIBLE);
                updateActiveTab(url, null);
                browserBar.setBookmarked(url != null && store.isBookmarked(url));
                refreshPointerVisibility();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (showingError) {
                    showingError = false;
                    browserBar.setUrl(errorUrl);
                } else {
                    if (!browserBar.isAddressFocused()) browserBar.setUrl(url);
                    String title = view.getTitle();
                    updateActiveTab(url, title);
                    if (url != null && !url.startsWith("data:") && !url.startsWith("about:")) {
                        store.addHistory(title, url);
                        homeView.refreshLists();
                    }
                    browserBar.setBookmarked(url != null && store.isBookmarked(url));
                }
                progressBar.setVisibility(View.GONE);
                refreshPointerVisibility();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    handlePageError(request.getUrl().toString());
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
                enterFullscreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }

            /**
             * Sites built for the desktop often open links with target=_blank or
             * window.open. On a TV there is nowhere for a second window to go, so
             * the popup is captured and loaded into the current tab instead.
             */
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView probe = new WebView(MainActivity.this);
                probe.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView popup,
                                                            WebResourceRequest request) {
                        openPopup(request.getUrl().toString());
                        return true;
                    }

                    @SuppressWarnings("deprecation")
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView popup, String url) {
                        openPopup(url);
                        return true;
                    }
                });
                popupWebView = probe;
                // If the popup never navigates, drop the probe instead of leaking it.
                root.postDelayed(() -> {
                    if (popupWebView == probe) {
                        popupWebView = null;
                        try {
                            probe.destroy();
                        } catch (Exception ignored) {
                            // Already detached.
                        }
                    }
                }, 8000L);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(probe);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.GONE);

        pointerView = new RemotePointerView(this);
        pointerView.setVisibility(View.GONE);
        webContainer.addView(pointerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void handlePageError(String failingUrl) {
        errorUrl = failingUrl;
        showingError = true;
        progressBar.setVisibility(View.GONE);
        browserBar.setUrl(failingUrl);
        showErrorPage(failingUrl);
    }

    private void showErrorPage(String failingUrl) {
        String safe = failingUrl == null ? "" : failingUrl
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String html = "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>"
                + "html,body{height:100%;margin:0}"
                + "body{background:#0C0B09;color:#F4EFE6;font-family:sans-serif;"
                + "display:flex;align-items:center;justify-content:center;text-align:center}"
                + ".wrap{max-width:620px;padding:40px}"
                + ".eyebrow{color:#F0A33A;letter-spacing:.22em;font-size:12px;font-weight:700}"
                + "h1{font-size:36px;margin:18px 0 12px}"
                + "p{color:#A89F90;font-size:18px;line-height:1.55;margin:0}"
                + ".url{display:inline-block;margin-top:22px;padding:10px 14px;"
                + "border:1px solid #332D24;border-radius:10px;color:#6E6659;"
                + "font-size:13px;word-break:break-all}"
                + "</style></head><body><div class=\"wrap\">"
                + "<div class=\"eyebrow\">CONNECTION LOST</div>"
                + "<h1>This page didn't load.</h1>"
                + "<p>Check your Wi-Fi, then open MENU and press Reload to try again.</p>"
                + "<div class=\"url\">" + safe + "</div>"
                + "</div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private boolean handleUrl(WebView view, String value) {
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "about".equalsIgnoreCase(scheme) || scheme == null) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /** Loads a captured popup URL in the active tab, then frees the probe WebView. */
    private void openPopup(String url) {
        if (popupWebView != null) {
            final WebView dead = popupWebView;
            popupWebView = null;
            // Destroy after the current WebView callback unwinds; tearing a
            // WebView down inside its own client callback can crash older builds.
            root.post(() -> {
                try {
                    dead.stopLoading();
                    dead.destroy();
                } catch (Exception ignored) {
                    // A probe that never finished attaching is safe to drop.
                }
            });
        }
        if (url == null || url.isEmpty()) return;
        loadUrl(url);
    }

    private void navigate(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return;

        String target;
        if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            target = value;
        } else if (value.contains(" ") || !value.contains(".")) {
            try {
                target = SEARCH_TEMPLATES[searchEngineIndex]
                        + URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                target = SEARCH_TEMPLATES[0] + Uri.encode(value);
            }
        } else {
            target = "https://" + value;
        }
        loadUrl(target);
    }

    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) return;
        hideKeyboard();
        hideQuickBar();
        hideTabSwitcher();
        if (settingsOpen) closeSettings();
        browsing = true;
        homeView.setVisibility(View.GONE);
        browserArea.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);
        browserBar.setUrl(url);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        if (activeTab < tabs.size()) tabs.get(activeTab).url = url;
        webView.loadUrl(url);
        webView.requestFocus();
        updateTabCount();
        resetPointerToCenter();
        refreshPointerVisibility();
    }

    private void showHome() {
        browsing = false;
        hideQuickBar();
        hideTabSwitcher();
        if (webView != null) {
            webView.stopLoading();
            webView.setVisibility(View.GONE);
        }
        browserArea.setVisibility(View.GONE);
        homeView.setVisibility(View.VISIBLE);
        homeView.clearSearch();
        homeView.clearSearchFocus();
        browserBar.clearAddress();
        progressBar.setVisibility(View.GONE);
        releasePointerTouch();
        homeView.refreshLists();
        updateTabCount();
        refreshPointerVisibility();
        homeView.post(() -> {
            // The intent handler may have opened a page in the meantime.
            if (homeView.getVisibility() == View.VISIBLE && !browsing) {
                homeView.focusDefault(false);
            }
        });
    }

    private void openFromIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        Uri data = intent.getData();
        if (data == null) return;
        String url = data.toString();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            loadUrl(url);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFromIntent(intent);
    }

    private void goBack() {
        if (webView != null && browsing && webView.canGoBack()) {
            webView.goBack();
        } else {
            showHome();
        }
    }

    private void cycleSearchEngine() {
        searchEngineIndex = (searchEngineIndex + 1) % ENGINES.length;
        appPrefs.edit().putInt("engine", searchEngineIndex).apply();
        updateEngineLabels();
    }

    private void updateEngineLabels() {
        homeView.setEngineName(ENGINES[searchEngineIndex]);
        browserBar.setEngineName(ENGINE_SHORT[searchEngineIndex]);
    }

    private void applyUserAgent() {
        if (webView == null) return;
        if (mobileUserAgent == null) {
            mobileUserAgent = webView.getSettings().getUserAgentString();
            desktopUserAgent = makeDesktopUserAgent(mobileUserAgent);
        }
        webView.getSettings().setUserAgentString(
                desktopMode ? desktopUserAgent : mobileUserAgent);
    }

    private String makeDesktopUserAgent(String userAgent) {
        String value = userAgent == null ? "" : userAgent;
        value = value.replace("Android", "X11; Linux x86_64")
                .replace("; wv", "")
                .replace("Mobile", "")
                .replaceAll("\\s+", " ")
                .trim();
        return value;
    }

    private void setDesktopMode(boolean enabled) {
        desktopMode = enabled;
        appPrefs.edit().putBoolean("desktop_mode", enabled).apply();
        applyUserAgent();
        if (browsing && webView != null) webView.reload();
    }

    private void toggleBookmark() {
        if (!browsing || webView == null) return;
        String url = webView.getUrl();
        if (url == null || url.isEmpty() || url.startsWith("about:")) return;
        boolean added = store.toggleBookmark(webView.getTitle(), url);
        browserBar.setBookmarked(added);
        homeView.refreshLists();
        Toast.makeText(this, added ? "Bookmark added" : "Bookmark removed",
                Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        if (settingsOpen) return;
        if (customVideoView != null) exitFullscreenVideo();
        hideKeyboard();
        hideQuickBar();
        hideTabSwitcher();
        releasePointerTouch();
        settingsOpen = true;
        homeView.setVisibility(View.GONE);
        browserArea.setVisibility(View.GONE);
        settingsPanel.setVisibility(View.VISIBLE);
        settingsPanel.requestInitialFocus();
        refreshPointerVisibility();
    }

    private void closeSettings() {
        if (!settingsOpen) return;
        settingsOpen = false;
        settingsPanel.setVisibility(View.GONE);
        if (browsing) {
            browserArea.setVisibility(View.VISIBLE);
            if (webView != null) webView.requestFocus();
        } else {
            homeView.setVisibility(View.VISIBLE);
            homeView.post(() -> {
                if (homeView.getVisibility() == View.VISIBLE && !browsing) {
                    homeView.focusDefault(false);
                }
            });
        }
        refreshPointerVisibility();
    }

    private void toggleQuickBar() {
        if (quickBarOpen) {
            closeQuickBar();
            return;
        }
        if (settingsOpen || tabsOpen) return;
        hideKeyboard();
        releasePointerTouch();
        quickBarOpen = true;
        boolean canBack = browsing && webView != null && webView.canGoBack();
        boolean canForward = browsing && webView != null && webView.canGoForward();
        boolean bookmarked = browsing && webView != null
                && webView.getUrl() != null && store.isBookmarked(webView.getUrl());
        quickBar.setState(canBack, canForward, bookmarked, tabs.size());
        quickBar.setVisibility(View.VISIBLE);
        quickBar.requestInitialFocus();
        refreshPointerVisibility();
    }

    private void closeQuickBar() {
        hideQuickBar();
        restoreFocus();
        refreshPointerVisibility();
    }

    private void hideQuickBar() {
        if (quickBar == null || !quickBarOpen) return;
        quickBarOpen = false;
        quickBar.setVisibility(View.GONE);
    }

    private void openTabSwitcher() {
        if (tabsOpen) return;
        if (settingsOpen || quickBarOpen) return;
        hideKeyboard();
        releasePointerTouch();
        tabsOpen = true;
        tabSwitcher.show(buildTabItems());
        tabSwitcher.setVisibility(View.VISIBLE);
        refreshPointerVisibility();
    }

    private void closeTabSwitcher() {
        if (!tabsOpen) return;
        tabsOpen = false;
        tabSwitcher.setVisibility(View.GONE);
        restoreFocus();
        refreshPointerVisibility();
    }

    private void hideTabSwitcher() {
        if (tabSwitcher == null || !tabsOpen) return;
        tabsOpen = false;
        tabSwitcher.setVisibility(View.GONE);
    }

    private void refreshTabSwitcher() {
        if (!tabsOpen) return;
        tabSwitcher.show(buildTabItems());
    }

    private List<TabSwitcher.Item> buildTabItems() {
        List<TabSwitcher.Item> items = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            items.add(new TabSwitcher.Item(tab.title, tab.url, i == activeTab));
        }
        return items;
    }

    private void updateTabCount() {
        if (browserBar != null) browserBar.setTabCount(tabs.size());
    }

    private void updateActiveTab(String url, String title) {
        if (activeTab < 0 || activeTab >= tabs.size()) return;
        Tab tab = tabs.get(activeTab);
        if (url != null && !url.isEmpty()) tab.url = url;
        if (title != null && !title.trim().isEmpty()) tab.title = title;
        if (title == null && url != null && !url.isEmpty()) {
            String host = Uri.parse(url).getHost();
            if (host != null) {
                tab.title = host.startsWith("www.") ? host.substring(4) : host;
            }
        }
        updateTabCount();
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (index == activeTab) {
            if (!browsing) {
                Tab tab = tabs.get(activeTab);
                if (!tab.url.isEmpty()) {
                    loadUrl(tab.url);
                    return;
                }
            }
            closeTabSwitcher();
            return;
        }
        hideTabSwitcher();
        activeTab = index;
        Tab tab = tabs.get(activeTab);
        if (tab.url == null || tab.url.isEmpty()) {
            showHome();
        } else {
            loadUrl(tab.url);
        }
    }

    private void newTab() {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, "Maximum " + MAX_TABS + " tabs", Toast.LENGTH_SHORT).show();
            return;
        }
        tabs.add(new Tab());
        activeTab = tabs.size() - 1;
        hideTabSwitcher();
        updateTabCount();
        showHome();
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (tabs.size() == 1) {
            tabs.set(0, new Tab());
            activeTab = 0;
            hideTabSwitcher();
            updateTabCount();
            showHome();
            return;
        }

        boolean wasActive = index == activeTab;
        tabs.remove(index);
        if (index < activeTab) {
            activeTab--;
        } else if (wasActive) {
            activeTab = Math.min(index, tabs.size() - 1);
        }

        updateTabCount();
        if (wasActive) {
            hideTabSwitcher();
            Tab tab = tabs.get(activeTab);
            if (tab.url == null || tab.url.isEmpty()) showHome();
            else loadUrl(tab.url);
        } else {
            refreshTabSwitcher();
        }
    }

    private void restoreFocus() {
        if (settingsOpen) {
            settingsPanel.requestInitialFocus();
        } else if (tabsOpen) {
            return;
        } else if (browsing && webView != null) {
            webView.requestFocus();
        } else if (homeView != null) {
            homeView.post(() -> {
                if (homeView.getVisibility() == View.VISIBLE && !browsing) {
                    homeView.focusDefault(false);
                }
            });
        }
    }

    private void hideKeyboard() {
        if (browserBar != null) TvUi.hideKeyboard(browserBar.addressField());
        if (homeView != null) TvUi.hideKeyboard(homeView.searchField());
    }

    private void enterFullscreenVideo(View view, WebChromeClient.CustomViewCallback callback) {
        if (customVideoView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customVideoView = view;
        customViewCallback = callback;
        root.addView(customVideoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        browserArea.setVisibility(View.GONE);
        hideQuickBar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        refreshPointerVisibility();
        enterImmersive();
    }

    private void exitFullscreenVideo() {
        if (customVideoView == null) return;
        root.removeView(customVideoView);
        customVideoView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        if (browsing) browserArea.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        refreshPointerVisibility();
        enterImmersive();
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void applyPointerSettings() {
        if (pointerView != null) {
            pointerView.configure(settings.shape, settings.colorInt(), settings.size,
                    settings.outline, settings.shadow, settings.clickFeedback);
        }
        if (browserBar != null) browserBar.setPointerEnabled(settings.isPointerEnabled());
        refreshPointerVisibility();
    }

    private void togglePointerMode() {
        releasePointerTouch();
        settings.mode = settings.isPointerEnabled()
                ? PointerSettings.MODE_DPAD
                : PointerSettings.MODE_SMART;
        settings.save(this);
        applyPointerSettings();
        if (settings.isPointerEnabled()) {
            resetPointerToCenter();
            if (browsing && webView != null) webView.requestFocus();
        }
        Toast.makeText(this, settings.isPointerEnabled()
                ? "Point mode on \u2014 arrows move the cursor, OK clicks"
                : "D-pad mode \u2014 arrows move between links", Toast.LENGTH_LONG).show();
    }

    private void refreshPointerVisibility() {
        if (pointerView == null) return;
        boolean pageVisible = browsing && webView != null
                && webView.getVisibility() == View.VISIBLE && customVideoView == null
                && !settingsOpen && !quickBarOpen && !tabsOpen
                && !browserBar.isAddressFocused();
        boolean visible = pageVisible && settings.isPointerEnabled() && settings.visible;
        pointerView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void resetPointerToCenter() {
        if (pointerView == null) return;
        pointerView.post(() -> {
            if (pointerView.getWidth() <= 0 || pointerView.getHeight() <= 0) return;
            cursorX = pointerView.getWidth() / 2f;
            cursorY = pointerView.getHeight() / 2f;
            pointerView.setCursorPosition(cursorX, cursorY);
        });
    }

    private void movePointer(int keyCode, int repeatCount) {
        if (pointerView == null) return;
        if (pointerView.getWidth() <= 0 || pointerView.getHeight() <= 0) {
            resetPointerToCenter();
            return;
        }
        float repeatFactor = 1f + Math.min(repeatCount, 12) * (settings.acceleration / 100f);
        float modeFactor = PointerSettings.MODE_TRACKPAD.equals(settings.mode) ? 1.65f : 1f;
        float distance = TvUi.dp(this, settings.speed) * repeatFactor * modeFactor;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) cursorX -= distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) cursorX += distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) cursorY -= distance;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) cursorY += distance;

        float margin = TvUi.dp(this, 8);
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
        if (webView == null || webView.getVisibility() != View.VISIBLE
                || cursorTouchDownTime >= 0L) return;
        // The native WebView only accepts synthetic touches while it owns focus.
        webView.requestFocus();
        dispatchPointerHover(eventTime);
        cursorTouchDownTime = eventTime;
        dispatchPointerTouch(MotionEvent.ACTION_DOWN, eventTime);
        if (settings.clickFeedback && pointerView != null) pointerView.showClickFeedback();
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
        MotionEvent motion = buildTouchEvent(action, x, y, downTime, eventTime);
        webView.dispatchTouchEvent(motion);
        motion.recycle();
    }

    /**
     * Builds a full touch event. The old code used the short MotionEvent.obtain
     * overload, which leaves the source as SOURCE_UNKNOWN; WebView ignores those
     * events on many Android TV builds.
     */
    private MotionEvent buildTouchEvent(int action, float x, float y,
                                        long downTime, long eventTime) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[1];
        properties[0] = new MotionEvent.PointerProperties();
        properties[0].id = 0;
        properties[0].toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[1];
        coordinates[0] = new MotionEvent.PointerCoords();
        coordinates[0].x = x;
        coordinates[0].y = y;
        coordinates[0].pressure = 1f;
        coordinates[0].size = 1f;

        return MotionEvent.obtain(
                downTime, eventTime, action, 1, properties, coordinates,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    private boolean isPagePointerTarget() {
        return browsing && webView != null && webView.getVisibility() == View.VISIBLE
                && customVideoView == null && !settingsOpen && !quickBarOpen && !tabsOpen
                && !browserBar.isAddressFocused();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean firstPress = down && event.getRepeatCount() == 0;

        if (firstPress && (keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_TV_CONTENTS_MENU
                || keyCode == KeyEvent.KEYCODE_SEARCH)) {
            if (settingsOpen) {
                closeSettings();
            } else if (tabsOpen) {
                closeTabSwitcher();
            } else {
                toggleQuickBar();
            }
            return true;
        }

        if (firstPress && keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (settingsOpen) {
                closeSettings();
            } else {
                openSettings();
            }
            return true;
        }

        if (handleTextEntryKey(event)) {
            return true;
        }

        if (isPagePointerTarget() && settings.isPointerEnabled() && settings.visible) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (down) {
                    movePointer(keyCode, event.getRepeatCount());
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_BUTTON_A
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (down) {
                    if (event.getRepeatCount() == 0) beginPointerTouch(event.getEventTime());
                } else {
                    endPointerTouch(event.getEventTime());
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * TV remotes send OK as DPAD_CENTER, which an EditText will not turn into a
     * click or an IME action. Intercept it so OK opens the keyboard, and let a
     * hardware Enter submit the field.
     */
    private boolean handleTextEntryKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (!down) return false;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (homeView != null && homeView.getVisibility() == View.VISIBLE
                    && homeView.isSearchFocused()) {
                homeView.submitSearch();
                return true;
            }
            if (browserBar != null && browserBar.isAddressFocused()) {
                browserBar.submitAddress();
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getRepeatCount() == 0) {
            if (homeView != null && homeView.getVisibility() == View.VISIBLE
                    && homeView.isSearchFocused()) {
                homeView.openSearchKeyboard();
                return true;
            }
            if (browserBar != null && browserBar.isAddressFocused()) {
                browserBar.focusAddress();
                refreshPointerVisibility();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (quickBarOpen) {
            closeQuickBar();
            return;
        }
        if (tabsOpen) {
            closeTabSwitcher();
            return;
        }
        if (settingsOpen) {
            closeSettings();
            return;
        }
        if (customVideoView != null) {
            exitFullscreenVideo();
            return;
        }
        if (browserBar.isAddressFocused()) {
            TvUi.hideKeyboard(browserBar.addressField());
            if (webView != null) webView.requestFocus();
            refreshPointerVisibility();
            return;
        }
        if (browsing) {
            releasePointerTouch();
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                showHome();
            }
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
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
        if (popupWebView != null) {
            popupWebView.destroy();
            popupWebView = null;
        }
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
