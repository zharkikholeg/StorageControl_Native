package ru.partner.storagecontrol;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Logger {

	private static boolean _active = false;

	private static String getDateTimeStamp()
	{
		Date dateNow = Calendar.getInstance().getTime();                            // My locale, so all the log files have the same date and time format
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dateNow);
	}

	public static void Activate(){
		_active = true;
	}

	public static void Deactivate(){
		_active = false;
	}
	
	public static void appendLog(String text)
	{
		if(!_active) return;
        String baseDir = App.getAppDirectory();
		String fileName = "sc.log";
	    File logFile = new File(baseDir + File.separator + fileName);
	    if (!logFile.exists())
	    {
	        try
	        {
	            logFile.createNewFile();
	        }
	        catch (IOException e)
	        {
				Log.e("UCS", Log.getStackTraceString(e));
	        }
	    }
	    try
	    {
		    String msg = String.format("%1s ---> %2s", getDateTimeStamp(), text);
	        //BufferedWriter for performance, true to set append to file flag
	        BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
	        buf.append(msg);
	        buf.newLine();
	        buf.close();
	    }
	    catch (IOException e)
	    {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}

	public static void appendLog(Object sender, String text){
		String name = sender.getClass().getSimpleName();
		appendLog(String.format("%s: %s", name, text));
	}

	public static void appendLog(Throwable thr){
		appendLog(Log.getStackTraceString(thr));
	}

	public static void appendLog(String text, Throwable thr){
		appendLog(text);
		appendLog(Log.getStackTraceString(thr));
	}
}
