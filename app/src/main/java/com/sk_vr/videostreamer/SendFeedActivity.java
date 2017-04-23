package com.sk_vr.videostreamer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Calendar;
import java.util.List;

public class SendFeedActivity extends AppCompatActivity implements Runnable{
    Button startSendFeed;
    EditText IPAddrEditText;
    Socket senderSocket;
    BufferedWriter out;
    String IPAddr;
    ImgWriter imgWriter;
    private Camera mCamera;
    private CameraPreview mPreview;


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
                IPAddrEditText.setEnabled(false);
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

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camPreview);
        preview.addView(mPreview);




    }


    /**
     * Reference: https://developer.android.com/guide/topics/media/camera.html#access-camera
     */
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            mCamera.release();
            senderSocket.close();
            imgWriter.imgSenderSocket.close();
        }
        catch(IOException e){
            Log.d("VS123","Sender Thread - IOException from onStop() in SendFeed"+e.toString());
        }

    }



    /**
     * This function establishes socket connection over which dates will be sent
     */
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


    /**
     * This thread is used for sending dates over socket
     */
    @Override
    public void run() {

        initializeSendFeedActivity(IPAddr,Integer.parseInt(getString(R.string.common_socket)));

        try{
            while(true){
                Log.d("VS123","Sender Thread - created date content to send ");
                Calendar c=Calendar.getInstance();
                String date=c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
                out.write(date+"\n");
                out.flush();
                Log.d("VS123","Sender Thread - wrote "+date+" via socket");

                Thread.sleep(1000);
            }
        }
        catch (Exception e){
            Log.d("VS123",e.toString());
        }
    }


    /**
     * Inner class is used so that some variables defined in Activity can be used directly without thinking about passing them to this class instance
     */
    class ImgWriter extends Thread{
        Socket imgSenderSocket;
        DataOutputStream imgOutput;
        //String imgFolderPath="/storage/F074-706E/Demo/";  //Here saved images were present which were being sent over socket for testing

        Calendar pastSentImageCal;


        /**
         * This function establishes socket connection over which images will be sent
         */
        void initializeImgSend(String ip,int PortNo2){
            pastSentImageCal=Calendar.getInstance();  //This will be useful in setPreviewCallback
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


        /**
         * This thread is used for sending images over socket
         */
        @Override
        public void run() {
            initializeImgSend(IPAddr,Integer.parseInt(getString(R.string.img_socket)));

            /**
             * Reference: http://stackoverflow.com/questions/16602736/android-send-an-image-through-socket-programming
             */
            try{
                while(true){
                    /**
                     * takePicture caused a lot of latency while refreshing preview. Hence this was not selected
                     */
                    mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] imgBytes, Camera camera) {
                            try{
                                Calendar currentTime=Calendar.getInstance();
                                if(pastSentImageCal.get(Calendar.MILLISECOND)/100==currentTime.get(Calendar.MILLISECOND)/100){
                                    /**
                                     * Compare current time milliseconds and milliseconds at which last image frame was sent.
                                     * Here, both are divided by 100 thus achieving a frame rate of 10 fps
                                     */
                                    return;
                                }

                                pastSentImageCal=currentTime;  //If new image frame in new millisecond segment, store current Calendar instance

                                Log.d("VS123",imgBytes.length+"");


                                /**
                                 * Initially YUV to Bitmap conversion was performed on receiver side which was found to be inefficient
                                 * Reference: http://stackoverflow.com/a/9330203/5370202
                                 */
                                Camera.Parameters parameters = camera.getParameters();
                                int width = parameters.getPreviewSize().width;
                                int height = parameters.getPreviewSize().height;
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                YuvImage yuvImage = new YuvImage(imgBytes, ImageFormat.NV21, width, height, null);
                                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                                byte[] imageBytes = out.toByteArray();



                                imgOutput.writeInt(imageBytes.length); //Send image size
                                imgOutput.write(imageBytes,0,imageBytes.length); //Send image
                                imgOutput.flush();


                                int len=imageBytes.length;


                                Log.d("VS123","Img Sender Thread - sent image of len "+len+" via socket");

                            }
                            catch (Exception e){
                                Log.d("VS123 Cam","Exception thrown from onPictureTaken in PictureCallback "+e.toString());
                                e.printStackTrace();
                            }
                        }
                    });


                    Thread.sleep(1000);
                }
                /*
                //Stored Images Send Code
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
                }*/
            }
            catch (Exception e){
                Log.d("VS123","Exception in ImgWriter Thread "+e.toString());
            }
        }

        /*
        public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }*/
    }
}


/**
 * Reference: https://developer.android.com/guide/topics/media/camera.html#camera-preview
 * Except for parameters part, code is AS IS mentioned by example given above
 */

/** A basic Camera preview class */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;


        /**
         * Parameters can be changed over here
         */
        Camera.Parameters parameters=mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //This is for auto-focus
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);



        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("VS123 Cam", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("VS123 Cam", "Error starting camera preview: " + e.getMessage());
        }
    }
}
