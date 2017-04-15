package mcgroup16.asu.com.group16.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import mcgroup16.asu.com.group16.R;
import mcgroup16.asu.com.group16.model.Row;
import mcgroup16.asu.com.group16.utility.DatabaseUtil;

public class DataCollectActivity extends AppCompatActivity implements SensorEventListener {

    private final String TAG = DataCollectActivity.this.getClass().getSimpleName();

    // accelerometer related declarations
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private double[] sensorData = null;
    private ArrayList<Double> trainingArray;
    private static final int SAMPLING_FREQUENCY = 100;
    private static final int SAMPLE_COLUMN_COUNT = 150;

    // general declarations
    private int activityLabel;
    private static final String RADIO_RUN_OPTION = "Run";
    private static final String RADIO_WALK_OPTION = "Walk";
    private static final int LABEL_RUN = 1;
    private static final int LABEL_WALK = 2;
    private static final int LABEL_EAT = 3;
    private Button btnCollectData = null;
    private Button btnTrainModel = null;
    private Button btnCheckData = null;
    private Button btnPredict = null;
    private static final int DATA_THRESHOLD = 30;

    // handler related declarations
    private Handler insertHandle = null;

    // Database utility related declarations
    private String DB_NAME = null;
    private String TABLE_NAME = "training_table";
    private DatabaseUtil dbHelper = null;

    // file handling declarations
    BufferedWriter bw = null;
    BufferedReader br = null;
    String row = null;
    private String trainOrTestFile = null;
    String trainFileName = "train";
    String testFileName = "test";
    String modelFileName = "model";
    String predcitFileName = "output";

    //SVM files
    private String storagePath;
    private String appDataPath;
    private String appDataTrainingPath;
    private String appDataModelPath;
    private String appDataTestPath;
    private String appDataPredictPath;

    //Native methods
    static {
        System.loadLibrary("jnilibsvm");
    }

    // Credit - library taken from - https://github.com/yctung/AndroidLibSvm
    private native void jniSvmTrain(String cmd);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collect);

        // performing initial operations
        initDataPaths();
        createFolders();
        startAccelerometerSensor();
        btnTrainModel = (Button) findViewById(R.id.btn_train);
        btnTrainModel.setEnabled(false);

        btnPredict = (Button) findViewById(R.id.btnPredict);
        btnPredict.setEnabled(false);

        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");

        // DB handler instance initialization
        dbHelper = new DatabaseUtil(this, DB_NAME);
        dbHelper.createTable(TABLE_NAME);

        btnCollectData = (Button) findViewById(R.id.btn_collect_data);
        btnCollectData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                RadioGroup radioGroupDataChoice = (RadioGroup) findViewById(R.id.radio_data_choice);
                int choice = radioGroupDataChoice.getCheckedRadioButtonId();
                RadioButton radioButtonChoice = (RadioButton) findViewById(choice);
                String choiceOption = radioButtonChoice.getText().toString();

                if (choiceOption.equalsIgnoreCase(trainFileName)) {
                    trainOrTestFile = trainFileName;
                } else {
                    trainOrTestFile = testFileName;
                }

                RadioGroup radioGroupCollectData = (RadioGroup) findViewById(R.id.radio_collect_data);
                int selectedId = radioGroupCollectData.getCheckedRadioButtonId();
                RadioButton radioButtonCollect = (RadioButton) findViewById(selectedId);
                String collectOption = radioButtonCollect.getText().toString();

                if (collectOption.equals(RADIO_RUN_OPTION)) {
                    activityLabel = LABEL_RUN;
                } else if (collectOption.equals(RADIO_WALK_OPTION)) {
                    activityLabel = LABEL_WALK;
                } else {
                    activityLabel = LABEL_EAT;
                }

                // file handling operations prior to starting collecting data
                FileWriter fw = null;
                try {
                    File dataFile = new File(appDataPath, trainOrTestFile);
                    if (!dataFile.exists()) {
                        dataFile.createNewFile();
                    }
                    fw = new FileWriter(dataFile.getAbsoluteFile(), true);
                    bw = new BufferedWriter(fw);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Error:" + e, Toast.LENGTH_SHORT).show();
                }
                trainingArray = new ArrayList<Double>();
                insertHandle = new Handler();
                insertHandle.post(insertIntoTrainTestArray);
                Toast.makeText(getApplicationContext(), "Data collection started", Toast.LENGTH_SHORT).show();
            }
        });

        btnCheckData = (Button) findViewById(R.id.btn_check_data);
        btnCheckData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    RadioGroup radioGroupDataChoice = (RadioGroup) findViewById(R.id.radio_data_choice);
                    int choice = radioGroupDataChoice.getCheckedRadioButtonId();
                    RadioButton radioButtonChoice = (RadioButton) findViewById(choice);
                    String choiceOption = radioButtonChoice.getText().toString();

                    if (choiceOption.equalsIgnoreCase(trainFileName)) {
                        trainOrTestFile = trainFileName;
                    } else {
                        trainOrTestFile = testFileName;
                    }

                    char actLabel = '\0';
                    RadioGroup radioGroupCollectData = (RadioGroup) findViewById(R.id.radio_collect_data);
                    int selectedId = radioGroupCollectData.getCheckedRadioButtonId();
                    RadioButton radioButtonCollect = (RadioButton) findViewById(selectedId);
                    String collectOption = radioButtonCollect.getText().toString();
                    if (collectOption.equals(RADIO_RUN_OPTION)) {
                        actLabel = '1';
                    } else if (collectOption.equals(RADIO_WALK_OPTION)) {
                        actLabel = '2';
                    } else {
                        actLabel = '3';
                    }

                    File trainFile = new File(appDataPath, trainOrTestFile);
                    br = new BufferedReader(new FileReader(trainFile));
                    String readLine = "";
                    int dataCount = 0;
                    while ((readLine = br.readLine()) != null) {
                        char label = readLine.charAt(0);
                        if (label == actLabel) {
                            dataCount++;
                        }
                    }
                    Toast.makeText(getApplicationContext(), "Data count for " + actLabel + " is: " + dataCount, Toast.LENGTH_SHORT).show();

                    if (dataCount == DATA_THRESHOLD) {
                        btnTrainModel.setEnabled(true);
                        btnPredict.setEnabled(true);
                    }


                } catch (IOException e) {
                    Log.e(TAG, "Error: " + e);
                }
            }
        });

        btnTrainModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svmTrain();
                Toast.makeText(getApplicationContext(), "Training completed", Toast.LENGTH_SHORT).show();
            }
        });

        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent moveToPredictActivity = new Intent(getApplicationContext(), PredictActivity.class);
                moveToPredictActivity.putExtra("EXTRA_DB_NAME", DB_NAME);
                moveToPredictActivity.putExtra("EXTRA_FROM_ACTIVITY", "Collect");
                startActivity(moveToPredictActivity);
            }
        });
    }

    private Runnable insertIntoTrainTestArray = new Runnable() {
        @Override
        public void run() {

            if (trainingArray.size() < SAMPLE_COLUMN_COUNT) {
                trainingArray.add(sensorData[0]);
                trainingArray.add(sensorData[1]);
                trainingArray.add(sensorData[2]);
                insertHandle.postDelayed(this, SAMPLING_FREQUENCY);

            } else if (trainingArray.size() == SAMPLE_COLUMN_COUNT) {

                insertHandle.removeCallbacksAndMessages(null);
                row = String.valueOf(activityLabel);
                for (int i = 0; i < SAMPLE_COLUMN_COUNT; i++) {
                    row += " " + (i + 1) + ":" + trainingArray.get(i);
                }
                Log.e("DataCollection", row);
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
                // adding row to database
                Row databaseRow = new Row(trainingArray, String.valueOf(activityLabel));
                dbHelper.addRow(databaseRow, TABLE_NAME);
            }
        }
    };

    private void startAccelerometerSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorData = new double[4];
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
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

    private void initDataPaths() {
        storagePath = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        appDataPath = storagePath + "libsvm";
        appDataTrainingPath = appDataPath + "/" + trainFileName;
        appDataTestPath = appDataPath + "/" + testFileName;
        appDataModelPath = appDataPath + "/" + modelFileName;
        appDataPredictPath = appDataPath + "/" + predcitFileName;
    }

    private void svmTrain() {
        String svmOptions = "-t 2 ";
        jniSvmTrain(svmOptions + appDataTrainingPath + " " + appDataModelPath + " ");
    }

    private void createFolders() {
        File folder = new File(appDataPath);
        if (folder.exists()) {
            removeDirectory(folder);
        }
        folder.mkdir();
    }

    // reference taken from a post on stackoverflow.com
    private void removeDirectory(File dir) {
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
