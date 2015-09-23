package com.gencic.bleperipheral;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.util.UUID;

/**
 * Created by ngencic on 13.9.15..
 */
public class BleAdvertiser {

    static final long ADVERTISE_TIMEOUT = 5000l;

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattserver;
    private BluetoothDevice mConnectedDevice;
    private ILogger mLogger;
    private Context mContext;

    public BleAdvertiser(Context context, BluetoothManager bluetoothManager) {
        mBluetoothManager = bluetoothManager;
        mContext = context;
    }

    public void setLogger(ILogger logger) {
        mLogger = logger;
    }

    public void startAdvertising() {
        if (mBluetoothManager.getAdapter().isEnabled()) {
            if (mBluetoothManager.getAdapter().isMultipleAdvertisementSupported()) {
                mGattserver = mBluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                        super.onConnectionStateChange(device, status, newState);
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            if (mLogger != null) {
                                mLogger.log("Client connected: " + device.getAddress());
                            }
                            mConnectedDevice = device;
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if (mLogger != null) {
                                mLogger.log("Client disconnected: " + device.getAddress());
                            }
                            mConnectedDevice = null;
                        }
                    }

                    @Override
                    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                        if (mLogger != null) {
                            mLogger.log("onCharacteristicReadRequest");
                        }
                    }

                    @Override
                    public void onServiceAdded(int status, BluetoothGattService service) {
                        super.onServiceAdded(status, service);
                        BleAdvertiser.this.onServiceAdded();
                    }

                    @Override
                    public void onNotificationSent(BluetoothDevice device, int status) {
                        super.onNotificationSent(device, status);
                        if (mLogger != null) {
                            mLogger.log("onNotificationSent");
                        }
                    }

                    @Override
                    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                        if (characteristic.getUuid().equals(UUID.fromString(Constants.CHAT_CHARACTERISTIC_UUID))) {
                            String msg = "";
                            if (value != null) {
                                msg = new String(value);
                            }
                            mLogger.log("onCharacteristicWriteRequest: " + msg);
                        }
                    }
                });
                mGattserver.addService(ServiceFactory.generateService());
            } else {
                mLogger.log("Central mode not supported by the device!");
            }
        } else {
            mLogger.log("Bluetooth is disabled!");
        }
    }

    private void onServiceAdded(){
        final BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        //dataBuilder.setManufacturerData(0, advertisingBytes);
        dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(Constants.CHAT_SERVICE_UUID)));
        //dataBuilder.setServiceData(pUUID, new byte[]{});

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

        bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            }
        }, ADVERTISE_TIMEOUT);
    }

    public void sendMessage(String msg) {
        if (mConnectedDevice != null) {
            BluetoothGattCharacteristic characteristic = ServiceFactory.generateService().getCharacteristic(UUID.fromString(Constants.CHAT_CHARACTERISTIC_UUID));
            characteristic.setValue(msg);
            mGattserver.notifyCharacteristicChanged(mConnectedDevice, characteristic, false);
        }
    }

    public void destroy() {
        if (mGattserver != null) {
            mGattserver.clearServices();
            mGattserver.close();
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (mLogger != null) {
                mLogger.log("Advertising started successfully");
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            if (mLogger != null) {
                mLogger.log("Advertising failed error code = " + errorCode);
            }
        }
    };

}
