package com.duster.fr.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

/**
 * Created by Anas on 18/09/2015.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothChatService";
    private static final boolean DEBUG = true;

    private static final String NAME = "BluetoothCommunication";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private  final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    //To indicate the state of the current connection
    public static final int STATE_NONE = 0;       // nothing is done
    public static final int STATE_LISTEN = 1;     // currently listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /* Constructor to prepare a new Bluetooth communication session*/

    public BluetoothService(Context context, Handler handler){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState=STATE_NONE;
        mHandler=handler;
    }

    /*Getter and Setter of mState*/
    private synchronized void setState(int state){
        if(DEBUG) Log.d(TAG, "setState" + mState +"-->"+state);
        mState=state;
    }

    public synchronized int getState(){
        return mState;
    }

    public synchronized void start() {
        if (DEBUG) Log.d(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /*start the ConnectThread to initiate a connection with a remote device*/

    public synchronized void connect(BluetoothDevice device){
        if (DEBUG) Log.d(TAG,"connect to : "+device);

        //canceling any thread attempting to make a connection
        if (mState==STATE_CONNECTING){
            if(mConnectThread!=null){mConnectThread.cancel();mConnectThread=null;}
        }
        // Canceling any currently running connection
        if (mConnectedThread!=null){mConnectedThread.cancel();mConnectedThread=null;}

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    //Start the ConnectedThread to begin managing a bluetooth connection

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        if(DEBUG) Log.d(TAG,"connected");

        //Canceling the thread that completed the connection
        if(mConnectThread != null){mConnectThread.cancel();mConnectThread=null;}

        if(mAcceptThread !=null){mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);

    }

    public synchronized void stop(){
        if(DEBUG) Log.d(TAG,"stop");

        if (mConnectThread!=null){mConnectThread.cancel();mConnectThread=null;}
        if (mConnectedThread!=null){mConnectedThread.cancel();mConnectedThread=null;}
        if (mAcceptThread!=null){mAcceptThread.cancel();mAcceptThread=null;}

        setState(STATE_NONE);

    }
    public synchronized void accept() {
        Log.d(TAG, "accept");
        stop();
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    // To write to the ConnectedThread in an unsynchronized manner

    public void write(byte[] outMsg){
        ConnectedThread r;

        synchronized (this){
            if (mState!=STATE_CONNECTED) return;
            r=mConnectedThread;
        }

        //unsynchronized wrinting

        r.write(outMsg);


    }

    // indicating and notifying the UI activity that the connection attempt has failed

    private void connectionFailed(){
        setState(STATE_LISTEN);

        //send failure message back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "The connection to the device failed");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    // indicating and notifying the UI activity that the connection was lost

    private void connectionLost(){
        setState(STATE_LISTEN);

        // send failure message back to the UI Activity

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"The connection with the device was last");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    // This thread runs while listening for incoming connection. Runs until a connection is accepted

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }
        public void run() {
            if (DEBUG) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (DEBUG) Log.i(TAG, "END mAcceptThread");
        }
        public void cancel() {
            if (DEBUG) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothService.this.start();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    protected void makeDiscoverable(Activity activity){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,60000);
        activity.startActivity(discoverableIntent);
    }




}

