package com.sk_vr.videostreamer;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveFeedActivity extends AppCompatActivity implements Runnable {
    TextView statusText, contentDisplayText;
    ImageView receivedImageView;
    ServerSocket serverSocket;
    Socket socket;
    BufferedReader in;
    int CommonPortNo, ImgPortNo;
    ImgDisplayer imgDisplayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_feed);

        statusText = (TextView) findViewById(R.id.statusText);
        contentDisplayText = (TextView) findViewById(R.id.contentDisplayText);
        receivedImageView = (ImageView) findViewById(R.id.receivedImg);

        //receivedImageView.setRotationX((float) -90.0);
        CommonPortNo = Integer.parseInt(getResources().getString(R.string.common_socket));
        ImgPortNo = Integer.parseInt(getResources().getString(R.string.img_socket));
    }

    @Override
    protected void onStart() {
        super.onStart();

        new Thread(this).start();  //This will start receiver thread for socket having dates transferred through them

        imgDisplayer = new ImgDisplayer();
        imgDisplayer.start();  //This will start receiver thread for socket receiving realtime images of video
    }


    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
            //socket.close();
            imgDisplayer.imgServerSocket.close();
            //imgDisplayer.imgSocket.close();
        } catch (IOException e) {
            Log.d("VS123", "IOException from onStop() in SendFeed " + e.toString());
        }

    }


    /**
     * This function establishes socket connection over which dates will be received
     */
    void initializeReceiveFeedActivity() {
        try {
            serverSocket = new ServerSocket(CommonPortNo);
            Log.d("VS123", "Receiver Thread - server socket created");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText("Status: Waiting for connections...");
                }
            });

            Log.d("VS123", "Receiver Thread - waiting for clients");
            socket = serverSocket.accept();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText("Status: Connection established with " + socket.getRemoteSocketAddress());
                    Log.d("VS123", "Receiver Thread - connected to client: " + socket.getRemoteSocketAddress());
                }
            });
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            Log.d("VS123", "Receiver Thread - IOException from initializeReceiveFeedActivity() " + e.toString());
        }
    }



    /**
     * This thread is used for receiving dates sent over socket
     */
    @Override
    public void run() {
        Log.d("VS123", "Receiver Thread started");

        initializeReceiveFeedActivity();

        try {
            while (true) {
                final String receivedText = in.readLine();
                Log.d("VS123", "Receiver Thread - received date: " + receivedText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        contentDisplayText.setText("Content: " + receivedText);
                    }
                });

            }
        } catch (Exception e) {
            Log.d("VS123", e.toString());
        }
    }


    /**
     * Inner class is used so that some variables defined in Activity can be used directly without thinking about passing them to this class instance
     */
    class ImgDisplayer extends Thread {
        ServerSocket imgServerSocket;
        Socket imgSocket;
        DataInputStream imgIn;

        /**
         * This function establishes socket connection over which images will be received
         */
        void initializeReceiveImage() {
            try {
                imgServerSocket = new ServerSocket(ImgPortNo);
                Log.d("VS123", "Img Receiver Thread - server socket created");

                Log.d("VS123", "Img Receiver Thread - waiting for clients");
                imgSocket = imgServerSocket.accept();

                Log.d("VS123", "Img Receiver Thread - connected to client");

                imgIn = new DataInputStream(imgSocket.getInputStream());


            } catch (IOException e) {
                Log.d("VS123", "Img Receiver Thread - IOException from initializeReceiveImage()" + e.toString());
            }
        }


        /**
         * This thread is used for receiving images sent over socket
         */
        @Override
        public void run() {
            Log.d("VS123", "Img Receiver Thread started");

            initializeReceiveImage();

            try {
                while (true) {
                    //Size of image and image are added to this socket

                    final int len = imgIn.readInt(); //Read size of image as sent over socket

                    if (len < 0) {
                        continue;
                    }
                    final byte[] receivedImg = new byte[len];

                    imgIn.readFully(receivedImg);  //Read image from socket buffer

                    /*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Received Image of len "+len+" from socket", Toast.LENGTH_SHORT).show();
                        }
                    });
                    */

                    Log.d("VS123", "Img Receiver Thread - received Image of len " + len + " from socket");


                    /**
                     * Reference: http://stackoverflow.com/q/3520019/5370202
                     */
                    final Bitmap bm = BitmapFactory.decodeByteArray(receivedImg, 0, receivedImg.length);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            receivedImageView.setImageBitmap(bm);
                        }
                    });

                }
            } catch (Exception e) {
                Log.d("VS123", e.toString());
            }
        }
    }
}
