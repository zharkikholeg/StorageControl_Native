package ru.partner.storagecontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
//import androidx.webkit.WebViewAssetLoader;
//import androidx.webkit.WebViewClientCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;

//import org.xwalk.core.XWalkActivity;
//import org.xwalk.core.XWalkPreferences;
//import org.xwalk.core.XWalkView;

public class MainActivity extends Activity implements BarcodeReader.BarcodeListener, IBarcodeListenerCustom {
    private WebView mWebView;
    private View mDecorView;
    private JavaScriptInterface javaScriptIntarface;

    private AidcManager manager;
    private BarcodeScanner barcodeScanner;
    private BarcodeScannerM3 barcodeScannerM3;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if (android.os.Build.VERSION.SDK_INT > 9)
//        {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }
        mWebView = findViewById(R.id.mw_webview);
//        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
//                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
//                .build();
//        mWebView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
        WebSettings webSettings = mWebView.getSettings(); // получаем объект настроек WebView
        webSettings.setJavaScriptEnabled(true); // включаем js
        webSettings.setSupportZoom(false);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // включаем возможность дебага черех Chrome Devtools
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.clearCache(false);
        mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);; // по идее, должно решить проблему с тем, что с https-страницы идут http-запросы
        javaScriptIntarface = new JavaScriptInterface(mWebView, this); // создаём объект javascript-интерфейса
        mWebView.addJavascriptInterface(javaScriptIntarface, "androidInterface"); // добавляем этот объект интерфейса в вебвью
        //mWebView.loadUrl("file:///android_asset/test/index.html");
        mWebView.loadUrl("file:///android_asset/www/sc.html");
        mDecorView = getWindow().getDecorView();
        //hideSystemUI();
        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) { // The system bars are visible
                    //hideSystemUI();
                }
            }
        });
        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeScanner = new BarcodeScanner(manager, MainActivity.this);
                barcodeScanner.claim();
            }
        });

        barcodeScannerM3 = new BarcodeScannerM3(this, this);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            //hideSystemUI();
        }
    }

    private void hideSystemUI(){
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        mDecorView.setSystemUiVisibility(flags);
    }

    public void startScanningBarCode(int scanType) {
        if (scanType == ScanType.Scanner.getValue() && barcodeScanner != null) {
            barcodeScanner.on();
        } else if (scanType == ScanType.Scanner.getValue() && barcodeScannerM3 != null) {
            barcodeScannerM3.on();
        } else if (scanType == ScanType.Camera.getValue()) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
            integrator.setPrompt(this.getString(R.string.msg_scan_barcode));
            integrator.setCaptureActivity(QRReaderActivity.class);
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.setTimeout(45000);
            integrator.setCameraId(0);  // Use a specific camera of the device
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
        };
    }

    public void stopScanningBarCode(int scanType) {
        if(scanType == ScanType.Scanner.getValue() && barcodeScanner != null) {
            barcodeScanner.off();
        }else if (scanType == ScanType.Scanner.getValue() && barcodeScannerM3 != null) {
            barcodeScannerM3.off();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(scanningResult != null){	// выход из сканирования
            if (resultCode == RESULT_OK) {
                String barCode = scanningResult.getContents();
                javaScriptIntarface.scanningBarCodeResult(barCode);
            } else if (resultCode == RESULT_CANCELED) {
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeScanner != null) {
            barcodeScanner.claim();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.off();
            // release the scanner claim so we don't get any scanner
            // notifications while paused.
            barcodeScanner.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            // close BarcodeReader to clean up resources.
            barcodeScanner.close();
            barcodeScanner = null;
        }

        if (barcodeScannerM3 != null) {
            barcodeScannerM3.close();
            barcodeScannerM3 = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }

    public boolean checkBarcodeScanner() {
        return barcodeScanner != null || barcodeScannerM3 != null;
    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
        Log.d("Scanner", barcodeReadEvent.getBarcodeData());
        String barcode = barcodeReadEvent.getBarcodeData();
        barcode = barcode.replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
        javaScriptIntarface.scanningBarCodeResult(barcode);
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        javaScriptIntarface.scanningBarCodeFailure();
        Log.d("Scanner", "onFailureEvent");
    }

    @Override
    public void onBarcodeEvent(String barCode, String type) {
        Log.d("Scanner", barCode);
        Log.d("Scanner", type);
        String barcode = barCode.replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
        javaScriptIntarface.scanningBarCodeResult(barcode);
    }

    @Override
    public void onFailureEvent() {
        javaScriptIntarface.scanningBarCodeFailure();
    }

    // отключаем кнопку "назад"
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        } else {
            return true;
        }
    }
}