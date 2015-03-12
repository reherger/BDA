package com.estimote.examples.demos;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.utils.L;

import java.util.ArrayList;
import java.util.List;

import ch.hslu.herger.beacon.BeaconComparator;
import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.LocationReader;
import ch.hslu.herger.config.XMLBeacon;
import ch.hslu.herger.config.XMLLocation;

/**
 * Displays list of found beacons sorted by RSSI.
 * Starts new activity with selected beacon if activity was provided.
 *
 * @author Reto Herger
 */
public class BeaconScannerActivity extends Activity {

  private static final String TAG = BeaconScannerActivity.class.getSimpleName();

  public static final String EXTRAS_TARGET_ACTIVITY = "extrasTargetActivity";
  public static final String EXTRAS_BEACON = "extrasBeacon";

  private static final int REQUEST_ENABLE_BT = 1234;
  private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

  // Constant for converting Px to Dp
  private final float pxTodp = getResources().getDisplayMetrics().density;

  private BeaconManager beaconManager;
  //private LeDeviceListAdapter adapter;
  private List<Beacon> beaconList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.ins);
    getActionBar().setDisplayHomeAsUpEnabled(true);

    // Configure verbose debug logging.
    L.enableDebugLogging(true);

    // Read Configuration
    final List<XMLLocation> locationList = ((Configuration) this.getApplication()).getLocationList();

    // Disable Map until known beacon discovered
    final AbsoluteLayout positionMap = (AbsoluteLayout) findViewById(R.id.positionMap);
    final TextView tvNearestBeacon = (TextView) findViewById(R.id.tvNearestBeacon);
    final ImageView position = (ImageView) findViewById(R.id.position);
    positionMap.setVisibility(View.INVISIBLE);

    // Configure BeaconManager.
    beaconManager = new BeaconManager(this);
    beaconManager.setRangingListener(new BeaconManager.RangingListener() {
      @Override
      public void onBeaconsDiscovered(final Region region, final List<Beacon> beacons) {

          beaconList = beacons;

        // Note that results are not delivered on UI thread.
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // Note that beacons reported here are already sorted by estimated
            // distance between device and beacon.
            getActionBar().setSubtitle("Found beacons: " + beacons.size());

            if(beaconList.size()>0) {
                XMLBeacon currentBeacon = BeaconComparator.isBeaconKnown(beaconList.get(0), locationList);
                if(currentBeacon != null) {

                    positionMap.setVisibility(View.VISIBLE);

                    // set position on Map to position of current beacon
                    position.setX(Float.parseFloat(currentBeacon.getxPos())*pxTodp);
                    position.setY(Float.parseFloat(currentBeacon.getyPos())*pxTodp);

                    tvNearestBeacon.setText("Nearest beacon: " + currentBeacon.getMajor() +"Beacon RSSI: "+beaconList.get(0).getRssi() + "Meassured Power: "+beaconList.get(0).getMeasuredPower());
                }
            }else{
                positionMap.setVisibility(View.INVISIBLE);

                tvNearestBeacon.setText("No beacon in range");
            }
          }
        });
      }
    });
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
  protected void onStop() {
    try {
      beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
    } catch (RemoteException e) {
      Log.d(TAG, "Error while stopping ranging", e);
    }

    super.onStop();
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
    getActionBar().setSubtitle("Scanning...");
    beaconList = new ArrayList<Beacon>();
    beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
      @Override
      public void onServiceReady() {
        try {
          beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
          Toast.makeText(BeaconScannerActivity.this, "Cannot start ranging, something terrible happened",
              Toast.LENGTH_LONG).show();
          Log.e(TAG, "Cannot start ranging", e);
        }
      }
    });
  }


}
