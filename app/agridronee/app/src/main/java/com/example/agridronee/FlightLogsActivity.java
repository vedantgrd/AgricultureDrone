package com.example.agridronee;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;

public class FlightLogsActivity extends AppCompatActivity {

    private TextInputEditText etDirection, etSpeed, etAltitude, etFlightDuration;
    private SwitchMaterial cbSprayActivity;
    private MaterialButton btnSaveLog;
    private ListView listViewFlightLogs;
    private ImageButton userSetting;
    private ArrayList<String> flightLogs;
    private ArrayAdapter<String> logsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flightlogs);

        // Initialize views
        etDirection = findViewById(R.id.etDirection);
        etSpeed = findViewById(R.id.etSpeed);
        etAltitude = findViewById(R.id.etAltitude);
        etFlightDuration = findViewById(R.id.etFlightDuration);
        cbSprayActivity = findViewById(R.id.cbSprayActivity);
        btnSaveLog = findViewById(R.id.btnSaveLog);
        listViewFlightLogs = findViewById(R.id.listViewFlightLogs);
        userSetting = findViewById(R.id.usersetting);

        // Setup flight logs list
        flightLogs = new ArrayList<>();
        logsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                flightLogs);
        listViewFlightLogs.setAdapter(logsAdapter);

        // Button listeners
        btnSaveLog.setOnClickListener(v -> saveLog());
        userSetting.setOnClickListener(v ->
                Toast.makeText(this, "User Profile", Toast.LENGTH_SHORT).show());
    }

    private void saveLog() {
        String direction = etDirection.getText().toString();
        String speed = etSpeed.getText().toString();
        String altitude = etAltitude.getText().toString();
        String duration = etFlightDuration.getText().toString();
        boolean spray = cbSprayActivity.isChecked();

        if (direction.isEmpty() || speed.isEmpty() ||
                altitude.isEmpty() || duration.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String log = direction + ", " + speed + " m/s, " +
                altitude + " m, " + duration + " mins, Spray: " +
                (spray ? "On" : "Off");

        flightLogs.add(log);
        logsAdapter.notifyDataSetChanged();

        // Clear inputs
        etDirection.setText("");
        etSpeed.setText("");
        etAltitude.setText("");
        etFlightDuration.setText("");
        cbSprayActivity.setChecked(false);

        Toast.makeText(this, "Log Saved", Toast.LENGTH_SHORT).show();
    }
}