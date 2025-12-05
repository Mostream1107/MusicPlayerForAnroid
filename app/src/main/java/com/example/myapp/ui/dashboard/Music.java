package com.example.myapp.ui.dashboard;

public class Music {
    private String title;
    private String path;

    public Music(String title, String path) {
        this.title = title;
        this.path = path;
    }
    public String getTitle() { return title; }
    public String getPath() { return path; }
}
