package mi.rssKoelbl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DisplayWebsite extends Activity {

	private WebView webview;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.website_view);
         
        Intent in = getIntent();
        String page_url = in.getStringExtra("page_url");
         
        webview = (WebView) findViewById(R.id.website);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(page_url);
         
        webview.setWebViewClient(new DisPlayWebPageActivityClient());
    }
     
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
     
    private class DisPlayWebPageActivityClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
