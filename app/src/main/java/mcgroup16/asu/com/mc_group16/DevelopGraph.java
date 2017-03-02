package mcgroup16.asu.com.mc_group16;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import mcgroup16.asu.com.mc_group16.service.AccelerometerService;


public class DevelopGraph extends AppCompatActivity implements SensorEventListener {

    private GraphView runningGraphView;
    private GraphView defaultGraphView;
    private boolean hasRunPressed = false;
    private float defaultValues[];
    private float runningValues[];
    private Button btnRun = null;
    private Button btnStop = null;
    private Handler handle = null;
    private LinearLayout devGraph = null;

    private SensorManager sensor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_develop_graph);

        String patientName = getIntent().getStringExtra("EXTRA_PATIENT_NAME");
        String patientAge = getIntent().getStringExtra("EXTRA_PATIENT_AGE");

        TextView txtPatientName = (TextView) findViewById(R.id.txtPatientName);
        TextView txtPatientAge = (TextView) findViewById(R.id.txtPatientAge);
        txtPatientName.setText(patientName);
        txtPatientAge.setText(patientAge);

        final String[] X_Labels = new String[]{"0", "50", "100", "150", "200", "250"};
        final String[] Y_Labels = new String[]{"50", "100", "150", "200", "250"};
        runningValues = new float[20];
        defaultValues = new float[20];

        devGraph = (LinearLayout) findViewById(R.id.develop_graph);
        defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
        defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        devGraph.addView(defaultGraphView);


        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devGraph.removeView(runningGraphView);
                devGraph.removeView(defaultGraphView);
                defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
                devGraph.addView(defaultGraphView);
            }
        });

        btnRun = (Button) findViewById(R.id.btn_run);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runningGraphView != null) {
                    devGraph.removeView(runningGraphView);
                }
                runningGraphView = new GraphView(getApplicationContext(), runningValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                runningGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));

                if (devGraph != null && defaultGraphView != null) {
                    devGraph.removeView(defaultGraphView);
                }
                devGraph.addView(runningGraphView);

                if (!hasRunPressed) {
                    handle = new Handler();
                    hasRunPressed = true;
                    handle.post(postUpdate);
                }
            }
        });

    }

    private void getAccelerometer(SensorEvent event) {

        float[] values = event.values;

        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // This method is generating random values for plotting
    // It keeps on updating the values displayed on the graph
    private Runnable postUpdate = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < runningValues.length; i++) {
                runningValues[i] = (float) 0 + (int) (Math.random() * 5);
            }
            runningGraphView.setValues(runningValues);
            devGraph.removeView(runningGraphView);
            devGraph.addView(runningGraphView);
            handle.postDelayed(this, 500);
        }
    };
}
