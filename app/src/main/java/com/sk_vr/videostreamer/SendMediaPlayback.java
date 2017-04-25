package com.sk_vr.videostreamer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SendMediaPlayback extends AppCompatActivity {
    Button startSendFeed;
    EditText IPAddrEditText,FilePathEditText;
    BufferedWriter out;
    String IPAddr,FilePath;
    private CameraPreview mPreview;
    int commonPort,imagePort;
    CustomVideoView videoView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_media_playback);


        startSendFeed=(Button)findViewById(R.id.sendFeedStartButton);
        IPAddrEditText=(EditText)findViewById(R.id.IPAddrText);
        FilePathEditText=(EditText)findViewById(R.id.VideoPathText);

        commonPort=Integer.parseInt(getResources().getString(R.string.common_socket));
        imagePort=Integer.parseInt(getResources().getString(R.string.img_socket));


        startSendFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSendFeed.setEnabled(false);
                IPAddrEditText.setEnabled(false);
                FilePathEditText.setEnabled(false);
                Log.d("VS123","Sender Thread - detected click");
                IPAddr=IPAddrEditText.getText().toString();
                FilePath=FilePathEditText.getText().toString();
                Log.d("VS123","Sender Thread - found IP Address to be "+IPAddr);
                if(IPAddr!=null && IPAddr.length()!=0){
                    Log.d("VS123","Sender Thread - created");
                    new VideoSendThread().start();
                }
            }
        });

        videoView =(CustomVideoView)findViewById(R.id.senderVideoView);

        //Creating MediaController
        MediaController mediaController= new MediaController(this);
        mediaController.setAnchorView(videoView);

        //specify the location of media file
        //Uri uri=Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/test1.mp4");

    }



    class VideoSendThread extends Thread{
        Socket socket;
        ServerSocket svrSocketForCmds;
        BufferedWriter cmdOut;

        void initVideoSendThread(){
            try{
                socket=new Socket(IPAddr,imagePort);
            }
            catch (Exception e){
                Log.d("VS123","Exception in run of VideoSendThread "+e.toString());
                e.printStackTrace();
            }

        }
        @Override
        public void run() {

            initVideoSendThread();

            File file = new File(FilePath);

            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(bytes, 0, bytes.length);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                os.writeInt(bytes.length);
                os.write(bytes, 0, bytes.length);
                Log.d("VS123","File received");
                os.flush();
                socket.close();
                Log.d("VS123","Video sent");

                svrSocketForCmds=new ServerSocket(commonPort);
                socket=svrSocketForCmds.accept();

                cmdOut=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Uri uri=Uri.parse("/storage/F074-706E/test1.mp4");

                        //Setting MediaController and URI, then starting the videoView
                        //videoView.setMediaController(mediaController);
                        videoView.addStream(cmdOut);
                        videoView.setVideoURI(uri);
                        videoView.requestFocus();
                        videoView.start();

                        Log.d("VS123","Hello");

                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                try {
                                    cmdOut.write("STOP\n");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });


                        videoView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                                    if(videoView.playingStatus){
                                        Log.d("VS123","Sender Video paused");
                                        videoView.pause();
                                    }
                                    else{
                                        Log.d("VS123","Sender Video resumed");
                                        videoView.resume();
                                    }
                                    return true;
                                }
                                else{
                                    return false;
                                }
                            }
                        });
                    }
                });
            }
            catch (Exception e){
                Log.d("VS123","Exception in run of VideoSendThread "+e.toString());
                e.printStackTrace();
            }
        }
    }
}

class CustomVideoView extends VideoView{
    boolean playingStatus=true;
    int pausedMillis;
    BufferedWriter cmdOut;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    void addStream(BufferedWriter cmdOut){
        this.cmdOut=cmdOut;
    }

    @Override
    public void pause() {
        playingStatus=false;
        pausedMillis=getCurrentPosition();
        try {
            cmdOut.write("PAUSE-"+pausedMillis+"\n");
            cmdOut.flush();
            Log.d("VS123","PAUSE cmd sent");
        } catch (IOException e) {
            Log.d("VS123","Exception in pause");
            e.printStackTrace();
        }
        super.pause();
    }

    @Override
    public void start() {
        playingStatus=true;

        super.start();
    }

    @Override
    public void resume() {
        playingStatus=true;
        super.resume();
        seekTo(pausedMillis);
        try {
            cmdOut.write("PLAY-"+pausedMillis+"\n");
            cmdOut.flush();
            Log.d("VS123","PLAY cmd sent");
        } catch (IOException e) {
            Log.d("VS123","Exception in pause");
            e.printStackTrace();
        }
        start();
    }
}