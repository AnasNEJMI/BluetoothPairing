package com.duster.fr.client;

import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends ActionBarActivity {


    // for Debuging purposes
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;

    // Message types sent from BlutoothService handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    //Key names received from BlutoothService handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    //Intent request condes

    private static final int REQUEST_CONNECT_DEVICE=1;
    private static final int REQUEST_ENABLE_BT=2;

    //LayoutViews

    private Button scanBtn;
    private ListView listView;
    private TextView textView;

    //Name of the connected Device
    private String mConnectedDeviceName = null;

    //Array Adapter for the conversation thread
    private ArrayList<String> mConversationThread;

    //String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer();

    //Local Bluetooth adapter
    private BluetoothAdapter mBtAdapter = null;

    //Member object of the bluetooth service
    private BluetoothService mService = null;

    // Arrays for the list of detected devices
    ArrayAdapter<String> listAdapter;
    ArrayList<BluetoothDevice> devices;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    IntentFilter filter;
    BroadcastReceiver receiver;


    /*Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case SUCCESS_CONNECT:
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(),"You have successfully connected",Toast.LENGTH_SHORT).show();

                    String s ="Successfully connected";
                    connectedThread.write(s.getBytes());
                    break;
                case MESSAGE_READ :
                    byte[] readBuf = (byte[]) msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(),string,Toast.LENGTH_SHORT).show();
                    break;
            }



        }

    };*/




    /*EditText sensors_number;
    private String[] mString;
    Button btn;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.e(TAG,"+++ OnCreate +++");

        //Setting up the layout
        setContentView(R.layout.device_list);

        //Getting the local Bluetooth Adapter and the layouts for managing scan
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        scanBtn = (Button) findViewById(R.id.scanBtn);
        textView = (TextView) findViewById(R.id.textView);



        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(DEBUG) Log.d(TAG, "+++ OnClick scanBtn +++");

                init();

                if (mBtAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth could not be detected", Toast.LENGTH_SHORT).show();
                    finish();
                    
                //if BT is not on, request that it should be enabled. Then setupConnection()
                } else {
                    if (!mBtAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
                    }else {
                        if(mService == null) {
                            mService= new BluetoothService(getApplicationContext(),mHandler);
                        }
                    }

                    getPairedDevices();// getting the name and address of the paired devices
                    startDiscovery(); // starting the discovery
                }



            }
       });


   }

    /* Starting discovery */

    private void startDiscovery() {
        mBtAdapter.cancelDiscovery();
        mBtAdapter.startDiscovery();
    }


    /* Getting the list of the paired devices */

    private void getPairedDevices() {
        Log.d(TAG,"getting the paired devices");

        devicesArray = mBtAdapter.getBondedDevices();
        if(devicesArray.size()>0){
            for(BluetoothDevice device : devicesArray){
                pairedDevices.add(device.getName()+"\n"+device.getAddress()); // name + address

            }
        }

    }

    private void init() {

        listView = (ListView) findViewById(R.id.listDevice);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                if (mBtAdapter.isDiscovering()) {
                    mBtAdapter.cancelDiscovery();
                }
                BluetoothDevice selectedDevice = devices.get(arg2);
                String name = selectedDevice.getName();
                mService.connect(selectedDevice);
                //mService.accept();


                //If the pairing is established, move to the other activity where we can send and receive data


            }
        });




        listAdapter = new ArrayAdapter<String>(this,R.layout.device_name,0);
        listView.setAdapter(listAdapter);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<String>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices =new ArrayList<BluetoothDevice>();


        /* Setting up the broadcast receiver */
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    if(device.getName()==null){

                        listAdapter.add("Unidentified Device"+"\n"+device.getAddress()); //if it is the paired device, add (Paired) to it's name + adress

                    }else {
                        listAdapter.add(device.getName()+"\n"+device.getAddress()); //if it is the paired device, add (Paired) to it's name + adress
                    }



                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                     /*-----------------*/
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(mBtAdapter.getState()==mBtAdapter.STATE_OFF){
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent,REQUEST_ENABLE_BT); // if the state changes to 'off' turn on bluetooth

                    }
                }
            }
        };

        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver,filter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(receiver!=null)
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_CANCELED){
            Toast.makeText(getApplicationContext(),"Bluetooth must be enabled", Toast.LENGTH_SHORT);
            finish();
        }
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //sensorNumber.setText(mOutStringBuffer);
        }
    }


    /* ConnectThread */




    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            /***********/
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            /***********/
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            /**********/
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    textView.setText(readMessage);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





}
