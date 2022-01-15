package org.minohara.dare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class SensorData implements SensorEventListener {
    float lux;
    private SensorManager sensorManager;
    private Sensor mLight;
    String str;

    public SensorData(Context app){
        sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public  final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy){

    }
    @SuppressLint("DefaultLocale")
    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = event.values[0];
    }
    void startSensor(){
        sensorManager.registerListener(this, mLight, sensorManager.SENSOR_DELAY_NORMAL);
    }
    void stopSensor(){
        sensorManager.unregisterListener(this);
    }
public String lux(){
        str = "照度："+ lux;
        return str;
    }
}
