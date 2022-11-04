package ru.partner.storagecontrol;

import android.util.Log;
import android.widget.Toast;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerNotClaimedException;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BarcodeScanner implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    private BarcodeReader barcodeReader;
    private BarcodeReader.BarcodeListener barcodeListener;
    private Timer timer;
    private final int BAR_READ_TIMEOUT = 45000;

    public BarcodeScanner(AidcManager manager, BarcodeReader.BarcodeListener barcodeListener) {
        this.barcodeListener = barcodeListener;
        barcodeReader = manager.createBarcodeReader();
        // register bar code event listener
        barcodeReader.addBarcodeListener(this);
        // set the trigger mode to client control
        try {
            barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                    BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);
        } catch (UnsupportedPropertyException e) {
            e.printStackTrace();
        }
        // register trigger state change listener
        barcodeReader.addTriggerListener(this);
        Map<String, Object> properties = new HashMap<String, Object>();
        // Set Symbologies On/Off
        properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
        properties.put(BarcodeReader.EANUCC_EMULATION_MODE_GS1_EAN8_TO_EAN13_CONVERSION ,true);
        properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
        //EAN_13
        properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);

        properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);

        // Set Max Code 39 barcode length
        properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
        // Turn on center decoding
        properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
        // Disable bad read response, handle in onFailureEvent
        properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, false);
        properties.put(BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER, false);
        // Apply the settings
        barcodeReader.setProperties(properties);
    }

    public void claim() {
        try {
            barcodeReader.claim();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
            // Scanner unavailable
        }
    }

    public void release() {
        barcodeReader.release();
    }

    public void close() {
        barcodeReader.close();
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        off();
        barcodeListener.onBarcodeEvent(event);
//        Log.d("Scanner", event.getBarcodeData());
    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent event) {
        Log.d("Scanner", "onTriggerEvent: " + Boolean.toString(event.getState()));
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent arg0) {
        off();
        barcodeListener.onFailureEvent(arg0);
    }

    public void barcodeTrigger(boolean on) {
        try {
            if (barcodeReader != null) {
                barcodeReader.aim(on);
                barcodeReader.light(on);
                barcodeReader.decode(on);
            }
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void on() {
        barcodeTrigger(true);
        timer = new Timer();

        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                off();
            }
        }, BAR_READ_TIMEOUT);
    }

    public void off() {
        barcodeTrigger(false);
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
