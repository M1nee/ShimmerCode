package shimmerresearch.com.shimmerbasicexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;



import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;
import java.text.DecimalFormat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;


import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends Activity {

    private final static String LOG_TAG = "ShimmerBasicExample";
    Shimmer shimmer;
    TextView ACCEL_LN_X,ACCEL_LN_Y,ACCEL_LN_Z,GYRO_X,GYRO_Y,GYRO_Z,GYRO_X_MESS;
    TextView AXAvg,AYAvg,AZAvg,GXAvg,GYAvg,GZAvg;
    DecimalFormat df = new DecimalFormat("#.##");
    TextView AXMax, AYMax, AZMax, GXMax, GYMax, GZMax;
    TextView AXMin, AYMin, AZMin, GXMin, GYMin, GZMin;

    public final int SAMPLEPERSEC=100;
    public final int MINBUFFER=100;
    public final int MAXBUFFER=100;
    public final int AVGBUFFER=50;
    public final int ARRAYLENGTH=50;

    public double[] AccelX= new double[ARRAYLENGTH];
    public double[] AccelY= new double[ARRAYLENGTH];
    public double[] AccelZ= new double[ARRAYLENGTH];
    public double[] GyroX = new double[ARRAYLENGTH];
    public double[] GyroY = new double[ARRAYLENGTH];
    public double[] GyroZ = new double[ARRAYLENGTH];
    public double accelXData,accelYData,accelZData,gyroXData,gyroYData,gyroZData;
    public int index=0;
                 //   public double TotalAX,TotalAY,TotalAZ,TotalGX,TotalGY,TotalGZ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        shimmer = new Shimmer(mHandler);
//        BarChart barChart = (BarChart) findViewById(R.id.barchart);
//
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        entries.add(new BarEntry(8, 0));
//        entries.add(new BarEntry(2f, 1));
//        entries.add(new BarEntry(5f, 2));
//        entries.add(new BarEntry(20f, 3));
//        entries.add(new BarEntry(15f, 4));
//        entries.add(new BarEntry(19f, 5));
//
//        BarDataSet bardataset = new BarDataSet(entries, "Cells");
//
//        ArrayList<String> labels = new ArrayList<String>();
//        labels.add("AX");
//        labels.add("AY");
//        labels.add("AZ");
//        labels.add("GX");
//        labels.add("GY");
//        labels.add("GZ");
//
//        BarData data = new BarData(labels, bardataset);
//        barChart.setData(data); // set the data and list of labels into chart
//        barChart.setDescription("Set Bar Chart Description Here");  // set the description
//        bardataset.setColors(ColorTemplate.COLORFUL_COLORS);
//        barChart.animateY(5000);

    }

    public void connectDevice(View v) {
        Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
        startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
    }

    public void startStreaming(View v) throws InterruptedException, IOException{
        shimmer.setSamplingRateShimmer(SAMPLEPERSEC);
        shimmer.startStreaming();
    }

    public void stopStreaming(View v) throws IOException{
        shimmer.stopStreaming();
    }

    public void setArrays ()
    {
            AccelX[index]= accelXData;
            AccelY[index]= accelYData;
            AccelZ[index]= accelZData;
            GyroX[index]=  gyroXData;
            GyroY[index]=  gyroYData;
            GyroZ[index]=  gyroZData;

        if (index<ARRAYLENGTH-1)
            index++;
        else
            index=0;
    }

    public double ArrayAvg(double[] myArray)
    {
        double avg;
        double total=0;
        int i=index;
        for(int count=0; count<AVGBUFFER;count++)
        {
            total+=myArray[i];
            i--;
            if (i>0)
                i=ARRAYLENGTH;
        }
        avg=total/AVGBUFFER;
        return avg;

    }
    public double ArrayMin(double[] myArray)
    {
        double min=myArray[0];
        int i=index;
        for(int count=0; count<MINBUFFER;count++)
        {
            if(min>myArray[i])
                min=myArray[i];
            i--;
            if (i>0)
                i=ARRAYLENGTH;
        }

        return min;
    }

    public double ArrayMax(double[] myArray)
    {
        double max=myArray[0];
        int i=index;
        for(int count=0; count<MAXBUFFER;count++)
        {
            if(max<myArray[i])
                max=myArray[i];
            i--;
            if (i>0)
                i=ARRAYLENGTH;
        }

        return max;
    }


    /**
     * Messages from the Shimmer device including sensor data are received here
     */
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {

                        //Print data to Logcat
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        //Retrieve all possible formats for the current sensor device:
                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP);
                        FormatCluster timeStampCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        double timeStampData = timeStampCluster.mData;
                        Log.i(LOG_TAG, "Time Stamp: " + timeStampData);
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_X);
                        FormatCluster accelXCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (accelXCluster!=null) {
                            accelXData = accelXCluster.mData;
                            Log.i(LOG_TAG, "Accel LN X: " + accelXData);
//                            ACCEL_LN_X = (TextView) findViewById(R.id.accelX);
                            ACCEL_LN_X.setText("Accel X: " + Double.toString(Double.parseDouble(df.format(accelXData))));
                            AXAvg = (TextView) findViewById(R.id.AXAverage);
                            AXAvg.setText("AX Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(AccelX)))));
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y);
                        FormatCluster accelYCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (accelYCluster!=null) {
                            accelYData = accelYCluster.mData;
                            Log.i(LOG_TAG, "Accel LN Y: " + accelYData);
//                            ACCEL_LN_Y = (TextView) findViewById(R.id.accelY);
                            ACCEL_LN_Y.setText("Accel Y: " + Double.toString(Double.parseDouble(df.format(accelYData))));
                            AYAvg = (TextView) findViewById(R.id.AYAverage);
                            AYAvg.setText("AY Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(AccelY)))));
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z);
                        FormatCluster accelZCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (accelZCluster!=null) {
                            accelZData = accelZCluster.mData;
                            Log.i(LOG_TAG, "Accel LN Z: " + accelZData);
//                            ACCEL_LN_Z = (TextView) findViewById(R.id.accelZ);
                            ACCEL_LN_Z.setText("Accel Z: " + Double.toString(Double.parseDouble(df.format(accelZData))));
                            AZAvg = (TextView) findViewById(R.id.AZAverage);
                            AZAvg.setText("AZ Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(AccelZ)))));
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_X);
                        FormatCluster gyroXCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (gyroXCluster!=null) {
                            gyroXData = gyroXCluster.mData;
                            Log.i(LOG_TAG, "Gyro X: " + gyroXData);
                            GYRO_X = (TextView) findViewById(R.id.gyroX);
                            GYRO_X.setText("Gyro X: " + Double.toString(Double.parseDouble(df.format(gyroXData))));
                            GYRO_X_MESS = (TextView) findViewById((R.id.gyroXMessage));
                            GXAvg = (TextView) findViewById(R.id.GXAverage);
                            GXAvg.setText("GX Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(GyroX)))));
                            GXMax = (TextView) findViewById(R.id.GXMaximum);
                            GXMax.setText("GX Max: " + Double.toString(Double.parseDouble(df.format(ArrayMax(GyroX)))));
                            GXMin = (TextView) findViewById(R.id.GXMinimum);
                            GXMin.setText("GX Min: " + Double.toString(Double.parseDouble(df.format(ArrayMin(GyroX)))));
                            if(Math.abs(gyroXData) > 180) {
                                GYRO_X_MESS.setText("Move your head slower (gyro X)");
                            } else {
                                GYRO_X_MESS.setText("Keep it up, you're doing great :) (gyro X)");
                            }
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Y);
                        FormatCluster gyroYCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (gyroYCluster!=null) {
                            gyroYData = gyroYCluster.mData;
                            Log.i(LOG_TAG, "Gyro Y: " + gyroYData);
//                            GYRO_Y = (TextView) findViewById(R.id.gyroY);
                            GYRO_Y.setText("Gyro Y: " + Double.toString(Double.parseDouble(df.format(gyroYData))));
                            GYAvg = (TextView) findViewById(R.id.GYAverage);
                            GYAvg.setText("GY Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(GyroY)))));
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Z);
                        FormatCluster gyroZCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (gyroZCluster!=null) {
                            gyroZData = gyroZCluster.mData;
                            Log.i(LOG_TAG, "Gyro Z: " + gyroZData);
//                            GYRO_Z = (TextView) findViewById(R.id.gyroZ);
                            GYRO_Z.setText("Gyro Z: " + Double.toString(Double.parseDouble(df.format(gyroZData))));
                            GZAvg = (TextView) findViewById(R.id.GZAverage);
                            GZAvg.setText("GZ Avg: " + Double.toString(Double.parseDouble(df.format(ArrayAvg(GyroZ)))));
                        }
                        setArrays();
                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    /** Toast messages sent from {@link Shimmer} are received here. E.g. device xxxx now streaming.
                     *  Note that display of these Toast messages is done automatically in the Handler in {@link com.shimmerresearch.android.shimmerService.ShimmerService} */
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Shimmer.TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE:
                    ShimmerBluetooth.BT_STATE state = null;
                    String macAddress = "";

                    if (msg.obj instanceof ObjectCluster) {
                        state = ((ObjectCluster) msg.obj).mState;
                        macAddress = ((ObjectCluster) msg.obj).getMacAddress();
                    } else if (msg.obj instanceof CallbackObject) {
                        state = ((CallbackObject) msg.obj).mState;
                        macAddress = ((CallbackObject) msg.obj).mBluetoothAddress;
                    }

                    switch (state) {
                        case CONNECTED:
                            break;
                        case CONNECTING:
                            break;
                        case STREAMING:
                            break;
                        case STREAMING_AND_SDLOGGING:
                            break;
                        case SDLOGGING:
                            break;
                        case DISCONNECTED:
                            break;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /**
     * Get the result from the paired devices dialog
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                shimmer = new Shimmer(mHandler);
                shimmer.connect(macAdd, "default");                  //Connect to the selected device
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
