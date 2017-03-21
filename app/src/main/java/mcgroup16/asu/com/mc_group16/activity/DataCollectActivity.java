package mcgroup16.asu.com.mc_group16.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mcgroup16.asu.com.mc_group16.R;
import mcgroup16.asu.com.mc_group16.model.Row;
import mcgroup16.asu.com.mc_group16.utility.DatabaseUtil;

public class DataCollectActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private double[] sensorData = null;
    private ArrayList<Double> trainingArray = new ArrayList<Double>();
    private Handler insertHandle = null;
    private RadioGroup radioGroupCollectData = null;
    private RadioButton radioButtonCollect = null;
    private String activityLabel = null;
    private static final String RUN_OPTION = "Run";
    private static final String WALK_OPTION = "Walk";
    private static final String EAT_OPTION = "Eat";
    private Button btnCollect = null;
    private Button btnTest = null;

    // Database utility related declarations
    private String DB_NAME = null;
    private String TABLE_NAME = "training_table";
    private DatabaseUtil dbHelper = null;

    static{
        System.loadLibrary("jnilibsvm");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collect);
        initiateAccelerometer();

        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");

        // DB handler instance initialization
        dbHelper = new DatabaseUtil(this, DB_NAME);
        dbHelper.createTable(TABLE_NAME);

        btnCollect = (Button) findViewById(R.id.btn_collect);
        btnCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                radioGroupCollectData = (RadioGroup) findViewById(R.id.radio_data_collect);
                int selectedId = radioGroupCollectData.getCheckedRadioButtonId();
                radioButtonCollect = (RadioButton) findViewById(selectedId);
                String collectOption = radioButtonCollect.getText().toString();

                if (collectOption.equals(RUN_OPTION)) {
                    activityLabel = RUN_OPTION;
                } else if (collectOption.equals(WALK_OPTION)) {
                    activityLabel = WALK_OPTION;
                } else {
                    activityLabel = EAT_OPTION;
                }

                insertHandle = new Handler();
                insertHandle.post(insertIntoTrainingArray);
            }
        });


        btnTest = (Button) findViewById(R.id.btn_test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Row> rows = dbHelper.getRows(TABLE_NAME);
                List<Double> numColumns = rows.get(0).getData();
                String label = rows.get(0).getLabelActivity();
                Toast.makeText(DataCollectActivity.this, "Number of rows: " + rows.size() + ", activity label: " + label, Toast.LENGTH_LONG).show();

            }
        });

    }

    private Runnable insertIntoTrainingArray = new Runnable() {
        @Override
        public void run() {
            if (trainingArray.size() < 150) {
                trainingArray.add(sensorData[1]);
                trainingArray.add(sensorData[2]);
                trainingArray.add(sensorData[3]);
                insertHandle.postDelayed(this, 100);

            } else if (trainingArray.size() == 150) {
                insertHandle.removeCallbacksAndMessages(null);
                Row row = new Row(trainingArray, activityLabel);
                dbHelper.addRow(row, TABLE_NAME);
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

}
