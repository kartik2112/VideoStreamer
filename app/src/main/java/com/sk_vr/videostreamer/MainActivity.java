package com.sk_vr.videostreamer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button send,receive;

        send=(Button)findViewById(R.id.sendFeedButton);
        receive=(Button)findViewById(R.id.receiveFeedButton);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(view.getContext(),SendFeedActivity.class);
                startActivity(i);
            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(view.getContext(),ReceiveFeedActivity.class);
                startActivity(i);
            }
        });
    }
}
