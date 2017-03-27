package mcgroup16.asu.com.group16.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

import mcgroup16.asu.com.group16.R;

public class PredictActivity extends AppCompatActivity implements SensorEventListener {

    private final String TAG = PredictActivity.this.getClass().getSimpleName();

    // general declarations
    private static final String ACTION_RUN = " Running";
    private static final String ACTION_WALK = " Walking";
    private static final String ACTION_EAT = " Eating";
    private static final int LABEL_RUN = 1;
    private static final int LABEL_WALK = 2;
    private static final int LABEL_EAT = 3;

    // Accelerometer related declarations
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private double[] sensorData = null;
    private ArrayList<Double> trainingArray;

    //SVM files path
    private String storagePath;
    private String appDataPath;
    private String appDataSamplePath;
    private String appDataModelPath;
    private String appDataPredictPath;

    // file handling declarations
    BufferedWriter bw = null;
    BufferedReader br = null;
    String row = null;
    String sampleFileName = "sample";
    String modelFileName = "model";
    String outputFileName = "output";

    // handler related declarations
    private Handler insertHandle = null;

    //Native methods
    static {
        System.loadLibrary("jnilibsvm");
    }

    private native void jniSvmPredict(String cmd);

    private void svmPredict() {
        jniSvmPredict(appDataSamplePath + " " + appDataModelPath + " " + appDataPredictPath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        initDataPaths();
        createFolders();
        initiateAccelerometer();

        final Button btnCollect = (Button) findViewById(R.id.btn_create_data);
        btnCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Data collection started", Toast.LENGTH_SHORT).show();
                btnCollect.setEnabled(false);

                FileWriter fw = null;
                try {

                    File sampleDataFile = new File(appDataPath, appDataSamplePath);
                    if (!sampleDataFile.exists()) {
                        sampleDataFile.createNewFile();
                    }
                    fw = new FileWriter(sampleDataFile.getAbsoluteFile(), true);
                    bw = new BufferedWriter(fw);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error: " + e);
                }
                trainingArray = new ArrayList<Double>();
                insertHandle = new Handler();
                insertHandle.post(insertIntoTrainTestArray);
            }
        });

        Button btnPredict = (Button) findViewById(R.id.btn_predict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svmPredict();
                Log.i(TAG, "SVM predict completed");

                try {
                    File outputFile = new File(appDataPath, outputFileName);
                    br = new BufferedReader(new FileReader(outputFile));
                    String readLine = "";
                    while ((readLine = br.readLine()) != null) {
                        int label = (int) readLine.charAt(0);
                        TextView txtPredict = (TextView) findViewById(R.id.txtPredict);
                        switch (label) {
                            case LABEL_RUN:
                                txtPredict.setText(ACTION_RUN);
                                break;
                            case LABEL_WALK:
                                txtPredict.setText(ACTION_WALK);
                                break;
                            case LABEL_EAT:
                                txtPredict.setText(ACTION_EAT);
                                break;
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error: " + e);
                }
                Toast.makeText(getApplicationContext(), "Prediction completed", Toast.LENGTH_SHORT).show();
                btnCollect.setEnabled(true);
            }
        });
    }

    private Runnable insertIntoTrainTestArray = new Runnable() {
        @Override
        public void run() {
            if (trainingArray.size() < 150) {
                trainingArray.add(sensorData[0]);
                trainingArray.add(sensorData[1]);
                trainingArray.add(sensorData[2]);
                insertHandle.postDelayed(this, 100);

            } else if (trainingArray.size() == 150) {
                insertHandle.removeCallbacksAndMessages(null);
                for (int i = 0; i < 150; i++) {
                    row += " " + (i + 1) + ":" + trainingArray.get(i);
                }
                try {
                    bw.write(row);
                    bw.newLine();
                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e);
                } finally {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error: " + e);
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        sensorData[0] = sensorEvent.values[0];
        sensorData[1] = sensorEvent.values[1];
        sensorData[2] = sensorEvent.values[2];
        sensorData[3] = sensorEvent.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "Accuracy Changed");
    }

    private void initiateAccelerometer() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorData = new double[4];
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void initDataPaths() {
        storagePath = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        appDataPath = storagePath + "libsvm";
        appDataSamplePath = appDataPath + "/" + sampleFileName;
        appDataModelPath = appDataPath + "/" + modelFileName;
        appDataPredictPath = appDataPath + "/" + outputFileName;
        Log.i(TAG, "App data path: " + appDataPath);
        Log.i(TAG, "Sample data storage path: " + appDataSamplePath);
    }

    private void createFolders() {
        File folder = new File(appDataPath);
        if (folder.exists()) {
            removeDirectory(folder);
        }
        boolean created = folder.mkdir();
    }

    private static void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }

}
