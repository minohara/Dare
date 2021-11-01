package org.minohara.dare;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DataServer {
    private static final String TAG = "Dare";
    static final int REQUEST_BLUETOOTH_ADVERTISE = 1;
    static final int REQUEST_BLUETOOTH_CONNECT = 2;
    static final int REQUEST_BLUETOOTH_SCAN = 4;
    static final UUID SERVICE_UUID = UUID.fromString("18311939-d6d8-4b77-97e0-a4c323db95f4");
    static final UUID MESSAGE_UUID = UUID.fromString("f03fde31-928a-4c87-afff-6817e6ffe43a");
    static final UUID CONFIRM_UUID = UUID.fromString("855d6a12-478f-48f5-9171-b9e25800cb42");
    static int grantedRequest = 0;

    private Context app = null;
    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter adapter = null;

    private BluetoothLeAdvertiser advertiser = null;
    private AdvertiseCallback advertiseCallback = null;
    private AdvertiseSettings advertiseSettings = null;
    private AdvertiseData advertiseData = null;

    private MutableLiveData<String> _message;
    private LiveData<String> message = _message;

    private MutableLiveData<BluetoothDevice> _connectionRequest;
    private LiveData<BluetoothDevice> connectionRequest = _connectionRequest;

    private MutableLiveData<Boolean> _requestEnableBluetooth;
    private LiveData<Boolean> requestEnableBluetooth = _requestEnableBluetooth;

    private BluetoothGattServer gattServer;
    private BluetoothGattServerCallback gattServerCallback = null;

    private BluetoothGatt gattClient;
    private BluetoothGattCallback gattClientCallback = null;

    private BluetoothDevice currentDevice;
    //private MutableLiveData<DeviceConnectionState> _deviceConnection;
    //private LiveData<DeviceConnectionState> deviceConnection = _deviceConnection;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic messageCharacteristic;

    @RequiresApi(api = Build.VERSION_CODES.M)
    void startServer(Context context, Activity activity) {
        Log.d(TAG, "Server Start");
        app = context;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        /*
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED) {
           grantedRequest |= REQUEST_BLUETOOTH_ADVERTISE;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                grantedRequest |= REQUEST_BLUETOOTH_CONNECT;
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED) {
                    grantedRequest |= REQUEST_BLUETOOTH_SCAN;
                    */
                    if (bluetoothManager == null) {
                        bluetoothManager
                                = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
                        adapter = bluetoothManager.getAdapter();
                    }
                    /*
                    if (_requestEnableBluetooth == null) {
                        _requestEnableBluetooth = new MutableLiveData<Boolean>();
                    }
                    if (!adapter.isEnabled()) {
                        _requestEnableBluetooth.setValue(true);
                    } else {
                        _requestEnableBluetooth.setValue(false);
                     */
                        Log.d(TAG, "--------");
                        if (adapter.isMultipleAdvertisementSupported()) { // サーバーになれるかどうかのチェック
                            Log.d(TAG, "Server supported");
                            setupGattServer(app);
                            startAdvertisement();
                        }

                        /*
                    }
                } else {
                    activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN},
                            REQUEST_BLUETOOTH_SCAN);
                }
            } else {
                activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT);
            }
        } else {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                    REQUEST_BLUETOOTH_ADVERTISE);
        }
        */
    }

    void stopServer() {
        stopAdevertisement();
    }

    String getyourDeviceAddress() {
        return bluetoothManager.getAdapter().getAddress();
    }

    void setCurrentConnection(BluetoothDevice device) {
        currentDevice = device;
        /*
        if (_deviceConnection == null) {
            _deviceConnection = new MutableLiveData<DeviceConnectionState>();
        }*/
        connectToDevice(device);
    }

    private void connectToDevice(BluetoothDevice device) {
        gattClientCallback = new GattClientCallback();
        gattClient = device.connectGatt(app,false, gattClientCallback);
    }

    Boolean sendMessage(String message) {
        Log.d(TAG, "Send a message");
        messageCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        messageCharacteristic.setValue(messageBytes);
        if ( gatt != null ) {
            boolean success = gatt.writeCharacteristic(messageCharacteristic);
            if (success) {
                Log.d(TAG, "onServiceDiscovered: message send: true");
                _message.setValue(message);
            } else {
                Log.d(TAG, "onServiceDiscovered: message send: false");
            }
        }
        else {
            Log.d(TAG, "sendMessage: no gatt connection to send a message with");
        }
        return false;
    }

    private void setupGattServer(Context app) {
        Log.d(TAG, "Set up GATT server");
        gattServerCallback = new GattServerCallback();

        gattServer = bluetoothManager.openGattServer(app, gattServerCallback);
        gattServer.addService(setupGattService());
    }

    private BluetoothGattService setupGattService() {
        Log.d(TAG, "Set up GATT service");
        BluetoothGattService service
                = new BluetoothGattService(SERVICE_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic messageCharacteristic = new BluetoothGattCharacteristic(
                MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(messageCharacteristic);
        BluetoothGattCharacteristic confirmCharacteristic = new BluetoothGattCharacteristic(
                CONFIRM_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(confirmCharacteristic);

        return service;
    }

    private void startAdvertisement() {

        advertiser = adapter.getBluetoothLeAdvertiser();
        advertiseSettings = buildAdevertiseSettings();
        advertiseData = buildAdvertiseData();
        if (advertiser != null) {
            Log.d(TAG, String.format("startAdvertisement: with advertiser %s", advertiser.toString()));
            if (advertiseCallback == null) {
                advertiseCallback = new DeviceAdvertiseCallback();

                advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            }
        }
        else {
            Log.d(TAG, "can't get advertiser");
        }
    }

    private void stopAdevertisement() {
        Log.d(TAG, "stopAdvertisement:");
        if ( advertiser != null ) {
            Log.d(TAG, String.format("stopAdvertisement: with advertiser %s", advertiser.toString()));
            advertiser.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
        }
    }

    AdvertiseData buildAdvertiseData() {
        ParcelUuid parcelUuid = new ParcelUuid((SERVICE_UUID));
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    AdvertiseSettings buildAdevertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTimeout(0)
                .build();
    }

    private class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            boolean isSuccess = (status == BluetoothGatt.GATT_SUCCESS);
            boolean isConnected = (newState == BluetoothProfile.STATE_CONNECTED);
            Log.d(TAG,
                    String.format("onConnectionStateChange: Server %s %s"
                            + "succress: %s connected: %s",
                            device.toString(), device.getName(),
                            isSuccess?"true":"false", isConnected?"true":"false")
            );
            if (isSuccess && isConnected) {
                _connectionRequest.postValue(device);
            }
            else {
                //_deviceConnection.postValue(DeviceConnectionState.Disconnected);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic,
                    preparedWrite, responseNeeded, offset, value);
            if (characteristic.getUuid() == MESSAGE_UUID) {
                if (gattServer != null) {
                    gattServer.sendResponse(device, requestId,
                            BluetoothGatt.GATT_SUCCESS, 0, null);
                    if (value != null) {
                        String message = new String(value, StandardCharsets.UTF_8);
                        Log.d(TAG, String.format("onCharacteristicWriteRequest: Have message: %s",
                               message));
                        if (message != null) {
                            //_message.postValue(RemoteMessage(message));
                        }
                    }
                }
            }
        }
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isSuccess = (status == BluetoothGatt.GATT_SUCCESS);
            boolean isConnected = (newState == BluetoothProfile.STATE_CONNECTED);
            Log.d(TAG, String.format("onConnectionStateChange: Client %s success: %s connected: %s",
                    gatt.toString(), isSuccess?"true":"false", isConnected?"true":"false"));
            if (isSuccess && isConnected) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt discoveredGatt, int status) {
            super.onServicesDiscovered(discoveredGatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("onServiceDiscovered: Have gatt %s",
                        discoveredGatt.toString()));
                gatt = discoveredGatt;
                BluetoothGattService service = discoveredGatt.getService(SERVICE_UUID);
                messageCharacteristic = service.getCharacteristic(MESSAGE_UUID);
            }
        }
    }

    private class DeviceAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, String.format("Advertise failed with error: %d", errorCode));
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertiseing successfully started");
        }
    }
}
