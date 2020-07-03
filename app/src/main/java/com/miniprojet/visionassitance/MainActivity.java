package com.miniprojet.visionassitance;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final Integer REQUEST_ENABLE_BT = 1;
    BluetoothAdapter Bluetooth;
    BluetoothDevice HC06;
    String State="Not Connected";


    private class ConnectThread extends Thread{
        private  BluetoothSocket mmSocket;
        private  BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice=device;
            SetConnection();
        }

        public void run(){
            //Bluetooth.cancelDiscovery();
            Button BluetoothConnect =(Button)findViewById(R.id.ConnectBT);
            Log.d("Device",mmDevice.getName());
            BluetoothConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (State.equals("Not Connected") && Bluetooth.isEnabled()){
                        State="Connected";
                        Log.d("Device","Connected to");
                        Log.d("Device",mmDevice.getName());
                        try {
                            SetConnection();
                            mmSocket.connect();
                        }catch (IOException connectException){
                            try {
                                mmSocket.close();
                            }catch (IOException closeException){
                                Log.e("Device","Could not close the client socket",closeException);
                            }
                        }
                    }
                    else {
                        if (State.equals("Connected") && Bluetooth.isEnabled()) {
                            State = "Not Connected";
                            Log.d("Device", State);
                            try {
                                State="Not Connected";
                                mmSocket.close();
                            }catch (IOException e){
                                Log.e("Device","Could not close the client socket",e);
                            }
                        }
                    }
                }
            });
            BluetoothCommunicationHandler BCH= new BluetoothCommunicationHandler(mmSocket);
            BCH.start();
        }

        public void cancel(){
            try {
                State="Not Connected";
                mmSocket.close();
            }catch (IOException e){
                Log.e("Device","Could not close the client socket",e);
            }
        }
        private void SetConnection(){
            BluetoothSocket tmp=null;
            try {
                Log.d("Device","Connection Attempt");
                String my_UUID = "00001101-0000-1000-8000-00805F9B34FB";
                tmp=mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(my_UUID));
                Log.d("Device","Connection Attempt Successful");
            } catch (IOException e){
                Log.e("Device","Socket's create() method failed");
            }
            mmSocket=tmp;
        }

        public BluetoothSocket getSocket() {
            Log.d("Device", String.valueOf(mmSocket.isConnected()));
            return mmSocket;
        }
    }

    private class BluetoothCommunicationHandler extends Thread{
        private final BluetoothSocket mmsocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public BluetoothCommunicationHandler(BluetoothSocket socket){
            Log.d("Device","Communication Starting ...");
            mmsocket=socket;
            Log.d("Device", String.valueOf(socket.getRemoteDevice()));
            InputStream tmpIn=null;
            OutputStream tmpOut=null;
            try {
                tmpIn=socket.getInputStream();
                tmpOut=socket.getOutputStream();
            }catch (IOException e){
                Log.d("Device","Unable to get Input/Output Stream");
            }
            mmInStream=tmpIn;
            Log.d("Device",String.valueOf(mmInStream));
            mmOutStream=tmpOut;
        }

        public void run(){
            byte[] buffer=new byte[1024];
            int begin=0;
            int bytes=0;
            while (!mmsocket.isConnected()){}
            Log.d("Device","Socket Connected ! Communication engaged");
            while (mmsocket.isConnected()){
                try {
                    //Log.d("Device","Input Stream Read");
                    bytes+=mmInStream.read(buffer,bytes,buffer.length-bytes);
                    //Log.d("Device",String.valueOf(buffer));
                    //Log.d("Device","Input Steam read Complete");
                    for (int i=begin;i<bytes;i++){
                        mHandler.obtainMessage(1,begin,i,buffer).sendToTarget();
                        begin=i+1;
                        if(i==bytes-1){
                            bytes=0;
                            begin=0;
                        }
                    }
                } catch (IOException e){
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            try {
                mmOutStream.write(bytes);
            }catch (IOException e){ }
        }
    }
    @SuppressLint("HandlerLeak")
    Handler mHandler =new Handler(){
        @Override
        public void handleMessage(Message msg){
            byte[] writeBuf=(byte[])msg.obj;
            int begin=(int)msg.arg1;
            int end=(int)msg.arg2;
            TextView Distance=(TextView)findViewById(R.id.Distance);

            switch (msg.what){
                case 1:
                    String writeMessage=new String(writeBuf);
                    writeMessage=writeMessage.substring(begin,end);
                    Log.d("Device",writeMessage);
                    Distance.setText(writeMessage);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bluetooth = BluetoothAdapter.getDefaultAdapter();
        /*
        if (Bluetooth==null){
            // Device doesn't support Bluetooth
        }
         */
        if (!Bluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = Bluetooth.getBondedDevices();

        if (pairedDevices.size() > 0) {
            Log.d("Device","Paired devices discovered");
            for (BluetoothDevice device : pairedDevices) {
                Log.d("Device", device.getName());
                if (device.getName().equals("HC-06")){
                    HC06 = device;
                }
            }
        }
        ConnectThread HC06_Connect = new ConnectThread(HC06);
        HC06_Connect.start();

    }
}