package net.watto.apath;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

abstract class ControlActivity extends Activity {
    protected byte[] command = {'M', 0x40, 0x40};
    protected long distance = 0;
    private Timer timer;
    private BluetoothDevice device;
    private BluetoothSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String deviceAddress = extras.getString("device");
            device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
        }

        if (device == null) {
            finish();
            Log.i("watto", "Device not sent");
            this.finish();
            return;
        }

        timer = new Timer("Send data to Arduino");
    }

    @Override
    protected void onResume() {
        super.onResume();
        final OutputStream os;
        final InputStream is;

        Method m;
        try {
            m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
            socket = (BluetoothSocket) m.invoke(device, 1);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            socket.connect();
            os = socket.getOutputStream();
            is = socket.getInputStream();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    //Log.i("watto", "R" + command[1] + " L" + command[2]);
                    os.write(command);
                    os.flush();
                    //Log.i("watto", "D" + distance);
                } catch (IOException e) {
                    // send data to bluetooth
                    ControlActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ControlActivity.this.finish();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }, 0, 100);

        new ReceiveThread(is).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        timer.purge();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        this.finish();
    }

    private class ReceiveThread extends Thread {
        private InputStream is;
        private Boolean next = false;

        public ReceiveThread(InputStream is) {
            this.is = is;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int i;

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = this.is.read(buffer);
                    // Send the obtained bytes to the UI activity
                    for (i = 0; i < bytes; ++i) {
                        if (this.next) {
                            distance = (buffer[i] & 0xFF);
                            this.next = false;
                        } else if (buffer[i] == 'D') {
                            this.next = true;
                        }
                    }
                } catch (IOException e) {
                    finish();
                    break;
                }
            }
        }
    }
}
