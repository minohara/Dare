package org.minohara.dare;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Dare";
    private DataServer dataServer;
    private SensorData sensorData;
    private TextView textView;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner scanner = null;
    private List<ScanFilter> scanFilters;
    private static final int REQUEST_ENABLE_BT = 1000;
    private static final long SCAN_PERIOD = 10000;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    ScanSettings scanSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.message);
        dataServer = new DataServer();
        sensorData = new SensorData(getApplicationContext());
        scanFilters = buildScanFilters();
        scanSettings = buildScanSettings();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN}, 1001);
        } else {
            dataServer.startServer(getApplicationContext(), this);
        }
    }
//----------------------------------------------------------Scan----------------------------------------------------------
    private List<ScanFilter> buildScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(new ParcelUuid(DataServer.SERVICE_UUID));
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(builder.build());
        return filters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    private void startScan() {
        if (scanner == null) {
            scanner = adapter.getBluetoothLeScanner();
        }
        textView.setText("");
        scanner.startScan(scanFilters, scanSettings, scanCallback);
        Log.d(TAG, "Scan started");
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int type, ScanResult result) {
            super.onScanResult(type, result);
            saveDevice(result.getDevice());
            textView.append(result.toString());
            Log.d(TAG, String.format("onScanResult: %s", result.toString()));
            if (deviceList != null) {
                Log.d(TAG, String.format("complete save deviceList"));
            }
        }
    };

    public void saveDevice(BluetoothDevice device) {
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        deviceList.add(device);
    }
//----------------------------------------------------------Scan----------------------------------------------------------
    public void writeMsg(String text) {
        ((TextView)findViewById(R.id.message)).setText(text);
    }

    public void search(View view) {
        startScan();
    }

    public void Connect(View vie) {
        dataServer.setCurrentConnection(deviceList.get(0));
    }

    public void Close(View vi) { dataServer.stopServer(); }

    public void Send(View v){
        String aaa = "1234";
        dataServer.sendMessage(aaa);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorData.startSensor();

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorData.stopSensor();
        //dataServer.stopServer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        if (requestCode == 1001 ) {
            for (int i = 0; i < grantResult.length; i++) {
                if (grantResult[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.format("%s is not permitted", permissions[i].toString()));
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dataServer.startServer(getApplicationContext(), this);
            }
        }
    }
}
