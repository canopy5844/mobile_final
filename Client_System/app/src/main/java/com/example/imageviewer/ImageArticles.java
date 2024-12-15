package com.example.imageviewer;

import java.util.ArrayList;
import java.util.List;

public class ImageArticles {
    private final List<ImageArticle> imageArticleList;
    private static ImageArticles instance;

    private ImageArticles() {
        imageArticleList = new ArrayList<>();
    }

    public static ImageArticles getInstance() {
        if (instance == null) {
            instance = new ImageArticles();
        }
        return instance;
    }

    public void clear() {
        instance = null;
    }

    public void addArticle(ImageArticle article) {
        imageArticleList.add(article);
    }

    public ImageArticle getArticle(int pos) {
        return imageArticleList.get(pos);
    }

    public int length() {
        return imageArticleList.size();
    }
}
