package org.minohara.dare;

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
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    static final UUID KEY_UUID = UUID.fromString("c2123ac0-3c00-4179-a1af-76f5647c15dc");
    static int grantedRequest = 0;
    private Context app = null;
    private Activity activity = null;
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
    //private LiveData<DeviceConnectionState> deviceConnection = _deviceConnection
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic messageCharacteristic;
    private BluetoothGattCharacteristic keyCharacteristic;
    private int server_key;
    private int client_key;
    private int key;
    private int key_stopper = 1;
    private boolean f;
    private String s;
    BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 30, 0.01);

    @RequiresApi(api = Build.VERSION_CODES.M)
    void startServer(Context context, Activity activity) {
        Log.d(TAG, "Server Start");
        app = context;
        this.activity = activity;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = bluetoothManager.getAdapter();
        }
        if (adapter.isMultipleAdvertisementSupported()) {
            Log.d(TAG, "Server supported");
            setupGattServer(app);
            startAdvertisement();
        }
    }

    void stopServer() {
        stopAdevertisement();
    }

    Boolean sendMessage(String x) {
        Log.d(TAG, "Send a message");
        messageCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        byte[] messageBytes = x.getBytes(StandardCharsets.UTF_8);
        messageCharacteristic.setValue(messageBytes);
        if (gatt != null) {
            boolean success = gatt.writeCharacteristic(messageCharacteristic);
            if (success) {
                Log.d(TAG, "onServiceDiscovered: message send: true");
            } else {
                Log.d(TAG, "onServiceDiscovered: message send: false");
            }
        } else {
            Log.d(TAG, "sendMessage: no gatt connection to send a message with");
        }
        return false;
    }

    private void makeSkey() {
        server_key = 120;
        Log.d(TAG, "Make server key");
    }

    public void makeCkey() {
        client_key = 3;
        Log.d(TAG, "Make client key");
    }

    public void exchangeKey() {
        Log.d(TAG, "Key exchange");
        keyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        byte[] keyBytes = Integer.toString(client_key).getBytes(StandardCharsets.UTF_8);
        keyCharacteristic.setValue(keyBytes);
        if (gatt != null) {
            boolean success = gatt.writeCharacteristic(keyCharacteristic);
            if (success) {
                Log.d(TAG, "onServiceDiscovered: key send: true");
            } else {
                Log.d(TAG, "onServiceDiscovered: key send: false");
            }
        } else {
            Log.d(TAG, "sendMessage: no gatt connection to send a key with");
        }
        return;
    }

    public void Makekeystop() {
        Log.d(TAG, "Make key stop");
        key_stopper = 0;
    }

    //----------------------------------------------------------↓server↓----------------------------------------------------------
    private void setupGattServer(Context app) {
        Log.d(TAG, "Set up GATT server");
        gattServerCallback = new GattServerCallback();
        gattServer = bluetoothManager.openGattServer(app, gattServerCallback);
        gattServer.addService(setupGattService());
    }

    private BluetoothGattService setupGattService() {
        Log.d(TAG, "Set up GATT service");
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic Characteristic = new BluetoothGattCharacteristic(MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(Characteristic);
        BluetoothGattCharacteristic confirmCharacteristic = new BluetoothGattCharacteristic(CONFIRM_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(confirmCharacteristic);
        BluetoothGattCharacteristic keyCharacteristic = new BluetoothGattCharacteristic(KEY_UUID,
                (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE),
                (BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE));
        makeSkey();
        //keyCharacteristic.setValue(server_key, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        service.addCharacteristic(keyCharacteristic);
        return service;
    }

    private class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            boolean isSuccess = (status == BluetoothGatt.GATT_SUCCESS);
            boolean isConnected = (newState == BluetoothProfile.STATE_CONNECTED);
            Log.d(TAG, String.format("onConnectionStateChange: Server %s %s " + "succress: %s connected: %s",
                    device.toString(), device.getName(), isSuccess ? "true" : "false", isConnected ? "true" : "false"));
            /*
            if (isSuccess && isConnected) {
                _connectionRequest.postValue(device);
            } else {
                //_deviceConnection.postValue(deviceConnectionState.Disconnected);
            }
            */
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            if (characteristic.getUuid().equals(MESSAGE_UUID)) {
                if (gattServer != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                    if (value != null) {
                        String message = new String(value, StandardCharsets.UTF_8);
                        s = message.substring(0,3);
                        f = filter.mightContain(Integer.parseInt(s));
                        if (f = true) {
                            //key = (Integer.parseInt(message.substring(0)) + server_key);
                            //filter.put(key);
                            //Log.d(TAG, String.format("make key:" + key));
                       //} else {
                            Log.d(TAG, String.format("onCharacteristicWriteRequest: Have message: %s", message));
                            ((TextView) activity.findViewById(R.id.message)).setText(message);
                        }
                        message = "received:" + message;
                        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
                    }
                }
            } else if (characteristic.getUuid().equals(KEY_UUID)) {//
                if (gattServer != null) {
                    if (value != null) {
                        String message = new String(value, StandardCharsets.UTF_8);
                        if (key_stopper == 1) {
                            key = (Integer.parseInt(message) + server_key);
                            filter.put(key);
                            Log.d(TAG, String.format("Key (%d) is registered for (%s)", key, device.toString()));
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                        }
                    }
                }
            }
        }
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, String.format("onCharacteristicReadRequest ", characteristic.getUuid()));
            if (characteristic.getUuid().equals(KEY_UUID)) {
                byte[] messageBytes = String.valueOf(server_key).getBytes(StandardCharsets.UTF_8);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, messageBytes);
                Log.d(TAG, String.format("Key (%d) is sent to (%s)", server_key, device.toString()));
            }
        }
    }

    AdvertiseData buildAdvertiseData() {
        ParcelUuid parcelUuid = new ParcelUuid((SERVICE_UUID));
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder().addServiceUuid(parcelUuid).setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    AdvertiseSettings buildAdevertiseSettings() {
        return new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER).setTimeout(0).build();
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
        } else {
            Log.d(TAG, "can't get advertiser");
        }
    }

    private void stopAdevertisement() {
        Log.d(TAG, "stopAdvertisement:");
        if (advertiser != null) {
            Log.d(TAG, String.format("stopAdvertisement: with advertiser %s", advertiser.toString()));
            advertiser.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
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

    //----------------------------------------------------------↑server↑----------------------------------------------------------
    void setCurrentConnection(BluetoothDevice device) {
        makeCkey();
        currentDevice = device;
        connectToDevice(device);
    }

    private void connectToDevice(BluetoothDevice device) {
        gattClientCallback = new GattClientCallback();
        gattClient = device.connectGatt(app, false, gattClientCallback);
    }

    //----------------------------------------------------------↓client↓----------------------------------------------------------
    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isSuccess = (status == BluetoothGatt.GATT_SUCCESS);
            boolean isConnected = (newState == BluetoothProfile.STATE_CONNECTED);
            Log.d(TAG, String.format("onConnectionStateChange: Client %s success: %s connected: %s", gatt.toString(), isSuccess ? "true" : "false", isConnected ? "true" : "false"));
            if (isSuccess && isConnected) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt discoveredGatt, int status) {
            super.onServicesDiscovered(discoveredGatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("onServiceDiscovered: Have gatt %s", discoveredGatt.toString()));
                gatt = discoveredGatt;
                BluetoothGattService service = discoveredGatt.getService(SERVICE_UUID);
                messageCharacteristic = service.getCharacteristic(MESSAGE_UUID);
                keyCharacteristic = service.getCharacteristic(KEY_UUID);
                exchangeKey();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("onCharacteristicWrite: Have gatt %s", characteristic.getUuid().toString()));
                if (characteristic.getUuid().equals(KEY_UUID)) {
                    boolean success = gatt.readCharacteristic(keyCharacteristic);
                }
            }

        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("onCharacteristicRead: Have gatt %s", characteristic.getUuid().toString()));
                if (characteristic.getUuid().equals(KEY_UUID)) {
                    int key = Integer.parseInt( new String(characteristic.getValue()));
                    Log.d(TAG, String.format("received key (%d)", key));
                    key += client_key;
                    Log.d(TAG, String.format("exchanged key (%d)", key));
                    filter.put(key);
                }
            }

        }
    }
}
//----------------------------------------------------------↑client↑----------------------------------------------------------