package ch.hslu.herger.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.LocationReader;
import ch.hslu.herger.sensor.SensorFusionActivity;

/**
 * Shows all available demos.
 *
 * @author Reto Herger
 */
public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    // Read configuration file
    setConfiguration();

    findViewById(R.id.ins_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, SensorFusionActivity.class);
            startActivity(intent);
        }
    });

    findViewById(R.id.sensorFusion_Button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(MainActivity.this, InsActivity.class);
              startActivity(intent);
            }
  });


    findViewById(R.id.XMLReader_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setConfiguration();
        }
    });
  }

    private void setConfiguration() {
        File folder = new File(Environment.getExternalStorageDirectory() + "/iBeaconIndoorLokalisierung");
        if (!folder.exists()) {
            folder.mkdir();
            readConfigFile();
        }else{
            readConfigFile();
        }

    }

    private void readConfigFile() {
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the xml file
        File file = new File(sdcard, "/iBeaconIndoorLokalisierung/config.xml");

        if (file.exists()) {
            //Read text from file
            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();

                String config = text.toString();
                System.out.println("CONFIG = "+config);

                try {
                    ((Configuration) this.getApplication()).setLocationList(LocationReader.readXML(config));
                    Toast.makeText(this, "Configuration is set", Toast.LENGTH_LONG).show();
                } catch (XmlPullParserException e) {
                    Toast.makeText(this, "Error while reading Configuration", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    Toast.makeText(this, "Error while reading Configuration", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } catch (IOException e) {
                //You'll need to add proper error handling here
                Toast.makeText(this, "Error while reading Configuration", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "config.xml not found", Toast.LENGTH_LONG).show();
        }

    }

}
