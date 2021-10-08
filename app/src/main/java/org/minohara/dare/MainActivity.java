package org.minohara.dare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor mLight;
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1000;
    private final static int REQUEST_PERMIT_SCAN = 1010;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Bluetoothデバiスの確認
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            writeMsg("Device doesn't support Bluetooth");
        }
        // BluetoothがONになっていなければONにする
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // パーミッションを要求する
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.BLUETOOTH_SCAN}, REQUEST_PERMIT_SCAN);
        // 他のデバiスからの検出を許可する
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        Intent intent = discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        // Bluetooth関連の処理を非同期に行うための設定
        IntentFilter found = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter discoveryStarted = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        IntentFilter discoveryFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, found);
        registerReceiver(receiver, discoveryStarted);
        registerReceiver(receiver, discoveryFinished);

        // 検索中かどうかを確認して、検索中なら一度キャンセルする
        if (bluetoothAdapter.isDiscovering()) {
            writeMsg("Already in discovering");
            System.out.println("Already in discovering");
            bluetoothAdapter.cancelDiscovery();
        }
        // 検索を開始する
        if (!bluetoothAdapter.startDiscovery()) {
            writeMsg("Can't start discovery");
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @SuppressLint("DefaultLocale")
    @Override
    public final void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        ((TextView)findViewById(R.id.light)).setText(String.format("%8.2f",lux));
    }

    // 画面にメッセ-ジを出すための処理
    public void writeMsg(String text) {
        ((TextView)findViewById(R.id.message)).setText(text);
    }

    // Searchボタンが押されたときの処理
    public void searchBtn(View view) {
        // 検索を開始する
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        Intent intent = discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (!bluetoothAdapter.startDiscovery()) {
            writeMsg("Can't start discovery");
        }
        else {
            writeMsg("Searching...");
        }
    }

    // 非同期の処理内容
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) { // 端末を見付けたら名前とMACを表示する
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                System.out.println(String.format("%s(%s)", deviceName, deviceHardwareAddress));
                writeMsg(String.format("%s(%s)", deviceName, deviceHardwareAddress));
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) { // 検索の開始を表示
                writeMsg("Discovery Started");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { // 検索の終了を表示
                writeMsg("Discovery Finished");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Bluetooth を ONにしたときの処理
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        writeMsg(requestCode+","+resultCode);
    }

}