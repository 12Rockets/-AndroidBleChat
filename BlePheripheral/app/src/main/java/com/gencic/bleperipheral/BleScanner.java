package com.gencic.bleperipheral;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by ngencic on 13.9.15..
 */
public class BleScanner {

    static final long SCAN_TIMEOUT = 5000l;

    private BluetoothManager mBluetoothManager;
    private HashMap<String, BluetoothDevice> mDiscoveredDevices;
    private BluetoothLeScanner mScanner;
    private ILogger mLogger;
    private Context mContext;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    public BleScanner(Context context, BluetoothManager bluetoothManager) {
        mDiscoveredDevices = new HashMap<>();
        mBluetoothManager = bluetoothManager;
        mContext = context;
    }

    public void setLogger(ILogger logger){
        mLogger = logger;
    }

    public void startScanning(){
        if (mBluetoothManager.getAdapter().isEnabled()) {
            mDiscoveredDevices.clear();
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            filterBuilder.setServiceUuid(new ParcelUuid(UUID.fromString(Constants.CHAT_SERVICE_UUID)));
            ScanFilter filter = filterBuilder.build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(filter);
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            ScanSettings settings = settingsBuilder.build();
            mScanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
            mScanner.startScan(filters, settings, mScanCallback);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDiscoveredDevices.clear();
                    mScanner.stopScan(mScanCallback);
                }
            }, SCAN_TIMEOUT);
        } else {
            if (mLogger != null) {
                mLogger.log("Bluetooth is disabled!");
            }
        }
    }

    private void connectToGattServer(BluetoothDevice device){
        device.connectGatt(mContext, false, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                mGatt = gatt;
                for (int i = 0; i < gatt.getServices().size(); i++) {
                    BluetoothGattService service = gatt.getServices().get(i);
                    if (mLogger != null) {
                        mLogger.log("Service discovered: " + service.getUuid());
                    }
                    if (service.getUuid().equals(UUID.fromString(Constants.CHAT_SERVICE_UUID))) {
                        for (int j = 0; j < service.getCharacteristics().size(); j++) {
                            mCharacteristic = service.getCharacteristics().get(j);
                            if (mLogger != null) {
                                mLogger.log("Characteristic discovered: " + mCharacteristic.getUuid());
                            }
                            gatt.setCharacteristicNotification(mCharacteristic, true);
                        }
                    }
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                mGatt = gatt;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (mLogger != null) {
                        mLogger.log("Connected to Gatt Server");
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (mLogger != null) {
                        mLogger.log("Disconnected from Gatt Server");
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if (mLogger != null) {
                    mLogger.log("Characteristic changed value: " + characteristic.getStringValue(0));
                }
            }
        });
    }

    public void sendMessage(String msg) {
        if (mCharacteristic != null) {
            mCharacteristic.setValue(msg);
            mGatt.writeCharacteristic(mCharacteristic);
        }
    }

    public void destroy() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice() != null) {
                mScanner.stopScan(this);
                if (!mDiscoveredDevices.containsKey(result.getDevice().getAddress())) {
                    mDiscoveredDevices.put(result.getDevice().getAddress(), result.getDevice());
                    if (mLogger != null) {
                        mLogger.log("Discovered device: " + result.getDevice());
                    }
                    connectToGattServer(result.getDevice());
                }
            }
        }
    };

}
