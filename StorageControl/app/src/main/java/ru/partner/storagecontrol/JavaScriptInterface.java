package ru.partner.storagecontrol;

import android.os.Handler;

import org.xwalk.core.JavascriptInterface;
import org.xwalk.core.XWalkView;

public class JavaScriptInterface {
    private XWalkView webView;
    private MainActivity mainActivity;
    private Handler interfaceHandler;

    JavaScriptInterface(XWalkView webView, MainActivity mainActivity) {
        this.webView = webView;
        this.mainActivity = mainActivity;
        interfaceHandler = new Handler();
    }

    @JavascriptInterface
    public int checkCameraPermissionGranted() {
        return App.isCameraPermissionGranted() ? 1 : 0;
    }

    @JavascriptInterface
    public int checkBarcodeScanner() {
        return mainActivity.checkBarcodeScanner() ? 1 : 0;
    }

    @JavascriptInterface
    public void startScanningBarCode(final int scanType){
        interfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                mainActivity.startScanningBarCode(scanType);
            }
        });
    }

    @JavascriptInterface
    public void stopScanningBarCode(final int scanType){
        interfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                mainActivity.stopScanningBarCode(scanType);
            }
        });
    }

    public void scanningBarCodeResult(final String res) {
        interfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                callJS( String.format("javascript: SC.nativeInterface.scanningBarCodeResult('%s');", res) );
            }
        });
    }

    public void scanningBarCodeFailure() {
        interfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                callJS( String.format("javascript: SC.nativeInterface.scanningBarCodeFailure();"));
            }
        });
    }

    private void callJS(String js){
        webView.evaluateJavascript( js, null );
    }

}
