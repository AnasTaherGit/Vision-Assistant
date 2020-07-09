package com.miniprojet.visionassitance;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final Integer REQUEST_ENABLE_BT = 1;
    BluetoothAdapter Bluetooth;
    BluetoothDevice HC06;
    TextToSpeech tts;
    String State="Not Connected";
    Date date= new Date();
    long StartDate=date.getTime()-1500;

    private class ConnectThread extends Thread{
        private  BluetoothSocket mmSocket;
        private  BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice=device;
            SetConnection();
        }

        public void run(){
            //Bluetooth.cancelDiscovery();
            ImageButton BluetoothConnect = (ImageButton) findViewById(R.id.ConnectBT);
            Log.d("Device",mmDevice.getName());
            BluetoothConnect.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ShowToast")
                @Override
                public void onClick(View v) {
                    if (State.equals("Not Connected") && Bluetooth.isEnabled()){
                        State="Connected";
                        Log.d("Device","Connected to");
                        Log.d("Device",mmDevice.getName());
                        try {
                            SetConnection();
                            mmSocket.connect();
                            Toast.makeText(MainActivity.this,"Connected",Toast.LENGTH_SHORT).show();
                        }catch (IOException connectException){
                            try {
                                mmSocket.close();
                            }catch (IOException closeException){
                                Log.e("Device","Could not close the client socket",closeException);
                            }
                        }
                        BluetoothCommunicationHandler BCH= new BluetoothCommunicationHandler(mmSocket);
                        BCH.start();
                    }
                    else {
                        if (State.equals("Connected") && Bluetooth.isEnabled()) {
                            State = "Not Connected";
                            Log.d("Device", State);
                            try {
                                State="Not Connected";
                                Toast.makeText(MainActivity.this,"Not Connected",Toast.LENGTH_SHORT).show();
                                mmSocket.close();
                            }catch (IOException e){
                                Log.e("Device","Could not close the client socket",e);
                            }
                        }
                    }
                }
            });

        }

        public void cancel(){
            try {
                State="Not Connected";
                mmSocket.close();
            }catch (IOException e){
                Log.e("Device","Could not close the client socket",e);
            }
        }
        @SuppressLint("ShowToast")
        private void SetConnection(){
            BluetoothSocket tmp=null;
            try {
                Toast.makeText(MainActivity.this,"Connection attempt",Toast.LENGTH_SHORT).show();
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
        Button Sender=(Button) findViewById(R.id.Sender);
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
            Sender.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String mMsg="A feen a l3iwr\n";
                    write(mMsg.getBytes());
                }
            });

        }

        public void run(){
            byte[] buffer=new byte[1024];
            int begin=0;
            int bytes=-1;
            while (!mmsocket.isConnected()){}
            Log.d("Device","Socket Connected ! Communication engaged");
            while (mmsocket.isConnected()){
                try {
                    //Log.d("Device","Input Stream Read");
                    bytes = mmInStream.read(buffer);
                    //Log.d("Device",String.valueOf(buffer));
                    //Log.d("Device","Input Steam read Complete");
                    //for (int i=begin;i<bytes;i++){
                    mHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();
                    //    begin=i+1;
                    //    if(i==bytes-1){
                    //        bytes=0;
                    //        begin=0;
                    //   }
                    //}
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
            int end=(int)msg.arg1;
            TextView Distance=(TextView)findViewById(R.id.Distance);
            date=new Date();
            long CurrentDate=date.getTime();
            long TIMEOUT=CurrentDate-StartDate;
            //Log.d("TIMEOUT",String.valueOf(TIMEOUT));
            switch (msg.what){
                case 1:
                    String writeMessage=new String(writeBuf,0,end);
                    if (writeMessage.startsWith("#")){
                        Log.d("Device",writeMessage);
                        Log.d("Device", String.valueOf((int)msg.arg1));
                        Distance.setText(writeMessage.substring(1,end));
                        if (!tts.isSpeaking() && TIMEOUT>=4000){
                            Thread thread= new Thread(new Runnable() {
                                String writeMessage;
                                int end;
                                @Override
                                public void run() {
                                    date = new Date();
                                    StartDate = date.getTime();
                                    //Log.d("Device",String.valueOf(StartDate));
                                    String toSpeak = "Attention un obstacle se trouve à une distance inférieure à 1 mètre";
                                    tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                                public Runnable init(String Message,int nbrend){
                                    end=nbrend;
                                    writeMessage=Message.substring(1,end);
                                    return this;
                                }
                            }.init(writeMessage,end));
                            thread.start();
                        }
                    }
                    else {
                        if (writeMessage.startsWith(">")) {
                            Distance.setText("No Obstacles");
                        } else {
                            Log.d("Device", writeMessage);
                            Log.d("Device", String.valueOf((int) msg.arg1));
                            Distance.setText(writeMessage);
                            if (!tts.isSpeaking() && TIMEOUT>=4000) {
                                Thread thread = new Thread(new Runnable() {
                                    String writeMessage;
                                    @Override
                                    public void run() {
                                        date = new Date();
                                        StartDate = date.getTime();
                                        //Log.d("Device",String.valueOf(StartDate));
                                        String toSpeak = "Attention un obstacle se trouve à une distance inférieure à 1 mètre";
                                        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                                    }
                                    public Runnable init(String Message) {
                                        writeMessage = Message;
                                        return this;
                                    }
                                }.init(writeMessage));
                                thread.start();
                            }
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bluetooth = BluetoothAdapter.getDefaultAdapter();
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.FRENCH);
                    tts.speak("Bonjour Je suis votre Assistant de Navigation",TextToSpeech.QUEUE_FLUSH,null,null);
                }
            }
        });

        /*
        if (Bluetooth==null){
            // Device doesn't support Bluetooth
        }
         */
        if (!Bluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //while(!Bluetooth.isEnabled()){}

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

    public void onPause(){
        if (tts!=null){
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }
}