/************************************************************************************
 * Copyright (c) 2012 Paul Lawitzki
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 ************************************************************************************/

package ch.hslu.herger.sensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.utils.L;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.hslu.herger.beacon.BeaconComparator;
import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.XMLBeacon;
import ch.hslu.herger.config.XMLLocation;
import ch.hslu.herger.data.DataHandler;
import ch.hslu.herger.main.R;

public class SensorFusionActivity extends Activity
implements SensorEventListener {

	private SensorManager mSensorManager = null;
	
    // angular speeds from gyro
    private float[] gyro = new float[3];
 
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
 
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
 
    // magnetic field vector
    private float[] magnet = new float[3];
 
    // accelerometer vector
    private float[] accel = new float[3];

    // linear accelertometer vector;
    private float[] linearAccel = new float[3];

    // world linear acceleration / velocity / position / vector
    private float[] worldLinearAccel = new float[3];
    private float[] tempSpeed = new float[2];
    private float[] speed = new float[2];
    private float[] tempDistance = new float[2];
    private float[] distance = new float[2];
    private int noMovementXCount = 0;
    private int noMovementYCount = 0;

    private double mapX;
    private double mapY;
    private double positionX;
    private double positionY;


 
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
 
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float MS2S = 1.0f / 1000000.0f;
    private static final double D2RAD = (2*Math.PI/360);
	private float timestamp;
    private float timestampAccel;

    // Constants for the low-pass accelerometer filter
    private static final float ALPHA = 0.15f;

	private boolean initState = true;
    
	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private Timer fuseTimer = new Timer();
	
	// The following members are only for displaying the sensor output.
	public Handler mHandler;
    private TableLayout tableDebug;
	private TextView mAzimuthView;
	private TextView mPitchView;
	private TextView mRollView;
    private TextView mLinAccX;
    private TextView mLinAccY;
    private TextView mLinAccZ;
    private TextView mSpeedX;
    private TextView mSpeedY;
    private TextView mDistX;
    private TextView mDistY;
    private TextView mCurrentBeacon;

    private AbsoluteLayout positionMap;
    private ImageView position;
    private ImageView beaconPosition;

    private RelativeLayout locationWarning;

    private boolean running = false;
    private boolean inBeaconRange = false;


	DecimalFormat d = new DecimalFormat("#.##");

    // Acceleration Strings for Logging
    private static StringBuilder sbX = new StringBuilder(10000000);
    private static StringBuilder sbY = new StringBuilder(10000000);

    // dataHandler
    private DataHandler dataHandler;

    private static final String TAG = SensorFusionActivity.class.getSimpleName();

    public static final String EXTRAS_TARGET_ACTIVITY = "extrasTargetActivity";
    public static final String EXTRAS_BEACON = "extrasBeacon";

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    // Constant for converting Px to Dp
    private DisplayMetrics metrics;
    private float pxTodp;

    private BeaconManager beaconManager;
    private List<Beacon> beaconList;
    private Beacon currentBeacon;
    private XMLBeacon currentXMLBeacon;

    private static XMLLocation currentLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_fusion);
        // Keep Screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;
 
        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
 
        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();
        
        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                                      1000, TIME_CONSTANT);
        
        // GUI stuff
        mHandler = new Handler();
        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);
        tableDebug = (TableLayout)findViewById(R.id.tableDebug);
        mAzimuthView = (TextView)findViewById(R.id.textView4);
        mPitchView = (TextView)findViewById(R.id.textView5);
        mRollView = (TextView)findViewById(R.id.textView6);
        mLinAccX = (TextView)findViewById(R.id.textViewLAXValue);
        mLinAccY = (TextView)findViewById(R.id.textViewLAYValue);
        mLinAccZ = (TextView)findViewById((R.id.textViewLAZValue));
        mSpeedX = (TextView)findViewById(R.id.textViewSpeedXValue);
        mSpeedY = (TextView)findViewById(R.id.textViewSpeedYValue);
        mDistX = (TextView)findViewById(R.id.textViewDistXValue);
        mDistY = (TextView)findViewById(R.id.textViewDistYValue);
        mCurrentBeacon = (TextView)findViewById(R.id.textViewCurrentBeaconValue);

        positionMap = (AbsoluteLayout) findViewById(R.id.positionMap);
        positionMap.setVisibility(View.INVISIBLE);
        position = (ImageView) findViewById(R.id.position);
        beaconPosition = (ImageView) findViewById(R.id.beaconPosition);

        locationWarning = (RelativeLayout) findViewById(R.id.locationWarning);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set Display Metrics
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        pxTodp = metrics.density;

        // Initialize DataHandler
        dataHandler = DataHandler.getInstance();

        // Configure verbose debug logging.
        L.enableDebugLogging(true);

        // Read Configuration
        final List<XMLLocation> locationList = ((Configuration) this.getApplication()).getLocationList();

        // Configure BeaconManager.
        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(final Region region, final List<Beacon> beacons) {

                beaconList = beacons;

                if(beaconList.size()>0) {
                    currentBeacon = beaconList.get(0);
                    currentXMLBeacon = isBeaconKnown(currentBeacon, locationList);
                    if (currentXMLBeacon != null) {
                        if(isBeaconInRange(currentBeacon)){
                            running = true;
                            inBeaconRange = true;
                        }else{
                            inBeaconRange = false;
                        }
                        System.out.println("BEACON = " + currentXMLBeacon.getMajor());

                    }else
                        running = false;
                }else{
                    currentBeacon = null;
                    currentXMLBeacon = null;
                    running = false;
                    inBeaconRange = false;
                }

            }
        });
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	// unregister sensor listeners to prevent the activity from draining the device's battery.
    	mSensorManager.unregisterListener(this);

        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging", e);
        }
    }
	
    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	// restore the sensor listeners when user resumes the application.
    	// TODO init listeners after Beacon detected
        initListeners();
    }
    
    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    public void initListeners(){
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST);
     
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST);
     
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_FASTEST);

    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array and calculate orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, linearAccel, 0, 3);
                calcAccelInWorldCoordinates(event.timestamp);
                break;
        }
	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateAccMagOrientation() {
	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
	    }
	}
	
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector,
            float timeFactor)
	{
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude =
		(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
		gyroValues[1] * gyroValues[1] +
		gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
		normValues[0] = gyroValues[0] / omegaMagnitude;
		normValues[1] = gyroValues[1] / omegaMagnitude;
		normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
	
    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;
     
        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }
     
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
        System.arraycopy(event.values, 0, gyro, 0, 3);
        getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }
     
        // measurement done, save current time for next interval
        timestamp = event.timestamp;
     
        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
     
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
     
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }
    
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
     
        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);
     
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
     
        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
     
        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
     
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
    
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            
            /*
             * Fix for 179� <--> -179� transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
             * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
             */
            
            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }
            
            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }
            
            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }
     
            // overwrite gyro matrix and orientation with fused orientation
            // to compensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

            // update sensor output in GUI
            // mHandler.post(updateOreintationDisplayTask);
        }
    }

    public void accelerometerLowPass(SensorEvent event)
    {
        for(int i=0; i<event.values.length; i++){
            linearAccel[i] = linearAccel[i] + ALPHA * (event.values[i] - linearAccel[i]);
        }
    }

    public void calcAccelInWorldCoordinates(long time){

        worldLinearAccel[0] =(float) (linearAccel[0]*(Math.cos(fusedOrientation[2])*Math.cos(fusedOrientation[0])
                +Math.sin(fusedOrientation[2])*Math.sin(fusedOrientation[1])*Math.sin(fusedOrientation[0]))
                + linearAccel[1]*(Math.cos(fusedOrientation[1])*Math.sin(fusedOrientation[0]))
                + linearAccel[2]*(-Math.sin(fusedOrientation[2])*Math.cos(fusedOrientation[0])
                +Math.cos(fusedOrientation[2])*Math.sin(fusedOrientation[1])*Math.sin(fusedOrientation[0])));
        worldLinearAccel[1] = (float) (linearAccel[0]*(-Math.cos(fusedOrientation[2])*Math.sin(fusedOrientation[0])
                +Math.sin(fusedOrientation[2])*Math.sin(fusedOrientation[1])*Math.cos(fusedOrientation[0]))
                + linearAccel[1]*(Math.cos(fusedOrientation[1])*Math.cos(fusedOrientation[0]))
                + linearAccel[2]*(Math.sin(fusedOrientation[2])*Math.sin(fusedOrientation[0])
                + Math.cos(fusedOrientation[2])*Math.sin(fusedOrientation[1])*Math.cos(fusedOrientation[0])));

        //sbX.append(worldLinearAccel[0]+";");
        //sbY.append(worldLinearAccel[1]+";");

        if(timestampAccel != 0) {
            final float dTAccel = (time - timestampAccel) * NS2S;

            tempSpeed[0] = worldLinearAccel[0]*dTAccel;
            tempSpeed[1] = worldLinearAccel[1]*dTAccel;

            tempDistance[0] = speed[0]*dTAccel;
            tempDistance[1] = speed[1]*dTAccel;

            // ignore small acceleration
            if(worldLinearAccel[0]>0.15){
                speed[0] += tempSpeed[0];
                distance[0] = tempDistance[0];
                noMovementXCount = 0;
            }else {
                noMovementXCount++;
                // too long no movement -> reset speed
                if(noMovementXCount > 40) {
                    speed[0] = 0;
                    noMovementXCount = 0;
                }
            }
            if(worldLinearAccel[1]>0.15){
                speed[1] += tempSpeed[1];
                distance[1] = tempDistance[1];
                noMovementYCount = 0;
            }else {
                noMovementYCount++;
                // too long no movement -> reset speed
                if(noMovementYCount > 40){
                    speed[1] = 0;
                    noMovementYCount = 0;
                }
            }
            // set current timestamp to old timestamp
            timestampAccel = time;
        }else{
            // ignore first event
            timestampAccel = time;
        }

        // TODO calculate XY DIRECTION ON MAP
        if(currentLocation != null) {
            float angleToNorth = Float.valueOf(currentLocation.getAngleToNorth());
            double deviceDirection = fusedOrientation[0]* 180/Math.PI;
            float distanceX = distance[0];
            float distanceY = distance[1];

            double diff = 0;

            double distYmapX = 0;
            double distYmapY = 0;
            double distXmapX = 0;
            double distXmapY = 0;

            if(angleToNorth >= 0f){
                if(deviceDirection >= 0f){
                    diff = deviceDirection - angleToNorth;
                    //System.out.println("FALL 1");
                }else{
                    double tempDevice = deviceDirection + 360;
                    diff = tempDevice - angleToNorth;
                    //System.out.println("FALL 2");
                }
            }else{
                if(deviceDirection >= 0f){
                    double tempNorth = angleToNorth + 360;
                    diff = deviceDirection - tempNorth;
                    //System.out.println("FALL 3");
                }else{
                    double tempNorth = angleToNorth + 360;
                    double tempDevice = deviceDirection + 360;
                    diff = tempDevice - tempNorth;
                    //System.out.println("FALL 4");

                }
            }
            //System.out.println("DIFF = "+diff);

            distYmapY = Math.cos(diff*D2RAD)*distanceY;
            distYmapX = Math.sin(diff*D2RAD)*distanceY;
            distXmapY = Math.cos(diff*D2RAD+90f*D2RAD)*distanceX;
            distXmapX = Math.sin(diff*D2RAD+90f*D2RAD)*distanceX;

            //System.out.println("distYmapY = "+distYmapY);
            //System.out.println("distYmapX = "+distYmapX);
            //System.out.println("distXmapY = "+distXmapY);
            //System.out.println("distXmapX = "+distXmapX);

            mapY = distYmapY + distXmapY;
            mapX = distYmapX + distXmapX;

            //System.out.println("mapY = "+mapY);
            //System.out.println("mapX = "+mapX);


        }

        mHandler.post(updateUITask);
    }

    // **************************** GUI FUNCTIONS *********************************

    public void updateOreintationDisplay() {
    	// case 0 was accMagOrientation
        // case 1 was gyroOrientation
        if(running){
            tableDebug.setVisibility(View.INVISIBLE);
            mAzimuthView.setText(d.format(fusedOrientation[0] * 180/Math.PI) + '°');
            mPitchView.setText(d.format(fusedOrientation[1] * 180/Math.PI) + '°');
            mRollView.setText(d.format(fusedOrientation[2] * 180/Math.PI) + '°');
            mLinAccX.setText(d.format(worldLinearAccel[0]) + 'm'+'/'+'s'+'^'+'2');
            mLinAccY.setText(d.format(worldLinearAccel[1]) + 'm'+'/'+'s'+'^'+'2');
            mLinAccZ.setText(d.format(linearAccel[2]) + 'm'+'/'+'s'+'^'+'2');
            mSpeedX.setText(d.format(speed[0]) + 'm'+'/'+'s');
            mSpeedY.setText(d.format(speed[1]) + 'm'+'/'+'s');
            mDistX.setText(d.format(distance[0]) + 'm');
            mDistY.setText(d.format(distance[1]) + 'm');
            if(inBeaconRange){
                locationWarning.setVisibility(View.INVISIBLE);
                mCurrentBeacon.setText(currentXMLBeacon.getMajor());
                positionMap.setVisibility(View.VISIBLE);
                mCurrentBeacon.setVisibility(View.VISIBLE);
                positionX = Float.parseFloat(currentXMLBeacon.getxPos());
                positionY = Float.parseFloat(currentXMLBeacon.getyPos());
                position.setX((float)positionX*pxTodp);
                position.setY((float) positionY * pxTodp);
                beaconPosition.setVisibility(View.VISIBLE);
                beaconPosition.setX((float) positionX * pxTodp);
                beaconPosition.setY((float) positionY * pxTodp);
                System.out.println("Position X = "+positionX);
            }else{
                locationWarning.setVisibility(View.INVISIBLE);
                beaconPosition.setVisibility(View.INVISIBLE);
                mCurrentBeacon.setText(currentXMLBeacon.getMajor()+"not in range");
                positionX += mapX*5;
                positionY += mapY*5;
                System.out.println("Position X = "+positionX);
                position.setX((float)positionX*pxTodp);
                position.setY((float)positionY*pxTodp);
            }
        }else{
            beaconPosition.setVisibility(View.INVISIBLE);
            locationWarning.setVisibility(View.VISIBLE);
            positionMap.setVisibility(View.INVISIBLE);
            tableDebug.setVisibility(View.INVISIBLE);
        }


    }
    
    private Runnable updateOreintationDisplayTask = new Runnable() {
		public void run() {
			updateOreintationDisplay();
		}
	};

    private Runnable updateUITask = new Runnable(){
        public void run() {
            updateOreintationDisplay();
        }
    };

    // **************************** BEACON FUNCTIONS *********************************
    // BEACON METHODS

    private static XMLBeacon isBeaconKnown(Beacon nearestBeacon, List<XMLLocation> locationList) {
        String nBMajor = Integer.toString(nearestBeacon.getMajor());

        for (XMLLocation loc : locationList) {
            for (XMLBeacon b : loc.getBeaconList()) {
                if (nBMajor.equalsIgnoreCase(b.getMajor())) {
                    XMLBeacon recognizedB = new XMLBeacon();
                    recognizedB = b;
                    currentLocation = loc;
                    return recognizedB;
                }
            }
        }
        return null;
    }

    private static boolean isBeaconInRange(Beacon nearestBeacon){
        if(nearestBeacon.getRssi()>-60){
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        MenuItem refreshItem = menu.findItem(R.id.refresh);
        refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
                getActionBar().setSubtitle("Bluetooth not enabled");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        getActionBar().setSubtitle("Scanning for beacons...");
        beaconList = new ArrayList<Beacon>();
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(SensorFusionActivity.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

}