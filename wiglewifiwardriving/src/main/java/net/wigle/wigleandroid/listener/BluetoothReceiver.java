package net.wigle.wigleandroid.listener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import net.wigle.wigleandroid.DatabaseHelper;
import net.wigle.wigleandroid.MainActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.adrecord.AdRecord;
import uk.co.alt236.bluetoothlelib.device.adrecord.AdRecordStore;

/**
 * Created by bobzilla on 12/20/15
 */
public final class BluetoothReceiver extends BroadcastReceiver {
    private MainActivity mainActivity;
    private final DatabaseHelper dbHelper;
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private final ScanCallback scanCallback;

    public BluetoothReceiver(final MainActivity mainActivity, final DatabaseHelper dbHelper ) {
        this.mainActivity = mainActivity;
        this.dbHelper = dbHelper;

        if (Build.VERSION.SDK_INT >= 21) {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    // MainActivity.info("LE scanResult: " + scanResult + " callbackType: " + callbackType);
                    handleScanResult(scanResult);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    // MainActivity.info("LE results: " + results);
                    for (final ScanResult scanResult : results) {
                        handleScanResult(scanResult);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    switch (errorCode) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            MainActivity.info("BluetoothLEScan already started");
                            break;
                        default:
                            MainActivity.error("Bluetooth scan error: " + errorCode);
                            scanning.set(false);
                    }
                }
            };
        }
        else {
            scanCallback = null;
        }
    }

    private void handleScanResult(final ScanResult scanResult) {
        if (Build.VERSION.SDK_INT >= 21) {
            MainActivity.info("LE scanResult: " + scanResult);
            final ScanRecord scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                final BluetoothDevice device = scanResult.getDevice();
                BluetoothUtil.BleAdvertisedData adData = BluetoothUtil.parseAdertisedData(scanRecord.getBytes());
                MainActivity.info("LE deviceName: " + scanRecord.getDeviceName()
                        + " address: " + device.getAddress()
                        + " name: " + device.getName()
                        + " adName: " + adData.getName()
                        + " bytes: " + Arrays.toString(scanRecord.getBytes()));

                final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, scanResult.getRssi(),
                        scanRecord.getBytes(), System.currentTimeMillis());
                final AdRecordStore adRecordStore = deviceLe.getAdRecordStore();
                for (int i = 0; i < 200; i++) {
                    if (!adRecordStore.isRecordPresent(i)) {
                        continue;
                    }
                    final AdRecord adRecord = adRecordStore.getRecord(i);
                    MainActivity.info("LE adRecord(" + i + "): " + adRecord);
                }

            }
        }
    }

    public void setMainActivity( final MainActivity mainActivity ) {
        this.mainActivity = mainActivity;
    }

    public void bluetoothScan() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        }

        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
        }

        if (Build.VERSION.SDK_INT >= 21) {
            final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                MainActivity.info("bluetoothLeScanner is null");
            }
            else {
                if (scanning.compareAndSet(false, true)) {
                    final ScanSettings.Builder scanSettingsBulder = new ScanSettings.Builder();
                    scanSettingsBulder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
                    bluetoothLeScanner.startScan(
                            Collections.<ScanFilter>emptyList(), scanSettingsBulder.build(), scanCallback);

                } else {
                    bluetoothLeScanner.flushPendingScanResults(scanCallback);
                }
            }
        }
    }

    public void stopScanning() {
        if (Build.VERSION.SDK_INT >= 21) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner != null && scanning.compareAndSet(true, false)) {
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            MainActivity.info("BT bluetooth name: " + device.getName() + " address: " + device.getAddress()
                    + " bluetoothClass: " + device.getBluetoothClass() + " bondState: " + device.getBondState());
            if (Build.VERSION.SDK_INT >= 18) {
                MainActivity.info("BT bluetooth type: " + device.getType());
            }
            if (Build.VERSION.SDK_INT >= 15) {
                MainActivity.info("BT bluetooth uuids: " + Arrays.toString(device.getUuids()));
            }

        }
    }
}
