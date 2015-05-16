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
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.XMLBeacon;
import ch.hslu.herger.config.XMLDoor;
import ch.hslu.herger.config.XMLLocation;
import ch.hslu.herger.config.XMLRoom;
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

    private float MINSPEED = 1.0f;
    private float MAXSPEED = 2.2f;

    private int noMovementCount = 0;

    private double mapX;
    private double mapY;
    private double positionX;
    private double lastPositionX;
    private double positionY;
    private double lastPositionY;


 
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
 
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f; // Nanoseconds to Seconds
    private static final double D2RAD = (2*Math.PI/360); // Degree to Radian
    private float PX2DP; // support for different resolutions
	private float timestamp;
    private float timestampAccel;

	private boolean initState = true;
    
	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private Timer fuseTimer = new Timer();
	
	// The following members are only for displaying the sensor output.
	public Handler mHandler;
	private TextView mNorthView;
	private TextView mAccuracyView;
    private TextView mCurrentBeacon;

    private AbsoluteLayout positionMap;
    private ImageView map;
    private ImageView position;
    private ImageView beaconPosition;

    private RelativeLayout locationWarning;
    private RelativeLayout probabilityWarning;

    private boolean running = false;
    private boolean inBeaconRange = false;
    private float timePassed;
    private float probability;
    private float dTAccel;


	DecimalFormat d = new DecimalFormat("#.##");

    private static final String TAG = SensorFusionActivity.class.getSimpleName();

    public static final String EXTRAS_TARGET_ACTIVITY = "extrasTargetActivity";
    public static final String EXTRAS_BEACON = "extrasBeacon";

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private DisplayMetrics metrics;

    private BeaconManager beaconManager;
    private List<Beacon> beaconList;
    private Beacon currentBeacon;
    private XMLBeacon currentXMLBeacon;

    private static XMLLocation currentLocation;
    private static XMLRoom currentRoom;
    private static List<XMLDoor> currentDoorList;
    private static double ratio; // ratio for meters on map to dynamic pixels

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
        d.setMaximumFractionDigits(0);
        d.setMinimumFractionDigits(0);
        mNorthView = (TextView)findViewById(R.id.textView4);
        mAccuracyView = (TextView)findViewById(R.id.textView5);
        mCurrentBeacon = (TextView)findViewById(R.id.textViewCurrentBeaconValue);

        positionMap = (AbsoluteLayout) findViewById(R.id.positionMap);
        map = (ImageView)findViewById(R.id.map);

        positionMap.setVisibility(View.INVISIBLE);
        position = (ImageView) findViewById(R.id.position);
        beaconPosition = (ImageView) findViewById(R.id.beaconPosition);

        locationWarning = (RelativeLayout) findViewById(R.id.locationWarning);
        probabilityWarning = (RelativeLayout) findViewById(R.id.probabilityWarning);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set Display Metrics
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        PX2DP = metrics.density;

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

    // Transforms AccelerationVectors from Device coordinate system to world coordinate system
    public void calcAccelInWorldCoordinates(long time) {

        worldLinearAccel[0] = (float) (linearAccel[0] * (Math.cos(fusedOrientation[2]) * Math.cos(fusedOrientation[0])
                + Math.sin(fusedOrientation[2]) * Math.sin(fusedOrientation[1]) * Math.sin(fusedOrientation[0]))
                + linearAccel[1] * (Math.cos(fusedOrientation[1]) * Math.sin(fusedOrientation[0]))
                + linearAccel[2] * (-Math.sin(fusedOrientation[2]) * Math.cos(fusedOrientation[0])
                + Math.cos(fusedOrientation[2]) * Math.sin(fusedOrientation[1]) * Math.sin(fusedOrientation[0])));
        worldLinearAccel[1] = (float) (linearAccel[0] * (-Math.cos(fusedOrientation[2]) * Math.sin(fusedOrientation[0])
                + Math.sin(fusedOrientation[2]) * Math.sin(fusedOrientation[1]) * Math.cos(fusedOrientation[0]))
                + linearAccel[1] * (Math.cos(fusedOrientation[1]) * Math.cos(fusedOrientation[0]))
                + linearAccel[2] * (Math.sin(fusedOrientation[2]) * Math.sin(fusedOrientation[0])
                + Math.cos(fusedOrientation[2]) * Math.sin(fusedOrientation[1]) * Math.cos(fusedOrientation[0])));

        // call Distance Calculation method
        calcDistanceVectors(time);
    }

    public void calcDistanceVectors(long time) {
        if(timestampAccel != 0) {
            dTAccel = (time - timestampAccel) * NS2S;
            tempSpeed[0] = worldLinearAccel[0]*dTAccel;
            tempSpeed[1] = worldLinearAccel[1]*dTAccel;

            // ignore small acceleration
            if(worldLinearAccel[0]>0.25f){
                calcProbability();
                if(speed[0] < MINSPEED){
                    speed[0] = MINSPEED;
                    speed[0] += tempSpeed[0];
                    distance[0] = speed[0]*dTAccel;
                }
                else if(speed[0] > MAXSPEED){
                    speed[0] = MAXSPEED;
                    distance[0] = speed[0]*dTAccel;
                }else {
                    speed[0] += tempSpeed[0];
                    distance[0] = speed[0]*dTAccel;
                }
                noMovementCount = 0;
            }else {
                noMovementCount++;
            }
            if(worldLinearAccel[1]>0.25f){
                calcProbability();
                if(speed[1] < MINSPEED){
                    speed[1] = MINSPEED;
                    speed[1] += tempSpeed[1];
                    distance[1] = speed[1]*dTAccel;
                }
                else if(speed[1] > MAXSPEED){
                    speed[1] = MAXSPEED;
                    distance[1] = speed[1]*dTAccel;
                }else {
                    speed[1] += tempSpeed[1];
                    distance[1] = speed[1]*dTAccel;
                    System.out.println("distance Y = "+distance[1]);
                }
                noMovementCount = 0;
            }else {
                noMovementCount++;
                // too long no movement -> reset speed
                if(noMovementCount > 10){
                    // reset speed & distance
                    speed[1] = 0;
                    speed[0] = 0;
                    distance[1] = 0;
                    distance[0] = 0;
                    noMovementCount = 0;
                }
            }
            // set current timestamp to old timestamp
            timestampAccel = time;
        }else{
            // ignore first event
            timestampAccel = time;
        }

        // Call Calculation for Distances on Map and calls UI Handler
        calcDistanceOnMap();
    }

    // Calculates Distances on Map and updates UI
    public void calcDistanceOnMap(){
        if(currentLocation != null) {

            float angleToNorth = Float.valueOf(currentLocation.getAngleToNorth());

            double deviceDirection = fusedOrientation[0]* 180/Math.PI;
            double[] rotatedVektor = new double[2];
            // decrease influence of x direction
            double distanceX = distance[0]*0.2d;
            double distanceY = distance[1];

            double diff = angleToNorth - deviceDirection;

            // calcutlation rotatedVector with rotationMatrix
            rotatedVektor[0] = distanceX*Math.cos(diff*D2RAD) - distanceY*Math.sin(diff*D2RAD);
            rotatedVektor[1] = distanceX*Math.sin(diff*D2RAD) + distanceY*Math.cos(diff*D2RAD);


            // set vectors for displaying on map
            // Y-Vector * -1 to get correct direction
            mapX = rotatedVektor[0];
            System.out.println("MAP X = "+mapX);
            mapY = rotatedVektor[1] * -1;

        }

        mHandler.post(updateUITask);
    }

    public void prohibitWallCrossing(){
        if(currentRoom != null && currentDoorList != null) {
            // get postion of walls
            double minX = Double.valueOf(currentRoom.getXleftUpperCorner());
            double maxX = minX + Double.valueOf(currentRoom.getWidth());
            double minY = Double.valueOf(currentRoom.getYleftUpperCorner());
            double maxY = minY + Double.valueOf(currentRoom.getHeight());


            if (positionX >= minX && positionX <= maxX) {
                // everything OK
            } else {
                for (XMLDoor door : currentDoorList) {
                    double dMinX = Double.valueOf(door.getxLeftUpperCorner());
                    double dMaxX = dMinX + Double.valueOf(door.getAreaWidth());
                    double dMinY = Double.valueOf(door.getyLeftUpperCorner());
                    double dMaxY = dMinY + Double.valueOf(door.getAreaHeight());
                    if (positionX >= dMinX && positionX <= dMaxX && positionY >= dMinY && positionY <= dMaxY) {
                        // everything OK
                    } else {
                        // reset position to last known
                        positionX = lastPositionX;

                        probability -= 2f;
                    }
                }
            }
            if (positionY >= minY && positionY <= maxY) {
                // everything OK
            } else {
                // TODO check if value is in Door area
                for (XMLDoor door : currentDoorList) {
                    double dMinX = Double.valueOf(door.getxLeftUpperCorner());
                    double dMaxX = dMinX + Double.valueOf(door.getAreaWidth());
                    double dMinY = Double.valueOf(door.getyLeftUpperCorner());
                    double dMaxY = dMinY + Double.valueOf(door.getAreaHeight());
                    if (positionY >= dMinY && positionY <= dMaxY && positionX >= dMinX && positionX <= dMaxX) {
                        // everything OK
                    } else {
                        positionY = lastPositionY;

                        probability -= 2f;
                    }
                }
            }
        }
    }

    private void calcProbability(){
        timePassed += dTAccel;
        probability = (1f - (timePassed / 30f)) * 100f; // accuracy = 0 after 20sec of not reaching a beacon (cause called twice in calcDistanceVectors())

    }

    private void resetProbability(){
        timePassed = 0f;
        probability = 100f;
    }

    // **************************** GUI FUNCTIONS *********************************

    public void updateOreintationDisplay() {
    	// case 0 was accMagOrientation
        // case 1 was gyroOrientation
        if(running){
            mNorthView.setText("North: "+d.format(fusedOrientation[0] * 180/Math.PI) + "°");
            mAccuracyView.setText("Probability: "+d.format(probability)+"%");
            if(inBeaconRange){
                resetProbability();
                locationWarning.setVisibility(View.INVISIBLE);
                probabilityWarning.setVisibility(View.INVISIBLE);
                mCurrentBeacon.setText("Nearest Beacon: " + currentXMLBeacon.getMajor() + " / RSSI: " + currentBeacon.getRssi());
                positionMap.setVisibility(View.VISIBLE);
                mCurrentBeacon.setVisibility(View.VISIBLE);
                positionX = Float.parseFloat(currentXMLBeacon.getxPos());
                positionY = Float.parseFloat(currentXMLBeacon.getyPos());
                position.setX((float) positionX * PX2DP);
                position.setY((float) positionY * PX2DP);
                beaconPosition.setVisibility(View.VISIBLE);
                beaconPosition.setX((float) positionX * PX2DP);
                beaconPosition.setY((float) positionY * PX2DP);
            }else{
                if(probability < 50f){
                    //probabilityWarning.setVisibility(View.VISIBLE);
                }else {
                    locationWarning.setVisibility(View.INVISIBLE);
                    probabilityWarning.setVisibility(View.INVISIBLE);
                    beaconPosition.setVisibility(View.INVISIBLE);
                    mCurrentBeacon.setText(currentXMLBeacon.getMajor() + " (not in range)");
                    lastPositionX = positionX;
                    lastPositionY = positionY;
                    positionX += mapX * ratio;
                    positionY += mapY * ratio;
                    prohibitWallCrossing();
                    position.setX((float) positionX * PX2DP);
                    position.setY((float) positionY * PX2DP);
                }
            }
        }else{
            locationWarning.setVisibility(View.VISIBLE);
            beaconPosition.setVisibility(View.INVISIBLE);
            probabilityWarning.setVisibility(View.INVISIBLE);
            positionMap.setVisibility(View.INVISIBLE);
        }
    }

    private Runnable updateUITask = new Runnable(){
        public void run() {
            updateOreintationDisplay();
        }
    };

    private void loadCorrectBackgroundMap(String path){
        File file = new File(Environment.getExternalStorageDirectory() + "/iBeaconIndoorLokalisierung/"+path);
        if(file.exists()){
            Drawable mDrawable = Drawable.createFromPath(file.getAbsolutePath());
            map.setImageDrawable(mDrawable);
        }
    }

    // **************************** BEACON FUNCTIONS *********************************
    // BEACON METHODS

    // Checks if Beacon is known by searching in all configured locations
    // sets current location, currentRoom, currentDoorList
    // returns nearest Beacon
    private XMLBeacon isBeaconKnown(Beacon nearestBeacon, List<XMLLocation> locationList) {
        String nBMajor = Integer.toString(nearestBeacon.getMajor());

        for (XMLLocation loc : locationList) {
            for (XMLBeacon b : loc.getBeaconList()) {
                if (nBMajor.equalsIgnoreCase(b.getMajor())) {
                    XMLBeacon recognizedB = new XMLBeacon();
                    recognizedB = b;
                    currentLocation = loc;
                    ratio = Double.valueOf(currentLocation.getRatio());
                    loadCorrectBackgroundMap(loc.getPathToMap());
                    // set current Room
                    for (XMLRoom r : loc.getRoomList()){
                        if(b.getMinor().equalsIgnoreCase(r.getRoomId())){
                            currentRoom = r;
                            currentDoorList = r.getDoorList();
                        }
                    }

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