package mcgroup16.asu.com.mc_group16;

import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.sql.Timestamp;

import mcgroup16.asu.com.mc_group16.service.AccelerometerService;

public class MainActivity extends AppCompatActivity {

    private static final String DB_NAME = "MC_Group16";
    private String patientName = null;
    private String patientId = null;
    private String patientAge = null;
    private String patientSex = null;
    private Timestamp timestamp = null;

    private String TABLE_NAME = null;
    private String CREATE_TABLE_SQL = null;
    private String INSERT_INTO_TABLE_SQL = null;

    private RadioGroup radioGroupSex = null;
    private RadioButton radioButtonSex = null;
    private EditText patientTextBox = null;
    private EditText patientIdTextBox = null;
    private EditText patientAgeTextBox = null;
    private Button nextButton = null;

    SQLiteDatabase patientDb = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            patientDb = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
            Toast.makeText(this, "Database created successfully ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Problem in creating database", Toast.LENGTH_SHORT).show();
        }

        nextButton = (Button) findViewById(R.id.btn_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                timestamp = new Timestamp(System.currentTimeMillis());

                patientIdTextBox = (EditText) findViewById(R.id.patientIdText);
                patientId = patientIdTextBox.getText().toString();

                patientTextBox = (EditText) findViewById(R.id.patientNameText);
                patientName = patientTextBox.getText().toString();

                patientAgeTextBox = (EditText) findViewById(R.id.patientAgeText);
                patientAge = patientAgeTextBox.getText().toString();

                radioGroupSex = (RadioGroup) findViewById(R.id.radio_sex);
                int selectedId = radioGroupSex.getCheckedRadioButtonId();
                radioButtonSex = (RadioButton) findViewById(selectedId);
                patientSex = radioButtonSex.getText().toString();

                TABLE_NAME = patientName + "_" + patientId + "_" + patientAge + "_" + patientSex;
                CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
                        + TABLE_NAME
                        + " ( "
                        + " timestamp TEXT, "
                        + " x_val REAL, "
                        + " y_val REAL, "
                        + " z_val REAL"
                        + " );";

                try {
                    patientDb.execSQL(CREATE_TABLE_SQL);
                    Toast.makeText(getApplicationContext(), "Table created successfully", Toast.LENGTH_SHORT).show();
                } catch (SQLException e) {
                    Toast.makeText(getApplicationContext(), "Problem in creating table", Toast.LENGTH_SHORT).show();
                }

                if (patientId.isEmpty() || patientName.isEmpty() || patientAge.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter patient's information to proceed", Toast.LENGTH_SHORT).show();
                } else {
                    // starting graph activity
                    Intent moveToGraphActivity = new Intent(getApplicationContext(), DevelopGraph.class);
                    moveToGraphActivity.putExtra("EXTRA_PATIENT_NAME", patientName);
                    moveToGraphActivity.putExtra("EXTRA_PATIENT_AGE", patientAge);
                    startActivity(moveToGraphActivity);
                }
            }
        });
    }
}
