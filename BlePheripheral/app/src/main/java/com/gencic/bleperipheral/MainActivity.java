package com.gencic.bleperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, ILogger {

    private TextView mTextViewLog;
    private EditText mEditTextMsg;
    private BluetoothManager mBluetoothManager;
    private BleAdvertiser mAdvertiser;
    private BleScanner mBleScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewLog = (TextView) findViewById(R.id.text_view_log);
        mEditTextMsg = (EditText) findViewById(R.id.edit_text_msg);
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_scan).setOnClickListener(this);
        findViewById(R.id.button_send).setOnClickListener(this);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTextViewLog.setText("");
        if (mAdvertiser != null) {
            mAdvertiser.destroy();
        }
        if (mBleScanner != null) {
            mBleScanner.destroy();
        }
    }

    private void startAdvertising() {
        if (mAdvertiser == null){
            mAdvertiser = new BleAdvertiser(this, mBluetoothManager);
            mAdvertiser.setLogger(this);
        }
        mAdvertiser.startAdvertising();
    }

    private void startScanning() {
        if (mBleScanner == null){
            mBleScanner = new BleScanner(this, mBluetoothManager);
            mBleScanner.setLogger(this);
        }
        mBleScanner.startScanning();
    }

    public void log(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextViewLog.setText(msg + "\n" + mTextViewLog.getText());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_advertise:
                startAdvertising();
                break;
            case R.id.button_scan:
                startScanning();
                break;
            case R.id.button_send:
                if (mEditTextMsg.getText() != null) {
                    if (mBleScanner != null) {
                        mBleScanner.sendMessage(mEditTextMsg.getText().toString());
                    } else if (mAdvertiser != null) {
                        mAdvertiser.sendMessage(mEditTextMsg.getText().toString());
                    }
                }
                break;
        }
    }
}
