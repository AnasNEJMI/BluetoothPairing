package com.duster.fr.client;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Anas on 17/09/2015.
 */
public class chatActivity extends Activity {

    EditText sensor_number;
    Button sendBtn;
    TextView textView;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_fragment);

        sensor_number = (EditText) findViewById(R.id.sensor_number);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        textView = (TextView) findViewById(R.id.request_data);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            textView.setText("Request Data From" +extras.getString("device_name"));
        }


        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.sensor_number);
                String message = view.getText().toString();
                //sendMessage(message);
            }
        });

    }


    private void sendMessage(String message) {
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
    }
}
