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
import android.widget.TextView;
import android.widget.Toast;

import ch.hslu.herger.main.R;

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

    private TextView tvDistX;
    private TextView tvDistY;
    private TextView tvCompass;
    private TextView tvSpeedX;
    private TextView tvSpeedY;
    private TextView tvXAcc;
    private TextView tvYAcc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_acceleration);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        dataHandler = DataHandler.getInstance();

        lastUpdate = (long) 0;

        xDirection = new Float[] {(float)0,(float)0};
        yDirection = new Float[] {(float)0,(float)0};

        tvDistX = (TextView) findViewById(R.id.tvDistX);
        tvDistY = (TextView) findViewById(R.id.tvDistY);
        tvCompass = (TextView) findViewById(R.id.tvCompass);
        tvSpeedX = (TextView) findViewById(R.id.tvSpeedX);
        tvSpeedY = (TextView) findViewById(R.id.tvSpeedY);
        tvXAcc = (TextView) findViewById(R.id.tvXAcc);
        tvYAcc = (TextView) findViewById(R.id.tvYAcc);
    }

    protected void onResume() {
        super.onResume();
        lastUpdate = 0;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor changedSensor = event.sensor;

        if(changedSensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){

            lastUpdate = actualTime;
            actualTime = event.timestamp;
            //System.out.println("actualTime = "+actualTime);
            //System.out.println("lastUpdate = "+lastUpdate);
            //System.out.println("dt = "+((actualTime-lastUpdate)/10000000000.0));

                float x = event.values[0];
                float y = event.values[1];
                //float z = event.values[2];

                xDirection[0] = xDirection[1];
                xDirection[1] = x;

                yDirection[0] = yDirection[1];
                yDirection[1] = y;

                dataHandler.calcPos(xDirection, yDirection, lastUpdate, actualTime);
                displayData();
        }
        if(changedSensor.getType() == Sensor.TYPE_ORIENTATION){
            // angle between the magnetic north direction
            // 0=North, 90=East, 180=South, 270=West
            float azimuth = event.values[0];

            dataHandler.setCompass(azimuth);
        }
    }

    public void displayData(){
        tvCompass.setText("Compass :"+dataHandler.getCompass());
        tvSpeedX.setText("Speed X: "+Float.toString(dataHandler.getXSpeed()));
        tvSpeedY.setText("Speed Y:"+Float.toString(dataHandler.getYSpeed()));
        tvDistX.setText("Distance X:"+Float.toString(dataHandler.getXDist()));
        tvDistY.setText("Distance Y:"+Float.toString(dataHandler.getYDist()));
        tvXAcc.setText("Acceleration X: "+Float.toString(dataHandler.getxAcc()));
        tvYAcc.setText("Acceleration Y: "+Float.toString(dataHandler.getyAcc()));
    }

}

