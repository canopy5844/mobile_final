package com.example.imageviewer;

import android.graphics.Bitmap;

public class ImageArticle {
    private Bitmap imageBitmap;
    private String title;
    private String bodyText;
    private String publishedDate;

    private static ImageArticle instance;

    public ImageArticle(Bitmap _imageBitmap, String _title, String _bodyText, String _publishedDate) {
        imageBitmap = _imageBitmap;
        title = _title;
        bodyText = _bodyText;
        publishedDate = _publishedDate;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String _publishedDate) {
        this.publishedDate = _publishedDate;
    }
}