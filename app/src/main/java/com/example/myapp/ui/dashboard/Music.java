package com.example.myapp.ui.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Music {
    private final String title;
    private final String path;
    private List<LyricLine> lyrics = new ArrayList<>();

    public Music(String title, String path) {
        this.title = title;
        this.path = path;
    }
    public String getTitle() { return title; }
    public String getPath() { return path; }

    public List<LyricLine> getLyrics() {
        return lyrics == null ? Collections.emptyList() : lyrics;
    }

    public void setLyrics(List<LyricLine> lines) {
        if (lines == null) {
            this.lyrics = new ArrayList<>();
        } else {
            this.lyrics = new ArrayList<>(lines);
        }
    }

    public boolean hasLyrics() {
        return lyrics != null && !lyrics.isEmpty();
    }
}
