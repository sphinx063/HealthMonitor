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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import mcgroup16.asu.com.group16.R;
import mcgroup16.asu.com.group16.utility.DatabaseUtil;

public class DataCollectActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private double[] sensorData = null;
    private ArrayList<Double> trainingArray = new ArrayList<Double>();
    private int activityLabel;
    private static final String RUN_OPTION = "Run";
    private static final String WALK_OPTION = "Walk";
    private Button btnCollectData = null;
    private Button btnTrainModel = null;
    private Button btnCollectTest = null;
    private Button btnCheckData = null;
    private Button btnCheckTest = null;
    private String trainOrTestFile = null;

    // handler related declarations
    private static final int HAS_FINISHED = 1;
    private Handler insertHandle = null;
    private boolean isFinished = false;

    // Database utility related declarations
    private String DB_NAME = null;
    private String TABLE_NAME = "training_table";
    private DatabaseUtil dbHelper = null;

    // file handling declarations
    FileOutputStream outputStream = null;
    BufferedWriter bw = null;
    BufferedReader br = null;
    File trainingFile = null;
    String row = null;
    FileInputStream fin = null;
    String trainFileName = "train";
    String testFileName = "test";

    //SVM files
    private String storagePath;
    private String appDataPath;
    private String appDataTrainingPath;
    private String appDataModelPath;
    private String appDataTestPath;

    //Native methods
    static {
        System.loadLibrary("jnilibsvm");
    }

//    private native void jniSvmTrain(String cmd);
//    private native void jniSvmPredict(String cmd);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collect);

        initDataPaths();
        createFolders();
        initiateAccelerometer();

//        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");
//
//        // DB handler instance initialization
//        dbHelper = new DatabaseUtil(this, DB_NAME);
//        dbHelper.createTable(TABLE_NAME);

        btnCollectData = (Button) findViewById(R.id.btn_collect_data);
        btnCollectData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                RadioGroup radioGroupDataChoice = (RadioGroup) findViewById(R.id.radio_data_choice);
                int choice = radioGroupDataChoice.getCheckedRadioButtonId();
                RadioButton radioButtonChoice = (RadioButton) findViewById(choice);
                String choiceOption = radioButtonChoice.getText().toString();

                if (choiceOption.equals("Train")) {
                    trainOrTestFile = trainFileName;
                } else {
                    trainOrTestFile = testFileName;
                }

                RadioGroup radioGroupCollectData = (RadioGroup) findViewById(R.id.radio_collect_data);
                int selectedId = radioGroupCollectData.getCheckedRadioButtonId();
                RadioButton radioButtonCollect = (RadioButton) findViewById(selectedId);
                String collectOption = radioButtonCollect.getText().toString();

                if (collectOption.equals(RUN_OPTION)) {
                    activityLabel = 0;
                } else if (collectOption.equals(WALK_OPTION)) {
                    activityLabel = 1;
                } else {
                    activityLabel = 2;
                }

                // initializing file handling operations prior to starting collecting data
                FileWriter fw = null;
                try {
                    File dataFile = new File(appDataPath, trainOrTestFile);
                    if (!dataFile.exists()) {
                        dataFile.createNewFile();
                    }
                    fw = new FileWriter(dataFile.getAbsoluteFile(), true);
                    bw = new BufferedWriter(fw);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Error occured while initializing file", Toast.LENGTH_SHORT).show();
                }

                insertHandle = new Handler();
                insertHandle.post(insertIntoTrainTestArray);

            }
        });

        btnTrainModel = (Button) findViewById(R.id.btn_train);
        btnTrainModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svmTrain();
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

                    if (choiceOption.equals("Train")) {
                        trainOrTestFile = trainFileName;
                    } else {
                        trainOrTestFile = testFileName;
                    }

                    char actLabel = '\0';
                    RadioGroup radioGroupCollectData = (RadioGroup) findViewById(R.id.radio_collect_data);
                    int selectedId = radioGroupCollectData.getCheckedRadioButtonId();
                    RadioButton radioButtonCollect = (RadioButton) findViewById(selectedId);
                    String collectOption = radioButtonCollect.getText().toString();

                    if (collectOption.equals(RUN_OPTION)) {
                        actLabel = '0';
                    } else if (collectOption.equals(WALK_OPTION)) {
                        actLabel = '1';
                    } else {
                        actLabel = '2';
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
                    Toast.makeText(getApplicationContext(), "Data count for " + activityLabel + " is: " + dataCount, Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Runnable insertIntoTrainTestArray = new Runnable() {
        @Override
        public void run() {
            if (trainingArray.size() < 150 && !isFinished) {
                trainingArray.add(sensorData[0]);
                trainingArray.add(sensorData[1]);
                trainingArray.add(sensorData[2]);
                insertHandle.postDelayed(this, 100);

            } else if (trainingArray.size() == 150) {
                isFinished = true;
                row = String.valueOf(activityLabel);
                for (int i = 0; i < 150; i++) {
                    row += " " + (i + 1) + ":" + trainingArray.get(i);
                }
                try {
                    bw.write(row);
                    bw.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // adding row to database
//                Row row = new Row(trainingArray, activityLabel);
//                dbHelper.addRow(row, TABLE_NAME);
            }
        }
    };

    private void initiateAccelerometer() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
        storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        appDataPath = storagePath + "libsvm";
        appDataTrainingPath = appDataPath + "/" + trainFileName;
        appDataTestPath = appDataPath + "/" + "test";
        appDataModelPath = appDataPath + "/" + "model";

    }

    private void svmTrain() {
        String svmOptions = "-t 2 ";
//        jniSvmTrain(svmOptions + appDataTrainingPath + " " + appDataModelPath + " ");
    }

    private void createFolders() {
        File folder = new File(appDataPath);
        if (folder.exists()) {
            removeDirectory(folder);
        }
        folder.mkdir();
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
