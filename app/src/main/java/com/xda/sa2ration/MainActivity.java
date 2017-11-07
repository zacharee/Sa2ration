package com.xda.sa2ration;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!testSudo()) {
            finish();
        }

        setContentView(R.layout.activity_main);

        String output = "1.0";

        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            Method get = SystemProperties.getMethod("get", String.class);
            output = get.invoke(null, "persist.sys.sf.color_saturation").toString();

            if (output == null || output.isEmpty()) output = "1.0";
        } catch (Exception e) {
            e.printStackTrace();
        }

        SeekBar seekBar = findViewById(R.id.seekBar);

        float fakeProgress = Float.valueOf(output) * 100;
        seekBar.setProgress((int) fakeProgress);

        final TextView textView = findViewById(R.id.textView);
        textView.setText(format(Float.valueOf(output)));

        ImageView preview = findViewById(R.id.imageView);
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.photo_by)
                        .setMessage(Html.fromHtml(getResources().getString(R.string.photo_by_desc), 0))
                        .show();

                TextView link = alertDialog.findViewById(android.R.id.message);
                link.setLinksClickable(true);
                link.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sudo("service call SurfaceFlinger 1022 f " + format(progress / 100F));
                textView.setText(format(progress / 100F));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sudo("setprop persist.sys.sf.color_saturation " + format(seekBar.getProgress() / 100F));
            }
        });
    }

    private String format(float progress) {
        return String.format(Locale.US, "%.2f", progress);
    }

    public static void sudo(String... strings) {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("No Root?", e.getMessage());
            }
            outputStream.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static boolean testSudo() {
        StackTraceElement st = null;

        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while (bufferedReader.readLine() != null) {
                bufferedReader.readLine();
            }

            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            for (StackTraceElement s : e.getStackTrace()) {
                st = s;
                if (st != null) break;
            }
        }

        return st == null;
    }
}
