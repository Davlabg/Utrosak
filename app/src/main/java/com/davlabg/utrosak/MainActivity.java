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
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
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
    /** Nalog za slanje mejla; odvojeno od podataka, lozinka se nikad ne vraća stranici. */
    private static final String SENDER_PREFS = "utrosak_sender";
    public static final String DEFAULT_SENDER = "utrosak.cw.restoran@gmail.com";

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
        public String getSenderEmail() {
            return getSharedPreferences(SENDER_PREFS, MODE_PRIVATE).getString("email", DEFAULT_SENDER);
        }

        @JavascriptInterface
        public boolean hasSenderPassword() {
            return !getSharedPreferences(SENDER_PREFS, MODE_PRIVATE).getString("password", "").isEmpty();
        }

        /** Prazna lozinka znači: zadrži postojeću. */
        @JavascriptInterface
        public void setSender(String email, String password) {
            SharedPreferences.Editor e = getSharedPreferences(SENDER_PREFS, MODE_PRIVATE).edit();
            e.putString("email", email.trim());
            String p = password.replaceAll("\\s+", "");
            if (!p.isEmpty()) e.putString("password", p);
            e.apply();
        }

        @JavascriptInterface
        public void clearSenderPassword() {
            getSharedPreferences(SENDER_PREFS, MODE_PRIVATE).edit().remove("password").apply();
        }

        /** Šalje odmah preko Gmail-a, u pozadini. Rezultat stiže u window.onAutoMailResult(ok, poruka). */
        @JavascriptInterface
        public void sendAuto(String toCsv, String subject, String body, String filename, String base64) {
            SharedPreferences sp = getSharedPreferences(SENDER_PREFS, MODE_PRIVATE);
            final String user = sp.getString("email", DEFAULT_SENDER);
            final String pass = sp.getString("password", "");
            final java.util.List<String> to = new java.util.ArrayList<>();
            for (String r : toCsv.split(",")) if (!r.trim().isEmpty()) to.add(r.trim());
            final byte[] bytes = base64 == null || base64.isEmpty() ? null : Base64.decode(base64, Base64.DEFAULT);
            new Thread(() -> {
                boolean ok;
                String msg;
                if (pass.isEmpty()) {
                    ok = false;
                    msg = "Nije upisana lozinka za aplikaciju.";
                } else {
                    try {
                        SmtpSender.send(user, pass, to, subject, body,
                                filename.replaceAll("[^A-Za-z0-9._-]", "_"), bytes);
                        ok = true;
                        msg = "";
                    } catch (SmtpSender.MailException e) {
                        ok = false;
                        msg = e.getMessage();
                    } catch (Exception e) {
                        ok = false;
                        msg = "Slanje nije uspelo. Pokušajte ponovo.";
                    }
                }
                final String js = "window.onAutoMailResult && onAutoMailResult(" + ok + "," + JSONObject.quote(msg) + ")";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }).start();
        }

        /** Otvara mejl aplikaciju sa upisanim primaocima, naslovom, tekstom i Excel prilogom. */
        @JavascriptInterface
        public void sendEmail(String toCsv, String subject, String body, String filename, String base64) {
            final Uri uri;
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");
                File dir = AttachmentProvider.dir(MainActivity.this);
                File[] old = dir.listFiles();
                if (old != null) for (File f : old) f.delete();
                File f = new File(dir, safe);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(bytes);
                }
                uri = Uri.parse("content://" + AttachmentProvider.AUTHORITY + "/" + Uri.encode(safe));
            } catch (Exception e) {
                web.post(() -> web.evaluateJavascript("window.onMailResult && onMailResult(false)", null));
                return;
            }
            final String[] to = toCsv.split("\\s*,\\s*");
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(AttachmentProvider.MIME_XLSX);
                send.putExtra(Intent.EXTRA_EMAIL, to);
                send.putExtra(Intent.EXTRA_SUBJECT, subject);
                send.putExtra(Intent.EXTRA_TEXT, body);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(android.content.ClipData.newRawUri(filename, uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Ponudi samo mejl aplikacije.
                send.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
                try {
                    startActivity(send);
                } catch (Exception e) {
                    // Nema mejl aplikacije koja prihvata selektor: ponudi sve aplikacije za deljenje.
                    send.setSelector(null);
                    try {
                        startActivity(Intent.createChooser(send, subject));
                    } catch (Exception e2) {
                        web.evaluateJavascript("window.onMailResult && onMailResult(false)", null);
                    }
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
