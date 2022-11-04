package ru.partner.storagecontrol;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class App extends Application {
    private static Context context;
    private Thread.UncaughtExceptionHandler baseUncaughtExceptionHandler;

    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
        baseUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException (Thread thread, Throwable e)	{
                Log.e("SC", Log.getStackTraceString(e));
                Logger.appendLog(e);
                baseUncaughtExceptionHandler.uncaughtException(thread, e);
            }
        });
    }

    public static Context getAppContext() {
        return App.context;
    }

    public static boolean isCameraPermissionGranted(){
        return ContextCompat.checkSelfPermission(getAppContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getExternalStorageDirectory(){
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getAppDirectory(){
        String dir = App.getAppContext().getExternalFilesDir(null).getAbsolutePath();
        return dir;
    }
}
