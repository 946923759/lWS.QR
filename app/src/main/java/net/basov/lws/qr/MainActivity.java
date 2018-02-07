/**
MIT License

Copyright (c) 2018 Mikhail Basov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */
package net.basov.lws.qr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.util.Log;

public class MainActivity extends Activity {
    private WebView mainUI_WV;
    
    static String size = "256";
    static String dark = "#000";
    static String light = "#e0ffff";
    static String corr = "L";
    static String txt = "";
    static Boolean interactive = true;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.webview_ui);
        mainUI_WV = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mainUI_WV.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        /* Enable JavaScript */
        webSettings.setJavaScriptEnabled(true);
        //mainUI_WV.addJavascriptInterface(new WebViewJSCallback(this), "Android");
        /* Show external page in browser */
        mainUI_WV.setWebViewClient(new MyWebViewClient());
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            /* Handle JavaScript console log */
            mainUI_WV.setWebChromeClient(new myWebChromeClient());
            /* Enable chome remote debuging for WebView (Ctrl-Shift-I) */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { 
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        onNewIntent(getIntent());

        //ui.setWelcome("w_debug", "[onCreate()]");
        //ui.displayWelcomeScreen(mainUI_WV);
        mainUI_WV.loadUrl("file:///android_asset/" + getString(R.string.welcome_ui_file));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);
        if (intent == null) return;
        if (intent.getAction().equals("net.basov.lws.qr.ENCODE")) {
            if (intent != null) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    if (extras.getString("ENCODE_DATA") != null) interactive = false;
                    txt = extras.getString("ENCODE_DATA", "");
                    size = extras.getString("ENCODE_SIZE", "256");
                    dark = extras.getString("ENCODE_DARK", "#e0ffff");
                    light = extras.getString("ENCODE_LIGHT", "#000");
                    corr = extras.getString("ENCODE_CORRECTION", "L");
                }
            }
        } else if (intent.getAction().equals("android.intent.action.MAIN")) {
            interactive = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public static String escapeCharsForJSON(String src) {
        return src
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("'", "&#39;")
                .replace("\"", "\\\"");
    }

    /**
     * Redirect external URLs to browser
     */
    public static class MyWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            return processUri(uri, view);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return processUri(uri, view);
        }

        private boolean processUri(final Uri uri, WebView view) {
            if (uri.getHost().length() == 0) {
                return false;
            } else {
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                view.getContext().startActivity(intent);
                return true;
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String js = "";
            if(interactive) {
                js = "document.getElementById('interactive').style.display = 'block';"
                + "welcome = '" + escapeCharsForJSON(view.getContext().getString(R.string.welcome_msg)) + "';";        
            } else {
                js = ""        
                + "document.getElementById('size').value = '" + size +"';"
                + "document.getElementById('dark').value = '" + dark + "';"
                + "document.getElementById('light').value = '" + light + "';"
                + "document.getElementById('corr').value = '" + corr +"';"
                + "document.getElementById('qrtext').value = '" + escapeCharsForJSON(txt) + "';"
                + "document.getElementById('text').innerHTML = '" + escapeCharsForJSON(txt) + "';"
                + "document.getElementById('unattended').style.display = 'block';";
            }
            js += "refresh();";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.evaluateJavascript("javascript:" + js,null);
            } else {
                view.loadUrl("javascript:" + js,null);
            }
            view.clearCache(true);
            view.clearHistory();
        }
    }
    
    /**
     * Handle JavaScript console log
     */
    private class myWebChromeClient extends WebChromeClient {

        public boolean onConsoleMessage(ConsoleMessage cm) {
            final String TAG = "lWS.QR";
            String formattedMessage =
                cm.message()
                + " -- From line: "
                + cm.lineNumber()
                + " of "
                + cm.sourceId();
            switch (cm.messageLevel()) {
                case DEBUG:
                    Log.d(TAG, formattedMessage);
                    break;
                case ERROR:
                    Log.e(TAG, formattedMessage);
                    break;
                case LOG:
                    Log.i(TAG, formattedMessage);
                    break;
                case TIP:
                    Log.i(TAG, formattedMessage);
                    break;
                case WARNING:
                    Log.v(TAG, formattedMessage);
                    break;
                default:
                    Log.w(TAG, formattedMessage);
                    break;
            }
            return true;
        }
    }
}
