package com.polar.autpolarcompanion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.reactivestreams.Publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarSensorSetting;

public class UserDataActivity extends AppCompatActivity implements PlotterListener {

    //can be used to log data to console and find it easier
    private static final String TAG = "UserDataActivity";

    private String deviceId;

    //Polar H10 API
    private PolarBleApi api;

    private TextView hrRrView;
    private TextView timerText;
    private EditText rpm;
    private EditText watt;
    private Button startTimer;
    private XYPlot plot;
    private Plotter plotter;

    private ArrayList<Double> timedData;
    private ArrayList<Double> fullData;
    private ArrayList<Double> rrData;
    private ArrayList<Double> standardDeviation;

    private List<Integer> rrsMs;

    private long duration;

    private int timerCounter;
    private int rowCounter;
    private int columnCounter;
    private int timedDataSize;

    private boolean start;

    private XSSFWorkbook workbook;
    private Sheet timedDataSheet;
    private Sheet fullDataSheet;
    private Row row;

    private MediaPlayer ring;

    private Disposable ecgDisposable = null;
    private final Context classContext = this;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        deviceId = getIntent().getStringExtra("id");

        hrRrView = findViewById(R.id.hrRr);
        startTimer = findViewById(R.id.startTimer);
        timerText = findViewById(R.id.timerTwo);
        rpm = findViewById(R.id.rpmEnter);
        watt = findViewById(R.id.wattEnter);
        plot = findViewById(R.id.plot);

        duration = TimeUnit.MINUTES.toMillis(3);

        timerCounter = 179;
        rowCounter = 0;
        columnCounter = 0;
        timedDataSize = 0;

        start = false;

        timedData = new ArrayList<>();
        fullData = new ArrayList<>();
        rrData = new ArrayList<>();
        standardDeviation = new ArrayList<>();

        workbook = new XSSFWorkbook();
        timedDataSheet = workbook.createSheet("Timed Data");
        fullDataSheet = workbook.createSheet("Full Data");

        ring= MediaPlayer.create(this,R.raw._timer);

        //creating the needed number of rows in the excel sheet
        for(int i = 0; i<=200; i++)
        {
            row = timedDataSheet.createRow(i);
        }



        api = PolarBleApiDefaultImpl.defaultImplementation(this,
                PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
                        |
                        PolarBleApi.FEATURE_BATTERY_INFO
                        |
                        PolarBleApi.FEATURE_HR
        );

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean b) {
                Log.d(TAG, "BluetoothStateChanged " + b);
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo s) {
                Log.d(TAG, "Device connected " + s.deviceId);
                Toast.makeText(classContext, R.string.connected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {

            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo s) {
                Log.d(TAG, "Device disconnected " + s);
            }

            @Override
            public void streamingFeaturesReady(@NonNull final String identifier,
                                               @NonNull final Set<PolarBleApi.DeviceStreamingFeature> features) {

                for (PolarBleApi.DeviceStreamingFeature feature : features) {
                    Log.d(TAG, "Streaming feature is ready: " + feature);
                    if (feature == PolarBleApi.DeviceStreamingFeature.ECG) {
                        streamECG();
                    }
                }
            }

            @Override
            public void hrFeatureReady(@NonNull String s) {
                Log.d(TAG, "HR Feature ready " + s);
            }

            @Override
            public void disInformationReceived(@NonNull String s, @NonNull UUID u, @NonNull String s1) {
            }

            @Override
            public void batteryLevelReceived(@NonNull String s, int i) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void hrNotificationReceived(@NonNull String s,
                                               @NonNull PolarHrData polarHrData) {

                Log.d(TAG, "HR " + polarHrData.hr);
                //get the rr interval data and add it to the rrms list
                rrsMs = polarHrData.rrsMs;
                //create a new msg with the hr and rr data
                String msg = "Heart Rate = " + polarHrData.hr + "\n" + "RR = ";
                //for each loop that will get the rrsMs data.
                for (int i : rrsMs) {
                    msg += i + ".";
                    fullData.add((double) i);
                    if(start)
                    {
                        timedData.add((double) i);
                        timedDataSize = timedData.size()-1;
                    }
                }
                if (msg.endsWith(".")) {
                    msg = msg.substring(0, msg.length() - 1);
                }

                //change the text view
                hrRrView.setText(msg + "ms");

            }

            @Override
            public void polarFtpFeatureReady(@NonNull String s) {
            }
        });

        //try and catch method to connect to the device
        try {
            api.connectToDevice(deviceId);
        } catch (PolarInvalidArgument a) {
            a.printStackTrace();
        }

        plotter = new Plotter("ECG Data");
        plotter.setListener(this);

        plot.addSeries(plotter.getSeries(), plotter.getFormatter());
        plot.setRangeBoundaries(-3.3, 3.3, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.INCREMENT_BY_FIT, 0.55);
        plot.setDomainBoundaries(0, 500, BoundaryMode.GROW);
        plot.setLinesPerRangeLabel(2);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
    }


    //starts the streaming of ECG and plotting it
    public void streamECG() {
        if (ecgDisposable == null) {
            ecgDisposable =
                    api.requestStreamSettings(deviceId, PolarBleApi.DeviceStreamingFeature.ECG)
                            .toFlowable()
                            .flatMap((Function<PolarSensorSetting, Publisher<PolarEcgData>>) sensorSetting ->
                                    api.startEcgStreaming(deviceId, sensorSetting.maxSettings()))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    polarEcgData -> {
                                        Log.d(TAG, "ecg update");
                                        for (Integer data : polarEcgData.samples) {
                                            plotter.sendSingleSample(((float) data / 1000));
                                        }
                                    },
                                    throwable -> {
                                        Log.e(TAG,
                                                "" + throwable.getLocalizedMessage());
                                        ecgDisposable = null;
                                    },
                                    () -> Log.d(TAG, "complete")
                            );
        } else {
            //stops streaming if it is "running"
            ecgDisposable.dispose();
            ecgDisposable = null;
        }
    }

    //Controls the timer
    public void startTimer(View view)
    {
        //if the timer starts the start timer button will not be enabled
        startTimer.setEnabled(false);
        //a new count down timer
        new CountDownTimer(10000, 1000)
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long l) {

                if(timerCounter <= 176)
                {
                    start = true;
                }
                //set the text of the timer
                timerText.setText("Timer: " + timerCounter + "s");

                //adds data to the excel sheet
                if(!timedData.isEmpty() && timerCounter <= 176)
                {
                    rrData.add(timedData.get(timedDataSize));
                    row = timedDataSheet.getRow(rowCounter);
                    row.createCell(columnCounter).setCellValue(timedData.get(timedDataSize));
                    rowCounter++;
                }
                timerCounter--;
            }

            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "Timer ended", Toast.LENGTH_SHORT).show();
                ring.start();
                start = false;
                startTimer.setEnabled(true);

                //calculates the standard deviation in the rrData list
                double standardDev = calculateSD(rrData);

                //adds the SD calculated to the the standard deviation list
                standardDeviation.add(standardDev);

                Log.d(TAG, "Standard = " + standardDev);

                //adds the RPM and WATT values to the excel sheet
                row = timedDataSheet.getRow(121);
                row.createCell(columnCounter).setCellValue("RPM: " + rpm.getText());
                row = timedDataSheet.getRow(122);
                row.createCell(columnCounter).setCellValue("Watt: " + watt.getText());

                //resets the timer
                timerCounter = 179;
                //resets the row
                rowCounter = 0;

                columnCounter++;
            }
        }.start();
    }

    public void saveDataToExcel(View view)
    {
        //a for loop to save the full data to the excel sheet
        for(int i = 0; i<= fullData.size()-1; i++)
        {
            Row rowTwo = fullDataSheet.createRow(i);
            rowTwo.createCell(0).setCellValue(fullData.get(i));
        }
        //The file name
        String fileName = "DATA.xlsx";
        //the location of the file to be exported
        String extStorageDirectory = Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

        File folder = new File(extStorageDirectory, "PolarData");
        //creates a new folder if the folder does not exist
        if(!folder.exists())
        {
            folder.mkdir();
        }

        //creates a new file if the file does not exist in the folder
        File file = new File(folder, fileName);
        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        //tries to add the data to the file
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //a dialog that will be shown to change the exertion of the user
    public void showExertionDialog(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.PolarTheme);
        dialog.setTitle("Please Enter your perceived exertion");

        View viewInflated = LayoutInflater.from(getApplicationContext()).inflate(R.layout.device_id_dialog_layout, (ViewGroup) view.getRootView(), false);

        final EditText input = viewInflated.findViewById(R.id.input2);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialog.setView(viewInflated);

        dialog.setPositiveButton("OK", (dialog1, which) -> {


        });
        dialog.setNegativeButton("Cancel", (dialog12, which) -> dialog12.cancel());
        dialog.show();
    }

    //calculates the standard deviation of a list
    public static double calculateSD(ArrayList numArray)
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.size();

        for(Object num : numArray) {
            sum = (sum + (double) num);
        }

        double mean = sum/length;

        for(Object num: numArray) {
            standardDeviation += Math.pow((double) num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }


    //
    public void onClickShowGraph(View view) {

        UtilityClass.getInstance().setList(standardDeviation);
        Intent intent = new Intent(this, HRVActivity.class);
        startActivity(intent);
    }

    //updates the plot
    @Override
    public void update() {
        runOnUiThread(() -> plot.redraw());
    }
}
