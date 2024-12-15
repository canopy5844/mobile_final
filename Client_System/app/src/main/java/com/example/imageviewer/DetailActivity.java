package com.example.imageviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;

public class DetailActivity extends AppCompatActivity {
    TextView titleView;
    TextView articleTextView;
    TextView publishedDateTextView;
    ImageView imageView;
    Button btnSaveLocal;
    Button btnBackToMain;
    Bitmap bitmapImg;

    Activity currActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        currActivity = this;
        titleView = (TextView) findViewById(R.id.text_title);
        articleTextView = (TextView) findViewById(R.id.text_article_body);
        publishedDateTextView = (TextView) findViewById(R.id.text_date);
        imageView = (ImageView) findViewById(R.id.image_detail);
        btnSaveLocal = (Button) findViewById(R.id.btn_save_local);
        btnBackToMain = (Button) findViewById(R.id.back_to_main);

        Intent intent = getIntent();
        int index = intent.getIntExtra("index", -1);
        ImageArticle imgArticle = ImageArticles.getInstance().getArticle(index);
        bitmapImg = imgArticle.getImageBitmap();
        imageView.setImageBitmap(bitmapImg);
        titleView.setTextSize(Dimension.SP, 19);
        titleView.setText(imgArticle.getTitle());
        articleTextView.setTextSize(Dimension.SP, 19);
        articleTextView.setText(imgArticle.getBodyText());
        publishedDateTextView.setTextSize(Dimension.SP, 19);
        publishedDateTextView.setText(imgArticle.getPublishedDate());


        btnSaveLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date date = new Date();
                String path = Environment.getExternalStorageDirectory().getPath();
                String filepath = path + "/" + Environment.DIRECTORY_DCIM + "/" + String.valueOf(date.getTime()) + ".png";
                saveBitmapAsFile(bitmapImg, filepath);
            }
        });

        btnBackToMain.setOnClickListener(v -> backToMain());
    }

    private void backToMain() {
        currActivity.finish();
    }

    // https://snowdeer.github.io/android/2016/02/03/android-save-bitmap-to-file/
    private void saveBitmapAsFile(Bitmap bitmap, String filepath) {
        File file = new File(filepath);
        OutputStream os = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                os = Files.newOutputStream(file.toPath());
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}