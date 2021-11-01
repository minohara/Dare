package org.minohara.dare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class SensorData implements SensorEventListener {
    float lux;
    private SensorManager sensorManager;
    private Sensor mLight;

    public SensorData(Context app) {
        sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) { }

    @SuppressLint("DefaultLocale")
    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = event.values[0];
    }

    void startSensor() {
        sensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    void stopSensor() {
        sensorManager.unregisterListener(this);
    }

}
