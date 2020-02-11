package net.mikespub.mywebview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import net.mikespub.myutils.MyContentUtility;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Main Activity for Android App
 */
// See also Chrome Custom Tabs https://developer.chrome.com/multidevice/android/customtabs
// and Android Browser Helper https://github.com/GoogleChrome/android-browser-helper
public class MainActivity extends AppCompatActivity {

    // SavedStateViewModel see https://github.com/googlecodelabs/android-lifecycles/blob/master/app/src/main/java/com/example/android/lifecycles/step6_solution/SavedStateActivity.java
    // or with AndroidViewModel see https://github.com/husaynhakeem/Androidx-SavedState-Playground/blob/master/app/src/main/java/com/husaynhakeem/savedstateplayground/AndroidViewModelWithSavedState.kt
    //private MySavedStateModel mySavedStateModel;

    // https://developer.chrome.com/multidevice/webview/gettingstarted
    protected WebView myWebView;
    BroadcastReceiver onDownloadComplete;

    /**
     * @param savedInstanceState    saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
        //this.mySavedStateModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
        //HashMap<String, Object> hashMap = mySavedStateModel.getSettings(this);
        //Log.d("State Get", hashMap.toString());

        myWebView = findViewById(R.id.activity_main_webview);
        // Enable Javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // See https://ukacademe.com/MobileApplication/AndroidGUI/Android_WebView
        // webSettings.setBuiltInZoomControls(true);
        // Some other options - https://github.com/codepath/android_guides/wiki/Working-with-the-WebView
        // webSettings.setUseWideViewPort(true);
        // webSettings.setLoadWithOverviewMode(true);
        //MyContentUtility.showMyDownloadFiles(this);
        /*
        try {
            //JSONObject jsonObject = MyJsonUtility.mapToJson(defaultSettings);
            //JSONObject jsonObject = (JSONObject) MyJsonUtility.toJson(defaultSettings);
            //String jsonString = jsonObject.toString(2).replace("\\","");
            String jsonString = MyJsonUtility.toJsonString(defaultSettings);
            Log.d("WebView", jsonString);
        } catch (Exception e) {
            Log.e("WebView", e.toString());
        }
         */
        // myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        // myWebView.setScrollbarFadingEnabled(false);
        //MyReflectUtility.showObject(myWebView);
        // Stop local links and redirects from opening in browser instead of WebView
        MyAppWebViewClient myWebViewClient = new MyAppWebViewClient(this);
        //MyReflectUtility.showObject(myWebViewClient);
        if (myWebViewClient.hasDebuggingEnabled()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // Set custom WebSettings (if any)
        //MyReflectUtility.showObject(webSettings);
        myWebViewClient.setWebSettings(webSettings);
        myWebView.setWebViewClient(myWebViewClient);
        // Add Javascript interface
        if (myWebViewClient.hasJavascriptInterface()) {
            myWebView.addJavascriptInterface(new AppJavaScriptProxy(this, myWebView), "androidAppProxy");
            Log.d("WebView", "Enable Javascript interface");
        }
        // Show console log messages - see https://developer.android.com/guide/webapps/debugging
        if (myWebViewClient.hasConsoleLog()) {
            final MainActivity myActivity = this;
            myWebView.setWebChromeClient(new WebChromeClient() {
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    String message = cm.messageLevel() + " " + cm.message() + " -- From line "
                            + cm.lineNumber() + " of "
                            + cm.sourceId();
                    Log.d("WebView", message);
                    Toast toast = Toast.makeText(
                            myActivity.getApplicationContext(),
                            cm.message(),
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
            });
        }
        // Support context menu for links and images in WebView
        // WebViewClient myWebViewClient = myWebView.getWebViewClient();
        //registerForContextMenu(myWebView);
        if (myWebViewClient.hasContextMenu()) {
            //Log.d("WebView", Boolean.toString(myWebView.isLongClickable()));
            myWebView.setLongClickable(true);
            // See https://github.com/AriesHoo/FastLib/blob/dev/app/src/main/java/com/aries/library/fast/demo/module/WebViewActivity.java
            myWebView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    WebView.HitTestResult hitTestResult = myWebView.getHitTestResult();
                    if (hitTestResult == null) {
                        return false;
                    }
                    int getType = hitTestResult.getType();
                    String extra = hitTestResult.getExtra();
                    if (extra == null || extra.equals("")) {
                        return false;
                    }
                    Uri uri = Uri.parse(extra);
                    Intent intent;
                    Intent chooser;
                    switch (getType) {
                        case WebView.HitTestResult.IMAGE_TYPE:
                            Log.d("WebView", "image");
                            //showDownDialog(hitTestResult.getExtra());
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            //myWebView.getContext().startActivity(intent);
                            chooser = Intent.createChooser(intent, null);
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                            Log.d("WebView", "image anchor");
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            //myWebView.getContext().startActivity(intent);
                            chooser = Intent.createChooser(intent, null);
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                            Log.d("WebView", "anchor");
                            //intent = new Intent(Intent.ACTION_SEND, uri);
                            // See also https://github.com/codepath/android_guides/wiki/Sharing-Content-with-Intents
                            //intent = new Intent(Intent.ACTION_SEND);
                            //intent.putExtra(Intent.EXTRA_TEXT, uri.toString());
                            //intent.putExtra(Intent.EXTRA_TITLE, "Send Me");
                            //intent.setType("text/plain");
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            // Create intent to show chooser
                            String title = uri.toString() + "\n\nOpen with";
                            chooser = Intent.createChooser(intent, title);
                            //chooser = Intent.createChooser(intent, null);
                            //chooser.putExtra(Intent.EXTRA_TITLE, uri.toString() + "\n\nOpen with");
                            myWebView.getContext().startActivity(chooser);
                            return true;
                        default:
                            Log.d("WebView", "other " + getType);
                            break;
                    }

                    Log.d("WebView", "Type:" + hitTestResult.getType() + ";Extra:" + hitTestResult.getExtra());
                    //return true;
                    return false;
                }
            });
        }
        // https://stackoverflow.com/questions/36987144/preventing-webview-reload-on-rotate-android-studio/46849736#46849736
        if (savedInstanceState == null) {
            // myWebView.loadUrl("http://beta.html5test.com/");
            String myUrl = getString(R.string.website_url);
            myWebView.loadUrl(myUrl);
        } else {
            // Bundle bundle = savedInstanceState.getBundle("webViewState");
            // Log.d("Web Create", bundle.toString());
            // myWebView = findViewById(R.id.activity_main_webview);
            // Stop local links and redirects from opening in browser instead of WebView
            // myWebView.setWebViewClient(new MyAppWebViewClient(this));
            // myWebView.restoreState(bundle);
        }
        // https://stackoverflow.com/questions/19365668/getting-webview-history-from-webbackforwardlist
        // https://stackoverflow.com/questions/33326833/save-state-of-webview-when-switching-activity
    }

    /**
     * @return  ViewModel with Saved State for Settings
     */
    // https://stackoverflow.com/questions/57838759/how-android-jetpack-savedstateviewmodelfactory-works
    public MySavedStateModel getSavedStateModel() {
        return new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MySavedStateModel.class);
    }

    /**
     *
     */
    // Note: this is different from https://developer.android.com/guide/webapps/webview#java
    // and http://tutorials.jenkov.com/android/android-web-apps-using-android-webview.html
    @Override
    public void onBackPressed() {
        if(myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * @param overrideConfiguration configuration to override
     */
    // https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview
    @Override
    public void applyOverrideConfiguration(final Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 25) {
            overrideConfiguration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    /**
     * @param outState  instance state to save
     */
    // https://stackoverflow.com/questions/39086084/save-webview-state-on-screen-rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        myWebView.saveState(outState);
        Log.d("Web Save", outState.toString());
        //Bundle bundle = new Bundle();
        //myWebView.saveState(bundle);
        //Log.d("Web Save", bundle.toString());
        //outState.putBundle("webViewState", bundle);
    }

    /**
     * @param savedInstanceState    saved instance state
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Web Restore", savedInstanceState.toString());
        myWebView.restoreState(savedInstanceState);
        //Bundle bundle = savedInstanceState.getBundle("webViewState");
        //Log.d("Web Restore", bundle.toString());
        // myWebView.restoreState(bundle);
    }

    /**
     * Register BroadcastReceiver for Downloads
     */
    void startDownloadReceiver(BroadcastReceiver mReceiver) {
        stopDownloadReceiver();
        onDownloadComplete = mReceiver;
        Log.d("Web Create", "register receiver");
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Unregister BroadcastReceiver for Downloads
     */
    void stopDownloadReceiver() {
        if (onDownloadComplete != null) {
            Log.d("Web Create", "unregister receiver");
            unregisterReceiver(onDownloadComplete);
        }
    }
    /*
     * When the Activity of the app that hosts files sets a result and calls
     * finish(), this method is invoked. The returned Intent contains the
     * content URI of a selected file. The result code indicates if the
     * selection worked or not.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent returnIntent) {
        super.onActivityResult(requestCode, resultCode, returnIntent);
        // If the selection didn't work
        if (resultCode != RESULT_OK) {
            // Exit without doing anything else
            return;
        } else {
            // Get the file's content URI from the incoming Intent
            Uri returnUri = returnIntent.getData();
            MyContentUtility.showContent(this, returnUri);
            /*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            ParcelFileDescriptor inputPFD;
            try {
                /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                inputPFD = getContentResolver().openFileDescriptor(returnUri, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MainActivity", "File not found.");
                return;
            }
            // Get a regular file descriptor for the file
            FileDescriptor fd = inputPFD.getFileDescriptor();
            try {
                FileInputStream inputStream = new FileInputStream(fd);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        }
    }
    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDownloadReceiver();
    }
}
