package com.sk_vr.videostreamer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Calendar;

public class SendFeedActivity extends AppCompatActivity implements Runnable{
    Button startSendFeed;
    EditText IPAddrEditText;
    Socket senderSocket;
    BufferedWriter out;
    String IPAddr;
    ImgWriter imgWriter;

    void initializeSendFeedActivity(String ip,int PortNo){
        try{
            senderSocket=new Socket(ip,PortNo);
            Log.d("VS123","Sender Thread - created socket");
            out=new BufferedWriter(new OutputStreamWriter(senderSocket.getOutputStream()));

        }
        catch (IOException e){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Device not found!",Toast.LENGTH_LONG).show();
                }
            });

            Log.d("VS123","Sender Thread - IOException from initializeSendFeedActivity()"+e.toString());
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feed);

        startSendFeed=(Button)findViewById(R.id.sendFeedStartButton);
        IPAddrEditText=(EditText)findViewById(R.id.IPAddrText);

        startSendFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSendFeed.setEnabled(false);
                startSendFeed.setClickable(false);
                Log.d("VS123","Sender Thread - detected click");
                IPAddr=IPAddrEditText.getText().toString();
                Log.d("VS123","Sender Thread - found IP Address to be "+IPAddr);
                if(IPAddr!=null && IPAddr.length()!=0){
                    Log.d("VS123","Sender Thread - created");
                    new Thread(SendFeedActivity.this).start();
                    imgWriter=new ImgWriter();
                    imgWriter.start();
                }
            }
        });
    }
/*
    @Override
    protected void onStop() {
        super.onStop();
        try{
            senderSocket.close();
            imgWriter.imgSenderSocket.close();
        }
        catch(IOException e){
            Log.d("VS123","Sender Thread - IOException from onStop() in SendFeed"+e.toString());
        }

    }*/

    @Override
    public void run() {

        initializeSendFeedActivity(IPAddr,Integer.parseInt(getString(R.string.common_socket)));

        try{
            while(true){
                Log.d("VS123","Sender Thread - created content ");
                Calendar c=Calendar.getInstance();
                String date=c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
                out.write(date+"\n");
                out.flush();
                Log.d("VS123","Sender Thread - wrote content via socket");

                Thread.sleep(1000);
            }
        }
        catch (Exception e){
            Log.d("VS123",e.toString());
        }
    }



    class ImgWriter extends Thread{
        Socket imgSenderSocket;
        DataOutputStream imgOutput;
        String imgFolderPath="/storage/F074-706E/Demo/";

        void initializeImgSend(String ip,int PortNo2){
            try{
                imgSenderSocket=new Socket(ip,PortNo2);
                imgOutput = new DataOutputStream(imgSenderSocket.getOutputStream());

                Log.d("VS123","Img Sender Thread - created socket");

            }
            catch (IOException e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Device not found!",Toast.LENGTH_LONG).show();
                    }
                });

                Log.d("VS123","Img Sender Thread - IOException from initializeImgSend()"+e.toString());
            }

        }

        @Override
        public void run() {
            /**
             * Reference: http://stackoverflow.com/questions/16602736/android-send-an-image-through-socket-programming
             */
            initializeImgSend(IPAddr,Integer.parseInt(getString(R.string.img_socket)));

            try{
                for(int imgNo=1;imgNo<=7;imgNo++){
                    Log.d("VS123","Img Sender Thread - created content ");

                    FileInputStream fis=new FileInputStream(new File(imgFolderPath+"DemoPic"+imgNo+".jpg"));

                    Bitmap bm= BitmapFactory.decodeStream(fis);
                    byte[] imgBytes=getBytesFromBitmap(bm);
                    Log.d("VS123",imgBytes.length+"");

                    imgOutput.writeInt(imgBytes.length);
                    imgOutput.write(imgBytes,0,imgBytes.length);
                    imgOutput.flush();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Sent Content to socket",Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.d("VS123","Img Sender Thread - wrote content via socket");

                    Thread.sleep(4000);
                }
            }
            catch (Exception e){
                Log.d("VS123","Exception in ImgWriter Thread "+e.toString());
            }
        }

        public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }
    }
}
