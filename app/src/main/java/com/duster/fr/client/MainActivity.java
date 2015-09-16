package com.duster.fr.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.Bundle;
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

public class MainActivity extends Activity  {

    private static final String TAG = "BluetoothService";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected static final int SUCCESS_CONNECT =0;

    protected static final int MESSAGE_READ =1;

    ArrayAdapter<String> listAdapter;
    Button connectNew;
    ListView listView;
    BluetoothAdapter btAdapter;
    ArrayList<BluetoothDevice> devices;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    IntentFilter filter;
    BroadcastReceiver receiver;


    Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case SUCCESS_CONNECT:
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(),"CONNECT",Toast.LENGTH_SHORT).show();

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

    };




    /*EditText sensors_number;
    private String[] mString;
    Button btn;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectNew = (Button) findViewById(R.id.scanBtn);

        /* the connectNew button enables scanning for new devices */

        connectNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
                if (btAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth could not be detected", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (!btAdapter.isEnabled()) {
                        turnOnBT();
                    }

                    getPairedDevices();// getting the name and address of the paired devices
                    startDiscovery(); // starting the discovery
                }

            }
       });

   }

    /* Starting discovery */

    private void startDiscovery() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }


    /* Turning on Bluetooth */

    private void turnOnBT() {
        Log.d(TAG,"turn on BT");
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,1);
    }


    /* Getting the list of the paired devices */

    private void getPairedDevices() {
        Log.d(TAG,"getting the paired devices");

        devicesArray = btAdapter.getBondedDevices();
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
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2 , long arg3) {

                    if(btAdapter.isDiscovering()){
                        btAdapter.cancelDiscovery();
                    }
                    if(listAdapter.getItem(arg2).contains("Paired")){
                        BluetoothDevice selectedDevice = devices.get(arg2);
                        ConnectThread connect = new ConnectThread(selectedDevice);
                        connect.start();
                    }else{
                        Toast.makeText(getApplicationContext(),"device is not paired",Toast.LENGTH_SHORT).show();
                    }

            }
        });
        listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,0);
        listView.setAdapter(listAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
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
                    listAdapter.add(device.getName()+"\n"+device.getAddress()); //if it is the paired device, add (Paired) to it's name + adress


                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                     /*-----------------*/
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(btAdapter.getState()==btAdapter.STATE_OFF){
                        turnOnBT(); // if the state changes to 'off' turn on bluetooth

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



    /* ConnectThread */

    private class ConnectThread extends Thread{
        private static final String TAG = "ConnectThread";

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { Log.e(TAG, e.toString());}
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
        }



        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;// buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    buffer = new byte[1024];
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


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
