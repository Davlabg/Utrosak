package com.davlabg.utrosak;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONArray;

import java.io.OutputStream;
import java.util.Map;

/**
 * Aplikacija "Utrošak": ekran je HTML stranica iz assets/index.html u WebView-u.
 * Podaci se čuvaju na telefonu (SharedPreferences), Excel se čuva preko sistemskog
 * prozora "Sačuvaj kao", a zbir se deli kao tekst (Viber, WhatsApp, e-mail…).
 */
public class MainActivity extends Activity {

    private static final int REQ_SAVE_XLSX = 41;
    private static final String PREFS = "utrosak_data";

    private WebView web;
    private SharedPreferences prefs;
    private byte[] pendingXlsx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Android 15+ crta aplikaciju ispod sistemskih traka: odmaknemo sadržaj od njih i od tastature.
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                return WindowInsets.CONSUMED;
            });
        }

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "Android");

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        // Sačuvaj odmah sve što je upisano, i kad se aplikacija samo skloni.
        web.evaluateJavascript("window.appFlush && appFlush()", null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.appBack ? appBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SAVE_XLSX) return;
        byte[] bytes = pendingXlsx;
        pendingXlsx = null;
        if (resultCode != RESULT_OK || data == null || data.getData() == null || bytes == null) return;
        Uri uri = data.getData();
        boolean ok;
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new java.io.IOException("no stream");
            out.write(bytes);
            ok = true;
        } catch (Exception e) {
            ok = false;
        }
        web.evaluateJavascript("window.onXlsxSaved && onXlsxSaved(" + ok + ")", null);
    }

    /** Funkcije koje HTML stranica poziva kao Android.xxx(...). */
    private class Bridge {

        @JavascriptInterface
        public String getItem(String key) {
            return prefs.getString(key, null);
        }

        @JavascriptInterface
        public void setItem(String key, String value) {
            prefs.edit().putString(key, value).apply();
        }

        @JavascriptInterface
        public void removeItem(String key) {
            prefs.edit().remove(key).apply();
        }

        @JavascriptInterface
        public String keys() {
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) arr.put(e.getKey());
            return arr.toString();
        }

        @JavascriptInterface
        public void saveXlsx(String filename, String base64) {
            final byte[] bytes;
            try {
                bytes = Base64.decode(base64, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                return;
            }
            runOnUiThread(() -> {
                pendingXlsx = bytes;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                i.putExtra(Intent.EXTRA_TITLE, filename);
                try {
                    startActivityForResult(i, REQ_SAVE_XLSX);
                } catch (Exception e) {
                    pendingXlsx = null;
                    web.evaluateJavascript("window.onXlsxSaved && onXlsxSaved(false)", null);
                }
            });
        }

        @JavascriptInterface
        public void shareText(String subject, String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, subject);
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, subject));
            });
        }
    }
}
