package com.duster.fr.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Anas on 15/09/2015.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothService";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private Boolean send;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    private ArrayAdapter<String> mArrayAdapter;

    /* Getter for ArrayAdapter*/
    public ArrayAdapter<String> getmArrayAdapter(){
        return mArrayAdapter;
    }

    private MainActivity activity;
    int testInt;

    public BluetoothService(Handler handler, MainActivity activity) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        this.activity = activity;
    }


    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

    }

    /* Starting the thread to enable connection to device*/
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "Connect");
        stop();
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /*Starting the tread to manage the connection and enable transmissions*/

    public synchronized void Connected(BluetoothSocket socket) {
        Log.d(TAG, "connected");
        stop();

        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
    }

    public boolean sendOrStop(){
        if(mConnectedThread!=null){
            send = !send;
        }
        return send;
    }

    public void change(){
        if(mConnectedThread!=null){mConnectedThread.change();}
    }


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };


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
            mAdapter.cancelDiscovery();

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
            BluetoothService.this.Connected(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private class ConnectedThread extends Thread {

        private static final String TAG = "ConnectedThread";

        private volatile boolean running = true;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private DataBuilder dataBuilder = new DataBuilder();

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            mHandler = handler;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Could not get streams from socket");
                BluetoothService.this.stop();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            int bytes =0;
            byte[] buffer = new byte[500];
            send = false;
            testInt = 0;
            while(running){
                try{
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    bytes = mmInStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(bytes >0){
                    try {
                        mmInStream.read(buffer,0,bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(send){
                    write(dataBuilder.getData());
                    if(testInt>100000){
                        testInt=0;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           // activity.printValue(testInt++);
                        }
                    });
                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           // activity.printValue(100000);
                        }
                    });
                }

            }

        }

        public void write(byte[] bytes){

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("writing", "Impossible to write to the device", e);
                cancel();
            }

        }

        public void change(){
            dataBuilder.changeType();
        }

        public void cancel() {
            Log.i(TAG, "cancel Thread");
            running = false;

            try {Thread.sleep(100);
            } catch (InterruptedException e){
                Log.e(TAG, "interrupter");
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close socket", e);
            }
        }
    }





    protected void checkBluetoothEnabled(Activity activity){
        if(!mAdapter.isEnabled()){
            Intent enableBtIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent,1);
        }
    }

    protected void makeDiscoverable(Activity activity){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        activity.startActivity(discoverableIntent);
    }

}
