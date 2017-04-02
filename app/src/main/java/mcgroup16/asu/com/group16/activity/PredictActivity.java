package mcgroup16.asu.com.group16.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android.util.Log;

import mcgroup16.asu.com.group16.R;

enum SVM {
    SVM_TYPE("svm_type"),
    KERNEL_TYPE("kernel_type"),
    SV_COUNT("total_sv"),
    GAMMA("gamma");
    private final String parameter;
    private SVM(String s) {
        parameter = s;
    }
    public boolean equals(String otherParameter) {
        return parameter.equals(otherParameter);
    }
    public String toString() {
        return this.parameter;
    }
}

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

    // file handling declarations
    BufferedWriter bw = null;
    BufferedReader br = null;
    String row = null;
    String trainFileName = "train";
    String testFileName = "test";
    String modelFileName = "model";
    String predcitFileName = "output";
    private String storagePath;
    private String appDataPath;
    private String appDataTrainingPath;
    private String appDataModelPath;
    private String appDataTestPath;
    private String appDataPredictPath;
    //SVM Parameter fields
    private TextView gammaText = null;
    private TextView kernelTypeText = null;
    private TextView supportVectorText = null;
    //Accuracy on the already trained model
    private TextView accuracyText = null;
    // handler related declarations
    private Handler insertHandle = null;

    //Load native SVM library
    static {
        System.loadLibrary("jnilibsvm");
    }

   // private native void jniSvmPredict(String cmd);

    private void svmPredict() {
       // jniSvmPredict(appDataTestPath + " " + appDataModelPath + " " + appDataPredictPath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);
        accuracyText = (TextView)findViewById(R.id.txtValidate);
        Intent intent = getIntent();
        String fromActivity = intent.getStringExtra("EXTRA_FROM_ACTIVITY");
        initDataPaths();
        //If coming from Main activity clear libsvm folder and copy train,model,test files from asset
        if (fromActivity.equalsIgnoreCase("Main")) {
            createFolders();
            copyAssets(1);
            accuracyText.setText(String.valueOf(computeAccuracy()));
        }
        Map<String, String> map = (HashMap<String, String>) getModelParameters();
        populateSVMParameters(map);
        initiateAccelerometer();
        final Button btnCollect = (Button) findViewById(R.id.btnCreateData);
        btnCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Data collection started", Toast.LENGTH_SHORT).show();
                btnCollect.setEnabled(false);

                FileWriter fw = null;
                try {

                    File sampleDataFile = new File(appDataPath, appDataTestPath);
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

        Button btnPredict = (Button) findViewById(R.id.btnPredict);
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svmPredict();
                Log.i(TAG, "SVM predict completed");

                try {
                    File outputFile = new File(appDataPath, predcitFileName);
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
        appDataTrainingPath = appDataPath + "/" + trainFileName;
        appDataTestPath = appDataPath + "/" + testFileName;
        appDataModelPath = appDataPath + "/" + modelFileName;
        appDataPredictPath = appDataPath + "/" + predcitFileName;
        Log.i(TAG, "App data path: " + appDataPath);
        Log.i(TAG, "test data path: " + appDataTestPath);
        Log.i(TAG, "model path: " + appDataModelPath);
        Log.i(TAG, "predicted data path: " + appDataPredictPath);
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

    private void copyAssets(int copyAll) {
        AssetManager assetManager = getAssets();
        if (copyAll == 0) {
            duplicateAsset(assetManager, appDataTrainingPath, trainFileName);
            duplicateAsset(assetManager, appDataTestPath, testFileName);
        }
        if (copyAll == 1) {
            duplicateAsset(assetManager, appDataTrainingPath, trainFileName);
            duplicateAsset(assetManager, appDataModelPath, modelFileName);
            duplicateAsset(assetManager, appDataTestPath, testFileName);
        }
    }

    private void duplicateAsset(AssetManager assetManager, String fileTo, String fileFrom) {
        InputStream instream = null;
        OutputStream outstream = null;
        try {
            instream = assetManager.open(fileFrom);
            outstream = new FileOutputStream(new File(fileTo), false);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = instream.read(buffer)) > 0) {
                outstream.write(buffer, 0, length);
            }
            instream.close();
            outstream.flush();
            outstream.close();
            Log.e("AssetDuplicate", "Copied file " + fileFrom + " to " + fileTo);

        } catch (IOException e) {
            Log.e("AssetDuplicate", "Could not copy asset file " + fileFrom + " to " + fileTo);
            e.printStackTrace();
        }
    }

    private Map<String, String> getModelParameters() {
        File model = new File(appDataModelPath);
        HashMap<String, String> modelParameters = new HashMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(model)));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("SV")) {
                    break;
                }
                String[] tokens = line.split("\\s+");
                String parameter = tokens[0];
                String value = tokens[1];
                modelParameters.put(parameter, value);
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return modelParameters;
    }

    private void populateSVMParameters(Map<String, String> parameterMap) {
        gammaText = (TextView) findViewById(R.id.txtGamma);
        kernelTypeText = (TextView) findViewById(R.id.txtKernel);
        supportVectorText = (TextView) findViewById(R.id.txtSvcount);
        gammaText.setText(parameterMap.get(SVM.GAMMA));
        kernelTypeText.setText(parameterMap.get(SVM.KERNEL_TYPE));
        supportVectorText.setText(parameterMap.get(SVM.SV_COUNT));

    }
    private double computeAccuracy(){
        File predictFile = new File(appDataPredictPath);
        File testFile = new File(appDataTestPath);
        int rowNumber = 0;
        int totalCorrectPrediction = 0;
        ArrayList<String> testLabels = new ArrayList<>();
        try {
            BufferedReader brPredict = new BufferedReader(new InputStreamReader(new FileInputStream(predictFile)));
            BufferedReader brTest = new BufferedReader(new InputStreamReader(new FileInputStream(predictFile)));
            String line = null;
            while ((line = brTest.readLine())!=null){
                String[] columns = line.trim().split("\\s+");
                testLabels.add(columns[0]);
            }
            while((line = brPredict.readLine())!= null){
                line = line.trim();
                if(line.equals(testLabels.get(rowNumber))){
                    totalCorrectPrediction++;
                }
                rowNumber++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (totalCorrectPrediction/(rowNumber+1))*100;
    }

}
