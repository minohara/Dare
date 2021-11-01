package org.minohara.dare;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
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
    private BluetoothLeScanner scanner = null;
    private List<ScanFilter> scanFilters;
    ScanSettings scanSettings;

    private void startScan() {
        if (scanner == null) {
            scanner = adapter.getBluetoothLeScanner();
        }
        textView.setText("");
        scanner.startScan(scanFilters, scanSettings, scanCallback);
        //scanner.startScan(scanCallback);
        Log.d(TAG, "Scan started");
    }

    private ScanCallback scanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int type, ScanResult result) {
                    super.onScanResult(type, result);
                    textView.append(result.toString());
                    Log.d(TAG, String.format("onScanResult: %s", result.toString()));
                }
            };

    private List<ScanFilter> buildScanFiters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(new ParcelUuid(DataServer.SERVICE_UUID));
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(builder.build());
        //Log.d(TAG, filters.toString());
        return filters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView)findViewById(R.id.message);
        dataServer = new DataServer();
        sensorData = new SensorData(getApplicationContext());

        scanFilters = buildScanFiters();
        scanSettings = buildScanSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN},
                    1001);
        } else {
            dataServer.startServer(getApplicationContext(), this);
        }
    }

    // 画面にメッセ-ジを出すための処理
    public void writeMsg(String text) {
        ((TextView)findViewById(R.id.message)).setText(text);
    }

    // Searchボタンが押されたときの処理
    public void searchBtn(View view) {
        startScan();
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