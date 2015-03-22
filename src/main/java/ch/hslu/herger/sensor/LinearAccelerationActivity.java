package ch.hslu.herger.sensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.estimote.examples.demos.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import ch.hslu.herger.data.DataHandler;

public class LinearAccelerationActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private DataHandler dataHandler;

    private long lastUpdate;
    private long actualTime;

    private Float[] xDirection;
    private Float[] yDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_linear_acceleration);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        dataHandler = DataHandler.getInstance();

        lastUpdate = System.currentTimeMillis();

        xDirection = new Float[] {(float)0,(float)0};
        yDirection = new Float[] {(float)0,(float)0};
    }

    protected void onResume() {
        super.onResume();
        lastUpdate = 0;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor changedSensor = event.sensor;

        if(lastUpdate != 0.0) {
            lastUpdate = actualTime;
        }
        actualTime = event.timestamp;

        if(changedSensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            // if total time is smaller than 10sec keep measuring

                float x = event.values[0];
                float y = event.values[1];
                //float z = event.values[2];

                xDirection[0] = xDirection[1];
                xDirection[1] = event.values[0];

                yDirection[0] = yDirection[1];
                yDirection[1] = event.values[1];

                dataHandler.calcPos(xDirection, yDirection, lastUpdate, actualTime);
        }
        if(changedSensor.getType() == Sensor.TYPE_ORIENTATION){
            // angle between the magnetic north direction
            // 0=North, 90=East, 180=South, 270=West
            float azimuth = event.values[0];

            dataHandler.setCompass(azimuth);
        }
    }

}

