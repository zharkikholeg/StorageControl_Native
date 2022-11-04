package ru.partner.storagecontrol;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureActivity;

public class QRReaderActivity extends CaptureActivity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TextView statusView = findViewById(R.id.zxing_status_view);
        statusView.setGravity(Gravity.CENTER);
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }
}
