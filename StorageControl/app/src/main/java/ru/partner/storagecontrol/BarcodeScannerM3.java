package ru.partner.storagecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.m3.sdk.scannerlib.Barcode;
import com.m3.sdk.scannerlib.BarcodeListener;
import com.m3.sdk.scannerlib.BarcodeManager;

import net.m3mobile.app.scannerservicez2d.IScannerServiceZebra2D;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * SDK information http://dev.m3mobile.net/#/?id=introduction
 */

public class BarcodeScannerM3 implements ServiceConnection {
    private Barcode mBarcode = null;
    private BarcodeListener mListener = null;
    private BarcodeManager mManager = null;
    private Barcode.Symbology mSymbology = null;
    private IScannerServiceZebra2D m2DService = null;
    private Context mContext = null;
    private IBarcodeListenerCustom mListenerCustom = null;

    private Timer timer;
    private final int BAR_READ_TIMEOUT = 10000;

    private String prefix = "_start_";
    private String postfix = "_end_";

    private static String TAG = "BarcodeScannerM3";

    public BarcodeScannerM3(Context mContext, final IBarcodeListenerCustom mListenerCustom) {
        this.mContext = mContext;
        this.mListenerCustom = mListenerCustom;
        mBarcode = new Barcode(mContext);
        mManager = new BarcodeManager(mContext);
        mSymbology = mBarcode.getSymbologyInstance();

        bindScannerService();

        mListener = new BarcodeListener() {

            @Override
            public void onGetSymbology(int nSymbol, int nVal) {
                Log.i("ScannerTest", "onGetSymbology result="+nSymbol + ", "+ nVal);
            }

            @Override
            public void onBarcode(String strBarcode) {
                Log.i(TAG,"result="+strBarcode);
            }

            @Override
            public void onBarcode(String barcode, String codeType) {
                Log.i(TAG,"result="+barcode);
                stopScanTimer();
                Pattern pattern = Pattern.compile(prefix + "(.+?)" + postfix);
                Matcher m = pattern.matcher(barcode);
                m.find();
                mListenerCustom.onBarcodeEvent(m.group(1), codeType);
            }

        };

        mManager.addListener(mListener);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected " + iBinder.getClass().getSimpleName());
        m2DService = IScannerServiceZebra2D.Stub.asInterface(iBinder);
        try {
            /**
             * end char mode
             public static final int END_CHAR_ENTER = 0;
             public static final int END_CHAR_SPACE = 1;
             public static final int END_CHAR_TAB = 2;
             public static final int END_CHAR_KEY_ENTER = 3;
             public static final int END_CHAR_KEY_SPACE = 4;
             public static final int END_CHAR_KEY_TAB = 5;
             public static final int END_CHAR_NONE = 6;
             */
            m2DService.setEndCharMode(6);
            m2DService.setPrefix(prefix);
            m2DService.setPostfix(postfix);
            /**
             * SOUND MODE
             public static final int SOUND_NONE = 0;
             public static final int SOUND_BEEP = 1;
             public static final int SOUND_DING_DONG=2;
             */
            m2DService.setSoundMode(0); // Set 1 if sound needed
            m2DService.setVibration(true);

            /**
             * OUTPUT MODE
             public static final int OUTPUT_DIRECT = 0;
             public static final int OUTPUT_EMU_KEY = 1;
             public static final int OUTPUT_CLIPBOARD = 2;
             */
            m2DService.setOutputMode(0);
            /**
             * READ MODE
             public static final int READ_ASYNC = 0;
             public static final int READ_SYNC = 1;
             public static final int READ_CONTINUE = 2;
             */
            m2DService.setReadMode(1);


            /**
             * setScannerTriggerMode
             0 : enable
             1 : can't use scanner key
             2 : using only calling api
             */
            m2DService.setScannerTriggerMode(2);

            setParam(136, 99); // Set session timeout 9.9 s
            setParam(7, 1); // Enable Codabar
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected " + componentName.getClassName());
    }

    public void bindScannerService(){
        Intent intent = null;
        intent = new Intent("net.m3mobile.app.scannerservicezebra2d.start");
        intent.setPackage(get2DAIDLPackageName());
        boolean bBind = mContext.bindService(intent,this, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
        Log.d(TAG, "bindScannerService " + bBind);
    }

    public void close() {
        off();
        mContext.unbindService(this);
        mManager.removeListener(mListener);
        mManager.dismiss();
    }

    private String get2DAIDLPackageName() {
        String packageName = "net.m3mobile.app.scanemul";
        PackageManager manager = mContext.getPackageManager();
        List<ApplicationInfo> list = manager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : list) {
            try {
                Log.d(TAG, "info.packageName : " + info.packageName);
                if(info.packageName.equals("net.m3mobile.app.scannerservicez2d")) {
                    packageName = "net.m3mobile.app.scannerservicez2d";
                }
                if(Build.MODEL.contains("SL10")){
                    packageName = "com.zebra.scanner";
                }

            } catch (Exception e) {
                Log.e(TAG, "getPackageType Exception : " + e.getMessage());
            }
        }
        return packageName;
    }

    public void on() {
        try {
            m2DService.decodeStart();
            startScanTimer();
        } catch (RemoteException e) {
            Log.d(TAG, "decodeStart error");
            e.printStackTrace();
        }

    }

    public void off() {
        try {
            m2DService.decodeStop();
            stopScanTimer();
        } catch (RemoteException e) {
            Log.d(TAG, "decodeStop error");
            e.printStackTrace();
        }
    }

    public void setParam(int paramNum, int value) {
        try {
            int nResult = m2DService.setScanParameter(paramNum,  value);
        } catch (RemoteException e) {
            Log.d(TAG, "setParam error");
            e.printStackTrace();
        }
    }

    private void startScanTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                mListenerCustom.onFailureEvent();
            }
        }, BAR_READ_TIMEOUT);
    }

    private void stopScanTimer() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
