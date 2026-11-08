package com.gobrowser.tv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny persistence layer for history and bookmarks.
 *
 * <p>Records are stored as a single delimited string so the app does not need a
 * database or a JSON dependency for a handful of entries. The delimiter is the
 * ASCII unit-separator character, which cannot appear in a URL or page title in
 * practice.</p>
 */
public final class BrowserStore {
    public static final class Entry {
        public final String title;
        public final String url;

        Entry(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    private static final String PREFS = "go_browser_data";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final char FIELD = '\u001F';
    private static final char RECORD = '\u001E';
    private static final int MAX_HISTORY = 16;

    private final SharedPreferences prefs;

    public BrowserStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Entry> history() {
        return decode(prefs.getString(KEY_HISTORY, ""));
    }

    public List<Entry> bookmarks() {
        return decode(prefs.getString(KEY_BOOKMARKS, ""));
    }

    public void addHistory(String title, String url) {
        if (url == null || url.isEmpty()) return;
        if (url.startsWith("about:") || url.startsWith("data:")) return;
        List<Entry> list = history();
        removeByUrl(list, url);
        list.add(0, new Entry(cleanTitle(title, url), url));
        while (list.size() > MAX_HISTORY) {
            list.remove(list.size() - 1);
        }
        prefs.edit().putString(KEY_HISTORY, encode(list)).apply();
    }

    public boolean isBookmarked(String url) {
        if (url == null) return false;
        for (Entry entry : bookmarks()) {
            if (entry.url.equals(url)) return true;
        }
        return false;
    }

    public boolean toggleBookmark(String title, String url) {
        if (url == null || url.isEmpty()) return false;
        List<Entry> list = bookmarks();
        if (removeByUrl(list, url)) {
            prefs.edit().putString(KEY_BOOKMARKS, encode(list)).apply();
            return false;
        }
        list.add(0, new Entry(cleanTitle(title, url), url));
        prefs.edit().putString(KEY_BOOKMARKS, encode(list)).apply();
        return true;
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private boolean removeByUrl(List<Entry> list, String url) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    private String cleanTitle(String title, String url) {
        String value = title == null ? "" : title.trim();
        if (value.isEmpty()) {
            try {
                String host = android.net.Uri.parse(url).getHost();
                value = host == null ? url : host;
            } catch (Exception ignored) {
                value = url;
            }
        }
        return value;
    }

    private String encode(List<Entry> entries) {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            builder.append(entry.title).append(FIELD)
                    .append(entry.url).append(RECORD);
        }
        return builder.toString();
    }

    private List<Entry> decode(String raw) {
        List<Entry> entries = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return entries;
        String[] records = raw.split(String.valueOf(RECORD));
        for (String record : records) {
            if (record.isEmpty()) continue;
            int split = record.indexOf(FIELD);
            if (split <= 0) continue;
            String title = record.substring(0, split);
            String url = record.substring(split + 1);
            if (!url.isEmpty()) entries.add(new Entry(title, url));
        }
        return entries;
    }
}
