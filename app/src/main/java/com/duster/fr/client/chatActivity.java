package com.duster.fr.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Anas on 17/09/2015.
 */
public class chatActivity extends Activity {

    EditText sensor_number;
    Button sendBtn;
    TextView textView;
    Handler bluetoothIn;
    TextView display_data;

    /*private int handlerState =0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address;


    protected static final int MESSAGE_READ =1;

    Handler mHandler = new Handler();*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        sensor_number = (EditText) findViewById(R.id.sensor_number);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        textView = (TextView) findViewById(R.id.request_data);
        display_data = (TextView) findViewById(R.id.display_data);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            textView.setText("Request Data From : " + extras.getString("device_name"));
        }
    }

       /* bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;            // determine the end-of-line
                    if (readMessage.length()> 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, readMessage.length());    // extract string
                        display_data.setText("Data Received = " + dataInPrint);
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.sensor_number);
                String message = view.getText().toString();
                mConnectedThread.write(message);    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });}
        private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

            return  device.createRfcommSocketToServiceRecord(MY_UUID);
            //creates secure outgoing connecetion with BT device using UUID
        };


        @Override
        public void onResume() {
            super.onResume();

            //Get MAC address from DeviceListActivity via intent
            Intent intent = getIntent();
            //Get the MAC address from the DeviceListActivty via EXTRA
            address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);

            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try
            {
                btSocket.connect();
            } catch (IOException e) {
                try
                {
                    btSocket.close();
                } catch (IOException e2)
                {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            mConnectedThread.write("x");
        }

        @Override
        public void onPause()
        {
            super.onPause();
            try
            {
                //Don't leave Bluetooth sockets open when leaving activity
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }

        //Checks that the Android device Bluetooth is available and prompts to be turned on if off
        private void checkBTState() {

            if(btAdapter==null) {
                Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
            } else {
                if (btAdapter.isEnabled()) {
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                }
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
                    } catch (IOException e) {
                    }

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
            //write method
            public void write(String input) {
                byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
                try {
                    mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                } catch (IOException e) {
                    //if you cannot write, close the application
                    Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                    finish();

                }
            }
        }


       /* private void sendMessage(String message) {
        MainActivity mainActivity = new MainActivity();
        // Check that we're actually connected before trying anything
        if (mainActivity.btAdapter.getState() != mainActivity.btAdapter.STATE_ON) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mainActivity.btAdapter.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }*/
    }


