package mcgroup16.asu.com.group16.activity;

import android.content.Intent;
import android.database.SQLException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import mcgroup16.asu.com.group16.R;
import mcgroup16.asu.com.group16.utility.DatabaseUtil;

public class MainActivity extends AppCompatActivity {

    private static final String DB_NAME = "McGroup16";
    private String patientName = null;
    private String patientId = null;
    private String patientAge = null;
    private String patientSex = null;
    private String TABLE_NAME = null;

    private RadioGroup radioGroupSex = null;
    private RadioButton radioButtonSex = null;
    private EditText patientNameTextBox = null;
    private EditText patientIdTextBox = null;
    private EditText patientAgeTextBox = null;
    private Button nextButton = null;
    private Button clearButton = null;

    DatabaseUtil dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().deleteDatabase(DB_NAME);

        dbHelper = new DatabaseUtil(this, DB_NAME);

        nextButton = (Button) findViewById(R.id.btn_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                patientIdTextBox = (EditText) findViewById(R.id.patientIdText);
                patientId = patientIdTextBox.getText().toString();

                patientNameTextBox = (EditText) findViewById(R.id.patientNameText);
                patientName = patientNameTextBox.getText().toString().trim();

                patientAgeTextBox = (EditText) findViewById(R.id.patientAgeText);
                patientAge = patientAgeTextBox.getText().toString();

                radioGroupSex = (RadioGroup) findViewById(R.id.radio_sex);
                int selectedId = radioGroupSex.getCheckedRadioButtonId();
                radioButtonSex = (RadioButton) findViewById(selectedId);
                patientSex = radioButtonSex.getText().toString();

                if (patientId.isEmpty() || patientName.isEmpty() || patientAge.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter patient's information to proceed", Toast.LENGTH_SHORT).show();
                } else {
                    // starting graph activity
                    Intent moveToDataCollectActivity = new Intent(getApplicationContext(), DataCollectActivity.class);
                    moveToDataCollectActivity.putExtra("EXTRA_DB_NAME", DB_NAME);
                    startActivity(moveToDataCollectActivity);
                }
            }
        });

        clearButton = (Button) findViewById(R.id.btn_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                patientIdTextBox.setText("");
                patientAgeTextBox.setText("");
                patientNameTextBox.setText("");
            }
        });
    }
}
