package com.sk_vr.videostreamer;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveMediaPlayback extends AppCompatActivity {

    int CommonPortNo, ImgPortNo;
    String IPAddr;
    VideoReceiveThread vt;
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_media_playback);


        CommonPortNo = Integer.parseInt(getResources().getString(R.string.common_socket));
        ImgPortNo = Integer.parseInt(getResources().getString(R.string.img_socket));

        videoView=(VideoView)findViewById(R.id.receiveVideoView);

        vt=new VideoReceiveThread();
        vt.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            vt.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class VideoReceiveThread extends Thread{
        ServerSocket serverSocket;
        Socket socket;

        void initVideoReceiveThread(){
            try{
                Log.d("VS123","Vid Serv Socket Ready");
                serverSocket=new ServerSocket(ImgPortNo);
                socket=serverSocket.accept();
                Log.d("VS123","Vid Serv Socket Connection established");
                String remoteAddr=socket.getRemoteSocketAddress().toString();
                IPAddr=remoteAddr.substring(remoteAddr.indexOf("/")+1,remoteAddr.indexOf(":"));
            }
            catch(Exception e){
                Log.d("VS123","Exception in initVideoReceiveThread "+e.toString());
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try{

                initVideoReceiveThread();

                Log.d("VS123","Receiving video");
                File file = new File(Environment.getExternalStorageDirectory(),"test1.mp4");

                DataInputStream is = new DataInputStream(socket.getInputStream());
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                byte[] bytes = new byte[is.readInt()];


                is.readFully(bytes, 0, bytes.length);
                bos.write(bytes, 0, bytes.length);

                Log.d("VS123","File received");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("VS123","Accessing video at "+Environment.getExternalStorageDirectory()+"/test1.mp4");
                        Uri uri=Uri.parse(Environment.getExternalStorageDirectory()+"/test1.mp4");

                        //Setting MediaController and URI, then starting the videoView
                        //videoView.setMediaController(mediaController);
                        videoView.setVideoURI(uri);
                        videoView.requestFocus();
                        videoView.start();

                        new VideoInteractThread().start();
                    }
                });

                bos.close();
                socket.close();
            }
            catch(Exception e){
                Log.d("VS123","Exception in run of VideoReceiveThread "+e.toString());
                e.printStackTrace();
            }

        }
    }

    class VideoInteractThread extends Thread{
        Socket socket;
        BufferedReader in;

        void initVideoInteractThread(){
            try{
                Log.d("VS123","connecting to ip: "+IPAddr);
                socket=new Socket(IPAddr,CommonPortNo);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch(Exception e){
                Log.d("VS123","Exception in VideoInteractThread "+e.toString());
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try{
                initVideoInteractThread();
                Log.d("VS123","Hello");

                while(true){

                    String cmdReceived=in.readLine();
                    Log.d("VS123","cmd received: "+cmdReceived);
                    if(cmdReceived.startsWith("PLAY")){
                        int millis=Integer.parseInt(cmdReceived.substring(cmdReceived.indexOf("-")+1));
                        videoView.seekTo(millis);
                        videoView.start();
                    }
                    else if(cmdReceived.startsWith("PAUSE")){
                        int millis=Integer.parseInt(cmdReceived.substring(cmdReceived.indexOf("-")+1));
                        videoView.seekTo(millis);
                        videoView.pause();
                    }
                    else if(cmdReceived.startsWith("STOP")){
                        videoView.stopPlayback();
                        break;
                    }
                }

            }
            catch(Exception e){
                Log.d("VS123","Exception in run of VideoInteractThread "+e.toString());
                e.printStackTrace();
            }

        }
    }
}
