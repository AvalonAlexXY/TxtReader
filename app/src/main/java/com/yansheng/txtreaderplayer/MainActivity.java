package com.yansheng.txtreaderplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kaixinbook.Read;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String path = getExternalCacheDir().getAbsolutePath()+"/艺术的起源.txt";

        file = new File(path);

        try {
            file.createNewFile();

            InputStream inputStream = this.getApplicationContext().getResources().openRawResource(R.raw.test);
            inputstreamtofile(inputStream, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this,Read.class);
        intent.putExtra("title","标题");
        intent.putExtra("path",path);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        file.delete();
    }

    private void inputstreamtofile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
