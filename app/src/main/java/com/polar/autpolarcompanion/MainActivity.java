package com.polar.autpolarcompanion;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    //can be used to log data to console and find it easier
    private static final String TAG = "Polar_MainActivity";

    private static final String SHARED_PREFS_KEY = "polar_device_id";
    private static final String SHARED_PREFS_KEY_NAME = "user_name";
    private static final String SHARED_PREFS_KEY_AGE = "user_age";

    //deviceId = "5B331524";
    private String deviceId;
    private String userName;
    private String userAge;

    private TextView nameView;
    private TextView ageView;

    //Saves the device ID, user name, and user age to the device
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //sets the layout of the main activity
        setContentView(R.layout.activity_main);

        //Assigning the shared preferences variable
        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

        //Getting access to the views in the main activity
        nameView = findViewById(R.id.userName);
        ageView = findViewById(R.id.userAge);


        //Getting the data saved once the activity is created
        userName = sharedPreferences.getString(SHARED_PREFS_KEY_NAME, "");
        userAge = sharedPreferences.getString(SHARED_PREFS_KEY_AGE, "");

        //Checks if the userName and age is not null to assign the text to the views
        if(!userName.equals("") && !userAge.equals(""))
        {
            nameView.setText(userName);
            ageView.setText(userAge);
        }

        checkBT();

    }

    public void onClickConnect(View view) {
        checkBT();
        deviceId = sharedPreferences.getString(SHARED_PREFS_KEY, "");

        if (deviceId.equals("")) {
            showDialog(view);
        } else {
            Toast.makeText(this, getString(R.string.connecting) + " " + deviceId, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, UserDataActivity.class);
            intent.putExtra("id", deviceId);
            startActivity(intent);
        }
    }

    public void onClickUserData(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.PolarTheme);
        dialog.setTitle("Please enter your details");

        View viewInflated = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_layout, (ViewGroup) view.getRootView(), false);

        final EditText input = viewInflated.findViewById(R.id.name);
        input.setText("Name: ");
        final EditText inputTwo = viewInflated.findViewById(R.id.age);
        inputTwo.setText("Age: ");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTwo.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialog.setView(viewInflated);

        dialog.setPositiveButton("OK", (dialog1, which) -> {
            userName = input.getText().toString();
            userAge = inputTwo.getText().toString();
            nameView.setText(input.getText());
            ageView.setText(inputTwo.getText());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SHARED_PREFS_KEY_NAME, userName);
            editor.putString(SHARED_PREFS_KEY_AGE, userAge);
            editor.apply();
        });
        dialog.setNegativeButton("Cancel", (dialog12, which) -> dialog12.cancel());
        dialog.show();
    }

    public void onClickChangeID(View view) {
        showDialog(view);
    }

    public void showDialog(View view) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.PolarTheme);
        dialog.setTitle("Enter your Polar device's ID");

        View viewInflated = LayoutInflater.from(getApplicationContext()).inflate(R.layout.device_id_dialog_layout, (ViewGroup) view.getRootView(), false);

        final EditText input = viewInflated.findViewById(R.id.input2);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        dialog.setView(viewInflated);

        dialog.setPositiveButton("OK", (dialog1, which) -> {
            deviceId = input.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SHARED_PREFS_KEY, deviceId);
            editor.apply();
        });
        dialog.setNegativeButton("Cancel", (dialog12, which) -> dialog12.cancel());
        dialog.show();
    }

    public void checkBT() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        //requestPermissions() method needs to be called when the build SDK version is 23 or above
        if (Build.VERSION.SDK_INT >= 23) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}
